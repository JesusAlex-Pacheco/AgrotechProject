// material-ui
import Box from '@mui/material/Box';

// assets
// Version liviana de inicio.png (npm run imagenes)
import imagen from 'assets/images/inicio-app.webp';

// ==============================|| INICIO - IMAGEN PRINCIPAL ||============================== //

export default function IlustracionCampo() {
  return (
    <Box
      component="img"
      src={imagen}
      alt="Agricultor entregando su cosecha a un cliente"
      sx={{ width: '100%', maxWidth: 480, height: 'auto', display: 'block', mx: 'auto', borderRadius: 3 }}
    />
  );
}
