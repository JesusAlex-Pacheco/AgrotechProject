import { Link } from 'react-router-dom';

// material-ui
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project imports
import AuthWrapper from 'sections/auth/AuthWrapper';
import AuthRegister from 'sections/auth/AuthRegister';

// ================================|| REGISTRO ||================================ //

export default function Register() {
  return (
    <AuthWrapper>
      <Grid container spacing={3}>
        <Grid size={12}>
          <Stack direction="row" sx={{ alignItems: 'baseline', justifyContent: 'space-between', mb: { xs: -0.5, sm: 0.5 } }}>
            <Typography variant="h3">Crear cuenta</Typography>
            <Typography component={Link} to="/login" variant="body1" sx={{ textDecoration: 'none', color: 'primary.main' }}>
              ¿Ya tiene cuenta?
            </Typography>
          </Stack>
        </Grid>
        <Grid size={12}>
          <AuthRegister />
        </Grid>
        <Grid size={12}>
          <Typography component={Link} to="/" variant="body2" sx={{ textDecoration: 'none', color: 'text.secondary' }}>
            ← Seguir sin crear cuenta
          </Typography>
        </Grid>
      </Grid>
    </AuthWrapper>
  );
}
