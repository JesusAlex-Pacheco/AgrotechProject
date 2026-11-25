import PropTypes from 'prop-types';
import { useState } from 'react';

// material-ui
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Collapse from '@mui/material/Collapse';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';

// project imports
import DialogoConfirmacion from 'components/DialogoConfirmacion';
import { cambiarEstadoPedido, refrescarPedidos, refrescarProductos, useEntrega } from 'api/agrotech';
import { esAdmin, esAgricultor, esCliente, gestionaVentas } from 'utils/roles';
import { ESTADO_ENTREGA, ESTADO_PAGO, ESTADO_PEDIDO, METODO_PAGO, cantidad, dia, fechaHora, pesos } from 'utils/formato';

// assets
import DownOutlined from '@ant-design/icons/DownOutlined';
import UpOutlined from '@ant-design/icons/UpOutlined';

export function ChipEstado({ estado, tabla = ESTADO_PEDIDO }) {
  const info = tabla[estado] ?? { texto: estado, color: 'default' };
  return <Chip label={info.texto} color={info.color} size="small" variant="light" />;
}

ChipEstado.propTypes = { estado: PropTypes.string, tabla: PropTypes.object };

// ==============================|| PEDIDOS - ACCIONES ||============================== //

/**
 * Que puede hacer cada rol segun el estado. Refleja las reglas del backend
 * (EstadoPedido, PedidoServiceImpl y ControlAcceso); si algo no cuadra, el
 * backend responde con el motivo y se muestra aqui.
 */
function accionesDisponibles(sesion, pedido, entrega) {
  const abierto = pedido.estado === 'CREADO' || pedido.estado === 'PAGADO';
  const contraEntrega = pedido.metodoPago === 'CONTRA_ENTREGA';
  const acciones = [];

  if (gestionaVentas(sesion)) {
    if (pedido.estado === 'CREADO' && !contraEntrega) acciones.push({ id: 'PAGADO', texto: 'Confirmar pago', variant: 'contained' });
    if (abierto && !entrega) acciones.push({ id: 'ENTREGA', texto: 'Programar entrega', variant: 'contained' });
    if (entrega && (pedido.estado === 'PAGADO' || (pedido.estado === 'CREADO' && contraEntrega))) {
      acciones.push({ id: 'EN_RUTA', texto: 'Despachar', variant: 'contained' });
    }
  }
  if (esAdmin(sesion) && pedido.estado === 'EN_RUTA') acciones.push({ id: 'ENTREGADO', texto: 'Marcar entregado', variant: 'contained' });
  if (abierto && (esCliente(sesion) || gestionaVentas(sesion)))
    acciones.push({ id: 'CANCELADO', texto: 'Cancelar pedido', variant: 'outlined', color: 'error' });

  return acciones;
}

// ==============================|| PEDIDOS - FILA ||============================== //

