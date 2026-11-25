// Genera las versiones livianas de las imagenes que usa la aplicacion.
//
// Uso (desde la carpeta frontend):   npm run imagenes
//
// Lee los originales de src/assets/images y escribe:
//   - src/assets/images/logo-claro-app.webp   (de logo-claro.png:  sin fondo blanco, recortado, 640 px)
//   - src/assets/images/logo-oscuro-app.webp  (de logo-oscuro.png: recortado, 640 px)
//   - src/assets/images/inicio-app.webp       (de inicio.png:      sin fondo blanco, recortada, 960 px)
//   - favicon-claro.png y favicon-oscuro.png  (de Icono-claro.png e Icono-oscuro.png: sin esquinas negras, 64 px)
//
// Si cambian alguna imagen, reemplacen el original con el mismo nombre y vuelvan a correrlo.
// Usa la libreria sharp (dependencia de desarrollo): funciona en Windows, Mac y Linux.

import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import sharp from 'sharp';

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), '..');
const IMAGENES = join(RAIZ, 'src', 'assets', 'images');

// Calidad WebP: 85 se ve igual al original y pesa una fraccion
const WEBP = { quality: 85, alphaQuality: 90, effort: 6 };

/** Lee la imagen como pixeles RGBA. */
async function abrir(nombre) {
  const ruta = join(IMAGENES, nombre);
  if (!existsSync(ruta)) throw new Error(`No se encontro ${ruta}`);
  const { data, info } = await sharp(ruta).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  return { px: data, ancho: info.width, alto: info.height };
}

/**
 * Vuelve transparente el fondo que toca los bordes ("blanco" o "negro"), sin
 * tocar el mismo color dentro del dibujo. La orilla queda semitransparente
 * para no dejar un halo.
 */
function quitarFondo({ px, ancho, alto }, color) {
  const FONDO = 18;
  const ORILLA = 70;
  const distancia = (i) => (color === 'blanco' ? 255 - Math.min(px[i], px[i + 1], px[i + 2]) : Math.max(px[i], px[i + 1], px[i + 2]));

  const visto = new Uint8Array(ancho * alto);
  const pila = [];
  for (let x = 0; x < ancho; x++) pila.push(x, (alto - 1) * ancho + x);
  for (let y = 0; y < alto; y++) pila.push(y * ancho, y * ancho + ancho - 1);

  while (pila.length) {
    const p = pila.pop();
    if (visto[p]) continue;
    const i = p * 4;
    // Un pixel ya transparente cuenta como fondo
    const d = px[i + 3] === 0 ? 0 : distancia(i);
    if (d > ORILLA) continue;
    visto[p] = 1;
    if (d <= FONDO) {
      px[i + 3] = 0;
      const x = p % ancho;
      if (x > 0) pila.push(p - 1);
      if (x < ancho - 1) pila.push(p + 1);
      if (p >= ancho) pila.push(p - ancho);
      if (p < (alto - 1) * ancho) pila.push(p + ancho);
    } else {
      px[i + 3] = Math.min(px[i + 3], Math.round((255 * (d - FONDO)) / (ORILLA - FONDO)));
    }
  }
  return { px, ancho, alto };
}

/**
 * Borra restos sueltos que tocan el borde de la imagen (por ejemplo, un
 * pedazo de dibujo que quedo al recortar el original). Solo se borran las
 * piezas pequenas (menos del 1 % de la imagen); el logo nunca es tan pequeno.
 */
function quitarRestosDelBorde({ px, ancho, alto }) {
  const total = ancho * alto;
  const pieza = new Int32Array(total).fill(-1);
  const opaco = (p) => px[p * 4 + 3] > 16;
  let borrados = 0;

  for (let inicio = 0; inicio < total; inicio++) {
    if (!opaco(inicio) || pieza[inicio] !== -1) continue;
    // Recorre la pieza conectada que empieza en este pixel
    const miembros = [];
    const pila = [inicio];
    pieza[inicio] = inicio;
    let tocaBorde = false;
    while (pila.length) {
      const p = pila.pop();
      miembros.push(p);
      const x = p % ancho;
      const y = (p - x) / ancho;
      if (x === 0 || y === 0 || x === ancho - 1 || y === alto - 1) tocaBorde = true;
      const vecinos = [x > 0 && p - 1, x < ancho - 1 && p + 1, y > 0 && p - ancho, y < alto - 1 && p + ancho];
      for (const v of vecinos) {
        if (v !== false && pieza[v] === -1 && opaco(v)) {
          pieza[v] = inicio;
          pila.push(v);
        }
      }
    }
    if (tocaBorde && miembros.length < total * 0.01) {
      for (const p of miembros) px[p * 4 + 3] = 0;
      borrados += miembros.length;
    }
  }
  return { px, ancho, alto, borrados };
}

