import { Link } from 'react-router-dom';

// material-ui
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project imports
import AuthWrapper from 'sections/auth/AuthWrapper';
import AuthLogin from 'sections/auth/AuthLogin';

// ================================|| INICIAR SESION ||================================ //

export default function Login() {
  return (
    <AuthWrapper>
      <Grid container spacing={3}>
        <Grid size={12}>
          <Stack direction="row" sx={{ alignItems: 'baseline', justifyContent: 'space-between', mb: { xs: -0.5, sm: 0.5 } }}>
            <Typography variant="h3">Iniciar sesión</Typography>
            <Typography component={Link} to="/register" variant="body1" sx={{ textDecoration: 'none', color: 'primary.main' }}>
              ¿No tiene cuenta?
            </Typography>
          </Stack>
        </Grid>
        <Grid size={12}>
          <AuthLogin />
        </Grid>
        <Grid size={12}>
          <Typography component={Link} to="/" variant="body2" sx={{ textDecoration: 'none', color: 'text.secondary' }}>
            ← Seguir sin iniciar sesión
          </Typography>
        </Grid>
      </Grid>
    </AuthWrapper>
  );
}
