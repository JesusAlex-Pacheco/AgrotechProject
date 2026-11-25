import PropTypes from 'prop-types';
import { useState } from 'react';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Rating from '@mui/material/Rating';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';

// project imports
import { registrarResena } from 'api/agrotech';
import { mutate } from 'swr';

// ==============================|| PEDIDOS - RESEÑAR UN PRODUCTO ||============================== //

/** `producto` = { idProducto, nombre }. Se monta de nuevo cada vez que se abre (key en el padre). */
export default function DialogoResena({ producto, idCliente, onCerrar, onGuardada }) {
  const [calificacion, setCalificacion] = useState(5);
  const [comentario, setComentario] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);

  const guardar = async () => {
    setEnviando(true);
    setError(null);
    try {
      await registrarResena(producto.idProducto, { idCliente, calificacion, comentario: comentario.trim() || null });
      mutate(`/productos/${producto.idProducto}/resenas`);
      mutate(`/clientes/${idCliente}/resenas`);
      onGuardada(producto.nombre);
    } catch (e) {
      setError(e.message);
      setEnviando(false);
    }
  };

  return (
    <Dialog open={Boolean(producto)} onClose={onCerrar} fullWidth maxWidth="xs">
      <DialogTitle>Reseñar {producto?.nombre}</DialogTitle>
      <DialogContent dividers>
        <Stack sx={{ gap: 2 }}>
          {error && <Alert severity="error">{error}</Alert>}
          <Stack sx={{ gap: 0.5 }}>
            <Typography id="calificacion-resena" variant="subtitle2">
              Calificación
            </Typography>
            <Rating
              aria-labelledby="calificacion-resena"
              value={calificacion}
              onChange={(_, valor) => valor && setCalificacion(valor)}
              size="large"
            />
          </Stack>
          <TextField
            label="Comentario (opcional)"
            multiline
            minRows={3}
            value={comentario}
            onChange={(e) => setComentario(e.target.value)}
            error={comentario.length > 500}
            helperText={`${comentario.length}/500`}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCerrar} color="secondary">
          Cancelar
        </Button>
        <Button variant="contained" onClick={guardar} disabled={enviando || comentario.length > 500}>
          {enviando ? 'Enviando…' : 'Publicar reseña'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

DialogoResena.propTypes = {
  producto: PropTypes.object,
  idCliente: PropTypes.number,
  onCerrar: PropTypes.func,
  onGuardada: PropTypes.func
};
