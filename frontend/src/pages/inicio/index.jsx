import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Avatar from '@mui/material/Avatar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CardActionArea from '@mui/material/CardActionArea';
import Grid from '@mui/material/Grid';
import Skeleton from '@mui/material/Skeleton';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project imports
import MainCard from 'components/MainCard';
import { esFotoValida } from 'components/FotoProducto';
import { estiloTono } from 'utils/tonos';
import IlustracionCampo from 'sections/inicio/IlustracionCampo';
import TarjetaProducto from 'sections/inicio/TarjetaProducto';
import DialogoProducto from 'sections/productos/DialogoProducto';
import { useCategorias, usePedidos, useProductos } from 'api/agrotech';
import useCarrito from 'hooks/useCarrito';
import useSesion from 'hooks/useSesion';
import { esAdmin, esAgricultor, esCliente, esTransportador, puedeComprar } from 'utils/roles';

// assets
import CarOutlined from '@ant-design/icons/CarOutlined';
import ShopOutlined from '@ant-design/icons/ShopOutlined';
import ShoppingCartOutlined from '@ant-design/icons/ShoppingCartOutlined';

// Por debajo de este stock un producto se considera para reabastecer
const STOCK_BAJO = 20;
const CANTIDAD_RECOMENDADOS = 8;

const PASOS = [
  { icono: ShopOutlined, titulo: 'El agricultor publica', texto: 'Cada finca sube su cosecha con precio, existencias y foto.' },
  { icono: ShoppingCartOutlined, titulo: 'Usted elige y paga', texto: 'Arme su carrito y pague por PSE, con tarjeta o contra entrega.' },
  { icono: CarOutlined, titulo: 'Llega a su casa', texto: 'Un transportador lleva el pedido y usted sigue su estado.' }
];

/**
 * Productos recomendados: los disponibles de las categorias que el cliente
 * ya compro; si no ha comprado, los mas recientes. Dentro de cada grupo van
 * primero los que tienen foto.
 */
function recomendar(productos, pedidos) {
  const comprados = new Set(pedidos.flatMap((pedido) => pedido.items.map((linea) => linea.idProducto)));
  const categorias = new Set(productos.filter((p) => comprados.has(p.id)).map((p) => p.idCategoria));
  const puntaje = (p) => (categorias.has(p.idCategoria) ? 2 : 0) + (esFotoValida(p.fotoUrl) ? 1 : 0);

  const lista = productos
    .filter((p) => p.disponible && Number(p.stock) > 0)
    .sort((a, b) => puntaje(b) - puntaje(a) || b.id - a.id)
    .slice(0, CANTIDAD_RECOMENDADOS);

  return { lista, segunCompras: categorias.size > 0 };
}

// ==============================|| INICIO - CIFRA ||============================== //

function Cifra({ titulo, valor, detalle, color = 'primary', cargando }) {
  return (
    <MainCard contentSX={{ p: 2.25 }} sx={{ height: '100%' }}>
      <Stack sx={{ gap: 0.5 }}>
        <Typography variant="h6" sx={{ color: 'text.secondary' }}>
          {titulo}
        </Typography>
        <Typography variant="h3" sx={{ color: `${color}.main` }}>
          {cargando ? <Skeleton width={60} /> : valor}
        </Typography>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {detalle}
        </Typography>
      </Stack>
    </MainCard>
  );
}

Cifra.propTypes = {
  titulo: PropTypes.string,
  valor: PropTypes.number,
  detalle: PropTypes.string,
  color: PropTypes.string,
  cargando: PropTypes.bool
};

// ==============================|| INICIO - BOTONES DEL ENCABEZADO ||============================== //

/** Las acciones principales cambian segun quien entra. */
function AccionesPrincipales({ sesion }) {
  const boton = (texto, ruta, principal) => (
    <Button key={ruta + texto} component={RouterLink} to={ruta} variant={principal ? 'contained' : 'outlined'} size="large">
      {texto}
    </Button>
  );

  let botones;
  if (esAgricultor(sesion)) botones = [boton('Publicar un producto', '/productos', true), boton('Mis ventas', '/pedidos')];
  else if (esTransportador(sesion)) botones = [boton('Mis entregas', '/entregas', true), boton('Ver productos', '/productos')];
  else if (esAdmin(sesion)) botones = [boton('Ver pedidos', '/pedidos', true), boton('Ver productos', '/productos')];
  else if (esCliente(sesion)) botones = [boton('Ver productos', '/productos', true), boton('Mis compras', '/pedidos')];
  else botones = [boton('Ver productos', '/productos', true), boton('Crear cuenta', '/register')];

  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 1.5 }}>
      {botones}
    </Stack>
  );
}

AccionesPrincipales.propTypes = { sesion: PropTypes.object };

// ==============================|| INICIO ||============================== //

