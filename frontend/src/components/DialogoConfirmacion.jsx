import PropTypes from 'prop-types';
import { useState } from 'react';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';

// ==============================|| DIALOGO DE CONFIRMACION ||============================== //

/**
 * Pide confirmacion antes de una accion. Si onConfirmar falla, muestra el
 * mensaje del error y deja el dialogo abierto.
 */
export default function DialogoConfirmacion({ abierto, titulo, mensaje, textoConfirmar = 'Confirmar', onConfirmar, onCerrar }) {
  const [enCurso, setEnCurso] = useState(false);
  const [error, setError] = useState(null);

  const cerrar = () => {
    setError(null);
    onCerrar();
  };

  const confirmar = async () => {
    setEnCurso(true);
    setError(null);
    try {
      await onConfirmar();
      cerrar();
    } catch (e) {
      setError(e.message);
    } finally {
      setEnCurso(false);
    }
  };

  return (
    <Dialog open={abierto} onClose={enCurso ? undefined : cerrar} maxWidth="xs" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <DialogContentText>{mensaje}</DialogContentText>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={cerrar} color="secondary" disabled={enCurso}>
          Cancelar
        </Button>
        <Button onClick={confirmar} variant="contained" color="error" disabled={enCurso}>
          {enCurso ? 'Procesando…' : textoConfirmar}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

DialogoConfirmacion.propTypes = {
  abierto: PropTypes.bool,
  titulo: PropTypes.string,
  mensaje: PropTypes.node,
  textoConfirmar: PropTypes.string,
  onConfirmar: PropTypes.func,
  onCerrar: PropTypes.func
};
