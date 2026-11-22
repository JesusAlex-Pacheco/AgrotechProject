import { useState } from 'react';

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
import DialogoConfirmacion from 'components/DialogoConfirmacion';
import { ChipEstado } from 'sections/pedidos/FilaPedido';
import { cambiarEstadoPedido, refrescarPedidos, useAgenda } from 'api/agrotech';
import useSesion from 'hooks/useSesion';
import { esTransportador } from 'utils/roles';
import { ESTADO_ENTREGA, dia } from 'utils/formato';

// ==============================|| ENTREGAS DEL TRANSPORTADOR ||============================== //

export default function Entregas() {
  const { sesion } = useSesion();
  const [estado, setEstado] = useState('');
  const [porEntregar, setPorEntregar] = useState(null);
  const [mensaje, setMensaje] = useState(null);

  const transportador = esTransportador(sesion);
  const { entregas, error, cargando, recargar } = useAgenda(transportador ? sesion.idPerfil : null, estado);

  if (!transportador) {
    return (
      <MainCard title="Mis entregas">
        <Alert severity="info">Esta pantalla es para los transportadores.</Alert>
      </MainCard>
    );
  }

  const marcarEntregado = async () => {
    await cambiarEstadoPedido(porEntregar.idPedido, 'ENTREGADO');
    await refrescarPedidos();
    setMensaje(`Pedido #${porEntregar.idPedido} entregado.`);
  };

  return (
    <MainCard title="Mis entregas" content={false}>
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, p: 2.5, alignItems: { sm: 'center' } }}>
        <TextField select size="small" label="Estado" value={estado} onChange={(e) => setEstado(e.target.value)} sx={{ minWidth: 200 }}>
          <MenuItem value="">Todas</MenuItem>
          {Object.entries(ESTADO_ENTREGA).map(([valor, info]) => (
            <MenuItem key={valor} value={valor}>
              {info.texto}
            </MenuItem>
          ))}
        </TextField>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          Su agenda por fecha. Cuando el agricultor despacha un pedido queda «En ruta» y usted lo marca como entregado al llevarlo.
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
          <Table aria-label="Entregas">
            <TableHead>
              <TableRow>
                <TableCell>Pedido</TableCell>
                <TableCell>Dirección</TableCell>
                <TableCell>Fecha programada</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell align="right">Acciones</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {entregas.map((entrega) => (
                <TableRow hover key={entrega.id}>
                  <TableCell>
                    <Typography variant="subtitle1">#{entrega.idPedido}</Typography>
                  </TableCell>
                  <TableCell sx={{ whiteSpace: 'normal !important', minWidth: 220 }}>{entrega.direccion}</TableCell>
                  <TableCell>{dia(entrega.fechaProgramada)}</TableCell>
                  <TableCell>
                    <ChipEstado estado={entrega.estado} tabla={ESTADO_ENTREGA} />
                    {entrega.fechaEntrega && (
                      <Typography variant="caption" sx={{ display: 'block', color: 'text.secondary' }}>
                        el {dia(entrega.fechaEntrega)}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell align="right">
                    {entrega.estado === 'EN_RUTA' && (
                      <Button variant="contained" size="small" onClick={() => setPorEntregar(entrega)}>
                        Marcar entregado
                      </Button>
                    )}
                    {entrega.estado === 'PROGRAMADA' && (
                      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                        Esperando despacho
                      </Typography>
                    )}
                  </TableCell>
                </TableRow>
              ))}

              {!cargando && entregas.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    {estado ? 'No hay entregas en este estado.' : 'Aún no tiene entregas asignadas.'}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <DialogoConfirmacion
        abierto={Boolean(porEntregar)}
        titulo={`Entregar el pedido #${porEntregar?.idPedido}`}
        mensaje={`Confirme que entregó el pedido en ${porEntregar?.direccion}. Si era contra entrega, el pago queda aprobado.`}
        textoConfirmar="Marcar entregado"
        onConfirmar={marcarEntregado}
        onCerrar={() => setPorEntregar(null)}
      />

      <Snackbar open={Boolean(mensaje)} autoHideDuration={4000} onClose={() => setMensaje(null)}>
        <Alert severity="success" variant="filled" onClose={() => setMensaje(null)}>
          {mensaje}
        </Alert>
      </Snackbar>
    </MainCard>
  );
}