export default function Inicio() {
  const { sesion } = useSesion();
  const { agregar } = useCarrito();
  const { productos, error, cargando: cargandoProductos } = useProductos();
  const { categorias, cargando: cargandoCategorias } = useCategorias();
  // Compras del cliente (para recomendar) o ventas del agricultor / todos los pedidos (ADMIN)
  const { pedidos, cargando: cargandoPedidos } = usePedidos(sesion);
  const [enFicha, setEnFicha] = useState(null);
  const [agregado, setAgregado] = useState(null);

  const cargando = cargandoProductos || cargandoCategorias;
  const nombreCategoria = useMemo(() => new Map(categorias.map((c) => [c.id, c.nombre])), [categorias]);
  const recomendados = useMemo(() => recomendar(productos, esCliente(sesion) ? pedidos : []), [productos, pedidos, sesion]);

  const disponibles = productos.filter((p) => p.disponible);
  const fincas = new Set(disponibles.map((p) => p.idAgricultor)).size;
  const porCategoria = (id) => disponibles.filter((p) => p.idCategoria === id).length;
  const porAtender = pedidos.filter((p) => p.estado === 'CREADO' || p.estado === 'PAGADO').length;
  const compra = puedeComprar(sesion);

  const agregarAlCarrito = (producto) => {
    agregar(producto, 1);
    setAgregado(producto.nombre);
  };

  // Resumen de trabajo para quien vende o administra
  const misProductos = esAgricultor(sesion) ? productos.filter((p) => p.idAgricultor === sesion.idPerfil) : productos;
  const resumen = (esAgricultor(sesion) || esAdmin(sesion)) && (
    <Grid size={12}>
      <Typography variant="h5" sx={{ mb: 1.5 }}>
        {esAgricultor(sesion) ? 'Resumen de su finca' : 'Resumen de AgroTech'}
      </Typography>
      <Grid container spacing={2.75}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <Cifra
            titulo={esAgricultor(sesion) ? 'Sus productos' : 'Productos'}
            valor={misProductos.length}
            detalle="Publicados en el catálogo"
            cargando={cargando}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <Cifra
            titulo="Disponibles"
            valor={misProductos.filter((p) => p.disponible).length}
            detalle="Con existencias para vender"
            color="success"
            cargando={cargando}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <Cifra
            titulo="Stock bajo"
            valor={misProductos.filter((p) => p.disponible && Number(p.stock) < STOCK_BAJO).length}
            detalle={`Disponibles con menos de ${STOCK_BAJO} unidades`}
            color="warning"
            cargando={cargando}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <Cifra
            titulo="Pedidos por atender"
            valor={porAtender}
            detalle="Creados o pagados, sin despachar"
            color="error"
            cargando={cargandoPedidos}
          />
        </Grid>
      </Grid>
    </Grid>
  );

  return (
    <Grid container rowSpacing={4.5} columnSpacing={2.75}>
      {/* ------------------------- Encabezado principal ------------------------- */}
      <Grid size={12}>
        <Box
          sx={(theme) => ({
            borderRadius: 3,
            overflow: 'hidden',
            px: { xs: 3, md: 6 },
            py: { xs: 4, md: 5 },
            background: 'linear-gradient(135deg, #f1f8e9 0%, #e8f5e9 45%, #fffde7 100%)',
            border: '1px solid',
            borderColor: 'divider',
            // Version nocturna del mismo degradado (verde y trigo apagados)
            ...theme.applyStyles('dark', {
              background: 'linear-gradient(135deg, #18241a 0%, #1b2e1c 45%, #2a2716 100%)'
            })
          })}
        >
          <Grid container spacing={4} sx={{ alignItems: 'center' }}>
            <Grid size={{ xs: 12, md: 5 }}>
              <IlustracionCampo />
            </Grid>
            <Grid size={{ xs: 12, md: 7 }}>
              <Stack sx={{ gap: 2.5 }}>
                <Typography variant="overline" sx={{ color: 'primary.main', fontWeight: 700, letterSpacing: 1.5 }}>
                  Mercado campesino en línea
                </Typography>
                <Typography
                  component="h1"
                  sx={{ fontSize: { xs: '2.2rem', md: '3.2rem' }, fontWeight: 800, lineHeight: 1.1, color: 'text.primary' }}
                >
                  Del campo a tu mesa,{' '}
                  <Box component="span" sx={{ color: 'primary.main' }}>
                    sin intermediarios
                  </Box>
                </Typography>
                <Typography variant="h5" sx={{ fontWeight: 400, color: 'text.secondary', maxWidth: 520 }}>
                  Compre frutas, verduras y tubérculos directamente a los agricultores de la región: precio justo para quien cultiva y
                  producto fresco para su casa.
                </Typography>
                <AccionesPrincipales sesion={sesion} />
                <Stack direction="row" sx={{ gap: { xs: 3, sm: 5 }, pt: 1, flexWrap: 'wrap' }}>
                  {[
                    [disponibles.length, 'productos frescos'],
                    [fincas, fincas === 1 ? 'finca vendiendo' : 'fincas vendiendo'],
                    [categorias.length, 'categorías']
                  ].map(([valor, texto]) => (
                    <Stack key={texto}>
                      <Typography variant="h3">{cargando ? <Skeleton width={40} /> : valor}</Typography>
                      <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                        {texto}
                      </Typography>
                    </Stack>
                  ))}
                </Stack>
              </Stack>
            </Grid>
          </Grid>
        </Box>
      </Grid>

      {error && (
        <Grid size={12}>
          <Alert severity="error">{error.message}</Alert>
        </Grid>
      )}

      {resumen}

      {/* ------------------------- Productos recomendados ------------------------- */}
      <Grid size={12}>
        <Stack direction="row" sx={{ alignItems: 'flex-end', justifyContent: 'space-between', mb: 1.5, gap: 2 }}>
          <Stack>
            <Typography variant="h4">{recomendados.segunCompras ? 'Recomendados para usted' : 'Recién cosechados'}</Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {recomendados.segunCompras ? 'Según lo que ha comprado antes' : 'Lo más reciente que publicaron las fincas'}
            </Typography>
          </Stack>
          <Button component={RouterLink} to="/productos">
            Ver todos
          </Button>
        </Stack>
        <Grid container spacing={2.75}>
          {cargando &&
            [1, 2, 3, 4].map((n) => (
              <Grid key={n} size={{ xs: 12, sm: 6, md: 4, lg: 3 }}>
                <Skeleton variant="rounded" height={290} />
              </Grid>
            ))}
          {recomendados.lista.map((producto) => (
            <Grid key={producto.id} size={{ xs: 12, sm: 6, md: 4, lg: 3 }}>
              <TarjetaProducto
                producto={producto}
                nombreCategoria={nombreCategoria.get(producto.idCategoria)}
                puedeComprar={compra}
                onVer={setEnFicha}
                onAgregar={agregarAlCarrito}
              />
            </Grid>
          ))}
          {!cargando && recomendados.lista.length === 0 && (
            <Grid size={12}>
              <Typography sx={{ color: 'text.secondary' }}>Aún no hay productos disponibles.</Typography>
            </Grid>
          )}
        </Grid>
      </Grid>

      {/* ------------------------- Categorias ------------------------- */}
      {categorias.length > 0 && (
        <Grid size={12}>
          <Typography variant="h4" sx={{ mb: 1.5 }}>
            Explore por categoría
          </Typography>
          <Grid container spacing={2}>
            {categorias.map((categoria, indice) => (
              <Grid key={categoria.id} size={{ xs: 6, sm: 4, md: 3, lg: 2 }}>
                <MainCard content={false}>
                  <CardActionArea component={RouterLink} to={`/productos?categoria=${categoria.id}`} sx={{ p: 2 }}>
                    <Stack sx={{ alignItems: 'center', gap: 1, textAlign: 'center' }}>
                      <Avatar sx={[{ width: 48, height: 48, fontWeight: 700 }, estiloTono(indice)]}>
                        {categoria.nombre.charAt(0).toUpperCase()}
                      </Avatar>
                      <Typography variant="subtitle1">{categoria.nombre}</Typography>
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                        {porCategoria(categoria.id)} {porCategoria(categoria.id) === 1 ? 'producto' : 'productos'}
                      </Typography>
                    </Stack>
                  </CardActionArea>
                </MainCard>
              </Grid>
            ))}
          </Grid>
        </Grid>
      )}

      {/* ------------------------- Como funciona (para quien compra) ------------------------- */}
      {compra && (
        <Grid size={12}>
          <Typography variant="h4" sx={{ mb: 1.5 }}>
            Cómo funciona
          </Typography>
          <Grid container spacing={2.75}>
            {PASOS.map(({ icono: Icono, titulo, texto }, indice) => (
              <Grid key={titulo} size={{ xs: 12, md: 4 }}>
                <MainCard sx={{ height: '100%' }}>
                  <Stack direction="row" sx={{ gap: 2, alignItems: 'flex-start' }}>
                    <Avatar sx={{ bgcolor: 'primary.lighter', color: 'primary.main', width: 48, height: 48 }}>
                      <Icono style={{ fontSize: '1.4rem' }} />
                    </Avatar>
                    <Stack sx={{ gap: 0.5 }}>
                      <Typography variant="h5">
                        {indice + 1}. {titulo}
                      </Typography>
                      <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                        {texto}
                      </Typography>
                    </Stack>
                  </Stack>
                </MainCard>
              </Grid>
            ))}
          </Grid>
        </Grid>
      )}

      <DialogoProducto producto={enFicha} nombreCategoria={nombreCategoria.get(enFicha?.idCategoria)} onCerrar={() => setEnFicha(null)} />

      <Snackbar open={Boolean(agregado)} autoHideDuration={3000} onClose={() => setAgregado(null)}>
        <Alert
          severity="success"
          variant="filled"
          onClose={() => setAgregado(null)}
          action={
            <Button component={RouterLink} to="/carrito" color="inherit" size="small">
              Ver carrito
            </Button>
          }
        >
          «{agregado}» se agregó al carrito
        </Alert>
      </Snackbar>
    </Grid>
  );
}
