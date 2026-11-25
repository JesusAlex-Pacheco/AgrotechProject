import { useMemo, useState } from 'react';
import { Link as RouterLink, Navigate, useLocation } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import LinearProgress from '@mui/material/LinearProgress';
import MenuItem from '@mui/material/MenuItem';
import Snackbar from '@mui/material/Snackbar';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

// project imports
import MainCard from 'components/MainCard';
import FilaPedido from 'sections/pedidos/FilaPedido';
import DialogoEntrega from 'sections/pedidos/DialogoEntrega';
import DialogoResena from 'sections/pedidos/DialogoResena';
import { useProductos, usePedidos, useResenasCliente } from 'api/agrotech';
import useSesion from 'hooks/useSesion';
import { esAgricultor, esCliente, esTransportador } from 'utils/roles';
import { ESTADO_PEDIDO } from 'utils/formato';

const TITULO = { CLIENTE: 'Mis compras', AGRICULTOR: 'Mis ventas', ADMIN: 'Todos los pedidos' };

const AYUDA = {
  CLIENTE: 'Abra un pedido para ver su detalle, cancelarlo mientras no haya salido o reseñar lo que recibió.',
  AGRICULTOR:
    'Pedidos que incluyen sus productos. Abra cada uno para confirmar el pago, programar la entrega con un transportador y despacharlo.',
  ADMIN: 'Todos los pedidos de AgroTech. Como administrador puede hacer cualquier cambio de estado.'
};

// ==============================|| PEDIDOS ||============================== //

export default function Pedidos() {
  const { sesion } = useSesion();
  const location = useLocation();
  const [estado, setEstado] = useState('');
  const [mensaje, setMensaje] = useState(location.state?.mensaje ?? null);
  const [paraEntrega, setParaEntrega] = useState(null);
  const [paraResena, setParaResena] = useState(null);

  const { pedidos, error, cargando, recargar } = usePedidos(sesion, estado);
  // El catalogo trae todos los productos (tambien los retirados): da nombres y unidades
  const { productos } = useProductos();
  const porId = useMemo(() => new Map(productos.map((p) => [p.id, p])), [productos]);
  // Productos que el cliente ya reseño: en lugar del boton "Reseñar" se marca "Reseñado"
  const { resenas } = useResenasCliente(esCliente(sesion) ? sesion.idPerfil : null);
  const resenados = useMemo(() => new Set(resenas.map((r) => r.idProducto)), [resenas]);

  if (!sesion) {
    return (
      <MainCard title="Pedidos">
        <Alert
          severity="info"
          action={
            <Button component={RouterLink} to="/login" state={{ desde: '/pedidos' }} color="inherit" size="small">
              Iniciar sesión
            </Button>
          }
        >
          Inicie sesión para ver sus pedidos.
        </Alert>
      </MainCard>
    );
  }

  // El transportador tiene su propia pantalla
  if (esTransportador(sesion)) return <Navigate to="/entregas" replace />;

  return (
    <MainCard title={TITULO[sesion.rol] ?? 'Pedidos'} content={false}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, p: 2.5, alignItems: { sm: 'center' } }}>
        <TextField select size="small" label="Estado" value={estado} onChange={(e) => setEstado(e.target.value)} sx={{ minWidth: 200 }}>
          <MenuItem value="">Todos</MenuItem>
          {Object.entries(ESTADO_PEDIDO).map(([valor, info]) => (
            <MenuItem key={valor} value={valor}>
              {info.texto}
            </MenuItem>
          ))}
        </TextField>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {AYUDA[sesion.rol]}
        </Typography>
      </Stack>

      {cargando && <LinearProgress />}

      {error && (
        <Alert
          severity="error"
          sx={{ mx: 2.5, mb: 2.5 }}
          action={
            <Button color="inherit" size="small" onClick={() => recargar()}>
              Reintentar
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}

      {!error && (
        <TableContainer sx={{ width: '100%', overflowX: 'auto', '& td, & th': { whiteSpace: 'nowrap' } }}>
          <Table aria-label="Pedidos">
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox" />
                <TableCell>Pedido</TableCell>
                <TableCell>Fecha</TableCell>
                {!esCliente(sesion) && <TableCell>Cliente</TableCell>}
                <TableCell>Estado</TableCell>
                <TableCell>Pago</TableCell>
                <TableCell align="right">Total</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {pedidos.map((pedido) => (
                <FilaPedido
                  key={pedido.id}
                  pedido={pedido}
                  sesion={sesion}
                  productos={porId}
                  resenados={resenados}
                  onProgramarEntrega={setParaEntrega}
                  onResenar={setParaResena}
                />
              ))}

              {!cargando && pedidos.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    {estado ? 'No hay pedidos en este estado.' : 'Aún no hay pedidos.'}
                    {esCliente(sesion) && !estado && (
                      <>
                        {' '}
                        <Button component={RouterLink} to="/productos" size="small">
                          Ver productos
                        </Button>
                      </>
                    )}
                    {esAgricultor(sesion) && !estado && ' Aparecerán aquí cuando un cliente compre sus productos.'}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <DialogoEntrega pedido={paraEntrega} onCerrar={() => setParaEntrega(null)} />

      {paraResena && (
        <DialogoResena
          key={paraResena.idProducto}
          producto={paraResena}
          idCliente={sesion.idPerfil}
          onCerrar={() => setParaResena(null)}
          onGuardada={(nombre) => {
            setParaResena(null);
            setMensaje(`Gracias: su reseña de «${nombre}» quedó publicada. Puede verla en Mis reseñas.`);
          }}
        />
      )}

      <Snackbar open={Boolean(mensaje)} autoHideDuration={5000} onClose={() => setMensaje(null)}>
        <Alert severity="success" variant="filled" onClose={() => setMensaje(null)}>
          {mensaje}
        </Alert>
      </Snackbar>
    </MainCard>
  );
}
