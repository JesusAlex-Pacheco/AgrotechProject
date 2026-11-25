import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

// project imports
import MainCard from 'components/MainCard';
import { refrescarPedidos, refrescarProductos, registrarPedido } from 'api/agrotech';
import useCarrito from 'hooks/useCarrito';
import useSesion from 'hooks/useSesion';
import { esCliente, puedeComprar } from 'utils/roles';
import { METODO_PAGO, cantidad as formatoCantidad, pesos } from 'utils/formato';

// assets
import DeleteOutlined from '@ant-design/icons/DeleteOutlined';

// ==============================|| CARRITO ||============================== //

export default function Carrito() {
  const { sesion } = useSesion();
  const { items, total, cambiarCantidad, quitar, vaciar } = useCarrito();
  const navigate = useNavigate();
  const [metodoPago, setMetodoPago] = useState('PSE');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  // Una cantidad es valida si es mayor que cero y no supera las existencias
  const cantidadInvalida = (item) => !(Number(item.cantidad) > 0) || Number(item.cantidad) > item.stock;
  const hayErrores = items.some(cantidadInvalida);

  const comprar = async () => {
    setEnviando(true);
    setError(null);
    try {
      const pedido = await registrarPedido({
        idCliente: sesion.idPerfil,
        metodoPago,
        items: items.map((item) => ({ idProducto: item.idProducto, cantidad: Number(item.cantidad) }))
      });
      vaciar();
      // El inventario cambio: se recargan el catalogo y los pedidos
      refrescarProductos();
      refrescarPedidos();
      navigate('/pedidos', { state: { mensaje: `Pedido #${pedido.id} registrado por ${pesos(pedido.total)}.` } });
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  };

  if (!puedeComprar(sesion)) {
    return (
      <MainCard title="Carrito">
        <Alert severity="info">Solo los clientes compran productos. Su cuenta es de otro tipo.</Alert>
      </MainCard>
    );
  }

  if (items.length === 0) {
    return (
      <MainCard title="Carrito">
        <Stack sx={{ gap: 2, alignItems: 'flex-start' }}>
          <Typography>Su carrito está vacío.</Typography>
          <Button component={RouterLink} to="/productos" variant="contained">
            Ver productos
          </Button>
        </Stack>
      </MainCard>
    );
  }

  return (
    <MainCard title="Carrito" content={false}>
      <TableContainer sx={{ width: '100%', overflowX: 'auto' }}>
        <Table aria-label="Productos del carrito">
          <TableHead>
            <TableRow>
              <TableCell>Producto</TableCell>
              <TableCell align="right">Precio</TableCell>
              <TableCell sx={{ width: 170 }}>Cantidad</TableCell>
              <TableCell align="right">Subtotal</TableCell>
              <TableCell align="right" />
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((item) => (
              <TableRow key={item.idProducto}>
                <TableCell>
                  <Typography variant="subtitle1">{item.nombre}</Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                    Disponible: {formatoCantidad(item.stock)} {item.unidadMedida}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  {pesos(item.precio)} / {item.unidadMedida}
                </TableCell>
                <TableCell>
                  <TextField
                    size="small"
                    type="number"
                    value={item.cantidad}
                    onChange={(e) => cambiarCantidad(item.idProducto, e.target.value)}
                    error={cantidadInvalida(item)}
                    helperText={cantidadInvalida(item) ? `Entre 0,01 y ${formatoCantidad(item.stock)}` : item.unidadMedida}
                    slotProps={{ htmlInput: { min: 0.01, max: item.stock, step: 0.5, 'aria-label': `Cantidad de ${item.nombre}` } }}
                  />
                </TableCell>
                <TableCell align="right">{pesos(item.precio * (Number(item.cantidad) || 0))}</TableCell>
                <TableCell align="right">
                  <Tooltip title="Quitar">
                    <IconButton aria-label={`Quitar ${item.nombre}`} color="error" onClick={() => quitar(item.idProducto)}>
                      <DeleteOutlined />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Stack sx={{ p: 2.5, gap: 2 }}>
        {error && <Alert severity="error">{error}</Alert>}

        <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, alignItems: { sm: 'center' }, justifyContent: 'space-between' }}>
          <TextField
            select
            size="small"
            label="Forma de pago"
            value={metodoPago}
            onChange={(e) => setMetodoPago(e.target.value)}
            sx={{ minWidth: 220 }}
          >
            {Object.entries(METODO_PAGO).map(([valor, texto]) => (
              <MenuItem key={valor} value={valor}>
                {texto}
              </MenuItem>
            ))}
          </TextField>
          <Typography variant="h4">Total: {pesos(total)}</Typography>
        </Stack>

        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {metodoPago === 'CONTRA_ENTREGA' ? 'Paga al recibir el pedido.' : 'El agricultor confirma el pago antes de despachar el pedido.'}
        </Typography>

        {esCliente(sesion) ? (
          <Button
            variant="contained"
            size="large"
            onClick={comprar}
            disabled={enviando || hayErrores}
            sx={{ alignSelf: { sm: 'flex-end' } }}
          >
            {enviando ? 'Registrando el pedido…' : 'Confirmar compra'}
          </Button>
        ) : (
          <Alert
            severity="info"
            action={
              <Button component={RouterLink} to="/login" state={{ desde: '/carrito' }} color="inherit" size="small">
                Iniciar sesión
              </Button>
            }
          >
            Para comprar, inicie sesión con una cuenta de cliente. Su carrito se conserva.
          </Alert>
        )}
      </Stack>
    </MainCard>
  );
}
