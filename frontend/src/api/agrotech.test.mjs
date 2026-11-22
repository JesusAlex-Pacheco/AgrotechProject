// Pruebas del cliente de la API (npm test). Se reemplaza fetch por uno simulado
// para comprobar que el token viaja bien y que una sesion vencida se maneja.
import { afterEach, beforeEach, describe, it } from 'node:test';
import assert from 'node:assert/strict';

import { ApiError, configurarSesionApi, solicitar } from './agrotech.js';

const fetchOriginal = globalThis.fetch;
let llamadas;

/** Simula el backend: cada llamada a fetch toma la siguiente respuesta de la lista. */
function backendResponde(...respuestas) {
  llamadas = [];
  globalThis.fetch = async (url, opciones) => {
    llamadas.push({ url, opciones });
    const r = respuestas[Math.min(llamadas.length - 1, respuestas.length - 1)];
    if (r instanceof Error) throw r;
    return new Response(r.cuerpo === undefined ? null : JSON.stringify(r.cuerpo), { status: r.estado ?? 200 });
  };
}

const cabecera = (indice, nombre) => llamadas[indice].opciones.headers[nombre];

beforeEach(() => configurarSesionApi({ token: null, alVencer: () => {} }));
afterEach(() => {
  globalThis.fetch = fetchOriginal;
});

describe('token de sesion', () => {
  it('envia el token como Authorization: Bearer', async () => {
    configurarSesionApi({ token: 'abc123' });
    backendResponde({ cuerpo: [] });

    await solicitar('/pedidos');

    assert.equal(llamadas[0].url, '/api/pedidos');
    assert.equal(cabecera(0, 'Authorization'), 'Bearer abc123');
  });

  it('sin sesion no envia Authorization', async () => {
    backendResponde({ cuerpo: [] });

    await solicitar('/productos');

    assert.equal(cabecera(0, 'Authorization'), undefined);
  });

  it('registrar alVencer no borra el token ya configurado', async () => {
    // Error que existio: al recargar la pagina se perdia el token
    configurarSesionApi({ token: 'abc123' });
    configurarSesionApi({ alVencer: () => {} });
    backendResponde({ cuerpo: [] });

    await solicitar('/pedidos');

    assert.equal(cabecera(0, 'Authorization'), 'Bearer abc123');
  });

  it('al cerrar sesion deja de enviar el token', async () => {
    configurarSesionApi({ token: 'abc123' });
    configurarSesionApi({ token: null });
    backendResponde({ cuerpo: [] });

    await solicitar('/productos');

    assert.equal(cabecera(0, 'Authorization'), undefined);
  });
});

describe('sesion vencida (401)', () => {
  it('en una consulta avisa que vencio y la repite sin token', async () => {
    let avisos = 0;
    configurarSesionApi({ token: 'vencido', alVencer: () => avisos++ });
    backendResponde({ estado: 401, cuerpo: { mensaje: 'Token vencido' } }, { cuerpo: [{ id: 1 }] });

    const productos = await solicitar('/productos');

    assert.equal(avisos, 1);
    assert.equal(llamadas.length, 2);
    assert.equal(cabecera(1, 'Authorization'), undefined);
    assert.deepEqual(productos, [{ id: 1 }]);
  });

  it('en una escritura avisa que vencio y no la repite', async () => {
    let avisos = 0;
    configurarSesionApi({ token: 'vencido', alVencer: () => avisos++ });
    backendResponde({ estado: 401, cuerpo: { mensaje: 'Inicie sesion de nuevo' } });

    await assert.rejects(solicitar('/pedidos', { method: 'POST', body: '{}' }), (e) => e instanceof ApiError && e.estado === 401);
    assert.equal(avisos, 1);
    assert.equal(llamadas.length, 1);
  });

  it('un 401 sin token (credenciales invalidas) no cierra ninguna sesion', async () => {
    let avisos = 0;
    configurarSesionApi({ alVencer: () => avisos++ });
    backendResponde({ estado: 401, cuerpo: { mensaje: 'Correo o clave incorrectos' } });

    await assert.rejects(solicitar('/sesiones', { method: 'POST', body: '{}' }), { message: 'Correo o clave incorrectos' });
    assert.equal(avisos, 0);
  });
});

describe('errores y respuestas', () => {
  it('convierte el error del backend en ApiError con su mensaje, estado y errores por campo', async () => {
    backendResponde({ estado: 400, cuerpo: { mensaje: 'Datos invalidos', errores: { precio: 'El precio es obligatorio' } } });

    await assert.rejects(solicitar('/productos', { method: 'POST', body: '{}' }), (e) => {
      assert.ok(e instanceof ApiError);
      assert.equal(e.estado, 400);
      assert.equal(e.message, 'Datos invalidos');
      assert.deepEqual(e.errores, { precio: 'El precio es obligatorio' });
      return true;
    });
  });

  it('un 403 trae el mensaje del backend', async () => {
    backendResponde({ estado: 403, cuerpo: { mensaje: 'Solo puede actuar a nombre de su propia finca' } });

    await assert.rejects(solicitar('/productos', { method: 'POST', body: '{}' }), {
      message: 'Solo puede actuar a nombre de su propia finca'
    });
  });

  it('sin conexion con el backend lanza un error claro con estado 0', async () => {
    backendResponde(new TypeError('Failed to fetch'));

    await assert.rejects(solicitar('/productos'), (e) => e instanceof ApiError && e.estado === 0 && /backend/.test(e.message));
  });

  it('una respuesta 204 devuelve null', async () => {
    backendResponde({ estado: 204 });

    assert.equal(await solicitar('/categorias/1', { method: 'DELETE' }), null);
  });

  it('con JSON envia Content-Type application/json', async () => {
    backendResponde({ cuerpo: {} });

    await solicitar('/categorias', { method: 'POST', body: '{"nombre":"Frutas"}' });

    assert.equal(cabecera(0, 'Content-Type'), 'application/json');
  });

  it('con FormData (fotos) deja que el navegador ponga el Content-Type', async () => {
    backendResponde({ cuerpo: {} });

    await solicitar('/productos/1/foto', { method: 'PUT', body: new FormData() });

    assert.equal(cabecera(0, 'Content-Type'), undefined);
  });
});
