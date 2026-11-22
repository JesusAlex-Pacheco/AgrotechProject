// material-ui
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Snackbar from '@mui/material/Snackbar';

// project imports
import Carrito from './Carrito';
import ModoColor from './ModoColor';
import Sesion from './Sesion';
import useSesion from 'hooks/useSesion';

// ==============================|| HEADER - CONTENT ||============================== //

export default function HeaderContent() {
  const { aviso, descartarAviso } = useSesion();

  return (
    <>
      {/* Espacio flexible: empuja el carrito y la sesion a la derecha */}
      <Box sx={{ width: '100%', ml: 1 }} />
      <ModoColor />
      <Carrito />
      <Sesion />

      {/* Aviso cuando el backend rechaza un token vencido */}
      <Snackbar
        open={Boolean(aviso)}
        autoHideDuration={6000}
        onClose={descartarAviso}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity="warning" variant="filled" onClose={descartarAviso}>
          {aviso}
        </Alert>
      </Snackbar>
    </>
  );
}