export default function FilaPedido({ pedido, sesion, productos, resenados, onProgramarEntrega, onResenar }) {
  const [abierta, setAbierta] = useState(false);
  const [trabajando, setTrabajando] = useState(false);
  const [error, setError] = useState(null);
  const [porCancelar, setPorCancelar] = useState(false);

  // La entrega solo se consulta al abrir el detalle
  const { entrega, cargando: cargandoEntrega } = useEntrega(abierta ? pedido.id : null);
  const acciones = abierta && !cargandoEntrega ? accionesDisponibles(sesion, pedido, entrega) : [];

  const cambiarEstado = async (estado) => {
    setTrabajando(true);
    setError(null);
    try {
      await cambiarEstadoPedido(pedido.id, estado);
      await refrescarPedidos();
      // Cancelar devuelve el inventario al catalogo
      if (estado === 'CANCELADO') refrescarProductos();
    } catch (e) {
      setError(e.message);
      throw e;
    } finally {
      setTrabajando(false);
    }
  };

  const ejecutar = (accion) => {
    if (accion.id === 'ENTREGA') onProgramarEntrega(pedido);
    else if (accion.id === 'CANCELADO') setPorCancelar(true);
    else cambiarEstado(accion.id).catch(() => {});
  };

  const nombreProducto = (id) => productos.get(id)?.nombre ?? `Producto #${id}`;
  const esDeMiFinca = (id) => esAgricultor(sesion) && productos.get(id)?.idAgricultor === sesion.idPerfil;

  return (
    <>
      <TableRow hover sx={{ '& > td': { borderBottom: abierta ? 'none' : undefined } }}>
        <TableCell padding="checkbox">
          <IconButton aria-label={abierta ? 'Ocultar detalle' : 'Ver detalle'} size="small" onClick={() => setAbierta(!abierta)}>
            {abierta ? <UpOutlined /> : <DownOutlined />}
          </IconButton>
        </TableCell>
        <TableCell>
          <Typography variant="subtitle1">#{pedido.id}</Typography>
        </TableCell>
        <TableCell>{fechaHora(pedido.fecha)}</TableCell>
        {!esCliente(sesion) && <TableCell>Cliente #{pedido.idCliente}</TableCell>}
        <TableCell>
          <ChipEstado estado={pedido.estado} />
        </TableCell>
        <TableCell>
          {METODO_PAGO[pedido.metodoPago] ?? pedido.metodoPago}
          <Typography variant="caption" sx={{ display: 'block', color: 'text.secondary' }}>
            Pago {ESTADO_PAGO[pedido.estadoPago] ?? pedido.estadoPago}
          </Typography>
        </TableCell>
        <TableCell align="right">
          <Typography variant="subtitle1">{pesos(pedido.total)}</Typography>
        </TableCell>
      </TableRow>

      <TableRow>
        <TableCell colSpan={7} sx={{ py: 0, bgcolor: abierta ? 'grey.50' : undefined }}>
          <Collapse in={abierta} timeout="auto" unmountOnExit>
            <Box sx={{ py: 2 }}>
              {(trabajando || cargandoEntrega) && <LinearProgress sx={{ mb: 2 }} />}
              <Grid container spacing={3}>
                <Grid size={{ xs: 12, md: 7 }}>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    Productos
                  </Typography>
                  <Table size="small" aria-label={`Productos del pedido ${pedido.id}`}>
                    <TableHead>
                      <TableRow>
                        <TableCell>Producto</TableCell>
                        <TableCell align="right">Cantidad</TableCell>
                        <TableCell align="right">Precio</TableCell>
                        <TableCell align="right">Subtotal</TableCell>
                        {esCliente(sesion) && pedido.estado === 'ENTREGADO' && <TableCell />}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {pedido.items.map((linea) => (
                        <TableRow key={linea.idProducto}>
                          <TableCell>
                            {nombreProducto(linea.idProducto)}
                            {esDeMiFinca(linea.idProducto) && (
                              <Chip label="Suyo" size="small" color="primary" variant="light" sx={{ ml: 1 }} />
                            )}
                          </TableCell>
                          <TableCell align="right">
                            {cantidad(linea.cantidad)} {productos.get(linea.idProducto)?.unidadMedida ?? ''}
                          </TableCell>
                          <TableCell align="right">{pesos(linea.precioUnitario)}</TableCell>
                          <TableCell align="right">{pesos(linea.subtotal)}</TableCell>
                          {esCliente(sesion) && pedido.estado === 'ENTREGADO' && (
                            <TableCell align="right">
                              {/* Solo se resena una vez cada producto: si ya lo hizo, se indica */}
                              {resenados?.has(linea.idProducto) ? (
                                <Chip label="Reseñado" size="small" color="success" variant="light" />
                              ) : (
                                <Button
                                  size="small"
                                  onClick={() => onResenar({ idProducto: linea.idProducto, nombre: nombreProducto(linea.idProducto) })}
                                >
                                  Reseñar
                                </Button>
                              )}
                            </TableCell>
                          )}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <Stack sx={{ alignItems: 'flex-end', mt: 1, gap: 0.25 }}>
                    <Typography variant="body2">Subtotal: {pesos(pedido.subtotal)}</Typography>
                    <Typography variant="body2">Envío: {pesos(pedido.costoEnvio)}</Typography>
                    <Typography variant="subtitle1">Total: {pesos(pedido.total)}</Typography>
                  </Stack>
                </Grid>

                <Grid size={{ xs: 12, md: 5 }}>
                  <Typography variant="h6" sx={{ mb: 1 }}>
                    Entrega
                  </Typography>
                  {!cargandoEntrega && !entrega && (
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {pedido.estado === 'CANCELADO' ? 'El pedido se canceló.' : 'Aún no tiene entrega programada.'}
                    </Typography>
                  )}
                  {entrega && (
                    <Stack sx={{ gap: 0.75 }}>
                      <ChipEstado estado={entrega.estado} tabla={ESTADO_ENTREGA} />
                      <Typography variant="body2">
                        <strong>Transportador:</strong> {entrega.nombreTransportador} · {entrega.placa}
                      </Typography>
                      <Typography variant="body2">
                        <strong>Dirección:</strong> {entrega.direccion}
                      </Typography>
                      <Typography variant="body2">
                        <strong>Fecha programada:</strong> {dia(entrega.fechaProgramada)}
                      </Typography>
                      {entrega.fechaEntrega && (
                        <Typography variant="body2">
                          <strong>Entregado el:</strong> {dia(entrega.fechaEntrega)}
                        </Typography>
                      )}
                    </Stack>
                  )}
                </Grid>

                {(error || acciones.length > 0) && (
                  <Grid size={12}>
                    {error && (
                      <Alert severity="error" sx={{ mb: 1.5 }} onClose={() => setError(null)}>
                        {error}
                      </Alert>
                    )}
                    <Stack direction="row" sx={{ gap: 1, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                      {acciones.map((accion) => (
                        <Button
                          key={accion.id}
                          variant={accion.variant}
                          color={accion.color ?? 'primary'}
                          disabled={trabajando}
                          onClick={() => ejecutar(accion)}
                        >
                          {accion.texto}
                        </Button>
                      ))}
                    </Stack>
                  </Grid>
                )}
              </Grid>
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>

      <DialogoConfirmacion
        abierto={porCancelar}
        titulo={`Cancelar el pedido #${pedido.id}`}
        mensaje="Los productos vuelven al inventario. Si el pago estaba pendiente, queda rechazado; si ya estaba aprobado, el reembolso se gestiona por fuera de AgroTech."
        textoConfirmar="Cancelar pedido"
        onConfirmar={() => cambiarEstado('CANCELADO')}
        onCerrar={() => setPorCancelar(false)}
      />
    </>
  );
}

FilaPedido.propTypes = {
  pedido: PropTypes.object,
  sesion: PropTypes.object,
  productos: PropTypes.instanceOf(Map),
  resenados: PropTypes.instanceOf(Set),
  onProgramarEntrega: PropTypes.func,
  onResenar: PropTypes.func
};