/** Recorta el margen transparente, dejando `margen` pixeles alrededor. */
function recorte({ px, ancho, alto }, margen) {
  let x0 = ancho;
  let y0 = alto;
  let x1 = 0;
  let y1 = 0;
  for (let y = 0; y < alto; y++) {
    for (let x = 0; x < ancho; x++) {
      if (px[(y * ancho + x) * 4 + 3] > 16) {
        if (x < x0) x0 = x;
        if (x > x1) x1 = x;
        if (y < y0) y0 = y;
        if (y > y1) y1 = y;
      }
    }
  }
  x0 = Math.max(0, x0 - margen);
  y0 = Math.max(0, y0 - margen);
  x1 = Math.min(ancho - 1, x1 + margen);
  y1 = Math.min(alto - 1, y1 + margen);
  return { left: x0, top: y0, width: x1 - x0 + 1, height: y1 - y0 + 1 };
}

/** Recorta, reduce al ancho pedido (sin agrandar) y guarda en el formato indicado. */
async function guardar(imagen, { margen, ancho, destino, formato }) {
  const salida = sharp(imagen.px, { raw: { width: imagen.ancho, height: imagen.alto, channels: 4 } })
    .extract(recorte(imagen, margen))
    .resize({ width: ancho, withoutEnlargement: true });
  const info = await (formato === 'webp' ? salida.webp(WEBP) : salida.png({ compressionLevel: 9 })).toFile(destino);
  const kb = (info.size / 1024).toFixed(0);
  console.log(`  ${destino.slice(RAIZ.length + 1).replaceAll('\\', '/')}  ${info.width}x${info.height}  ${kb} KB`);
}

const tareas = [
  {
    origen: 'logo-claro.png',
    fondo: 'blanco',
    salidas: [{ margen: 4, ancho: 640, destino: join(IMAGENES, 'logo-claro-app.webp'), formato: 'webp' }]
  },
  {
    origen: 'logo-oscuro.png',
    fondo: null,
    salidas: [{ margen: 4, ancho: 640, destino: join(IMAGENES, 'logo-oscuro-app.webp'), formato: 'webp' }]
  },
  {
    // Se muestra a 480 px como maximo: 960 se ve nitida en pantallas de alta densidad
    origen: 'inicio.png',
    fondo: 'blanco',
    salidas: [{ margen: 2, ancho: 960, destino: join(IMAGENES, 'inicio-app.webp'), formato: 'webp' }]
  },
  {
    origen: 'Icono-claro.png',
    fondo: 'negro',
    salidas: [{ margen: 0, ancho: 64, destino: join(RAIZ, 'favicon-claro.png'), formato: 'png' }]
  },
  {
    origen: 'Icono-oscuro.png',
    fondo: 'negro',
    salidas: [{ margen: 0, ancho: 64, destino: join(RAIZ, 'favicon-oscuro.png'), formato: 'png' }]
  }
];

async function generar() {
  console.log('Generando imagenes livianas:');
  for (const tarea of tareas) {
    let imagen = await abrir(tarea.origen);
    if (tarea.fondo) imagen = quitarFondo(imagen, tarea.fondo);
    imagen = quitarRestosDelBorde(imagen);
    if (imagen.borrados) console.log(`  (${tarea.origen}: se borraron ${imagen.borrados} pixeles sueltos en el borde)`);
    for (const salida of tarea.salidas) await guardar(imagen, salida);
  }
  console.log('Listo.');
}

generar().catch((error) => {
  console.error(`Error: ${error.message}`);
  process.exitCode = 1;
});
