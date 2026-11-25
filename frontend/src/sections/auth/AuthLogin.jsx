import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import FormHelperText from '@mui/material/FormHelperText';
import Grid from '@mui/material/Grid';
import InputAdornment from '@mui/material/InputAdornment';
import InputLabel from '@mui/material/InputLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import Stack from '@mui/material/Stack';

// third-party
import * as Yup from 'yup';
import { Formik } from 'formik';

// project imports
import IconButton from 'components/@extended/IconButton';
import AnimateButton from 'components/@extended/AnimateButton';
import useSesion from 'hooks/useSesion';

// assets
import EyeOutlined from '@ant-design/icons/EyeOutlined';
import EyeInvisibleOutlined from '@ant-design/icons/EyeInvisibleOutlined';

const esquema = Yup.object().shape({
  correo: Yup.string().trim().email('El correo no tiene un formato válido').required('El correo es obligatorio'),
  clave: Yup.string().required('La clave es obligatoria')
});

// ============================|| INICIAR SESION - FORMULARIO ||============================ //

export default function AuthLogin() {
  const { iniciarSesion } = useSesion();
  const navigate = useNavigate();
  const location = useLocation();
  const [mostrarClave, setMostrarClave] = useState(false);

  const enviar = async (valores, { setStatus }) => {
    setStatus(null);
    try {
      await iniciarSesion(valores.correo.trim(), valores.clave);
      // Vuelve a la pagina desde la que se pidio iniciar sesion (por ejemplo, el carrito)
      navigate(location.state?.desde ?? '/');
    } catch (error) {
      setStatus(error.message);
    }
  };

  return (
    <Formik initialValues={{ correo: '', clave: '' }} validationSchema={esquema} onSubmit={enviar}>
      {({ errors, handleBlur, handleChange, handleSubmit, isSubmitting, status, touched, values }) => (
        <form noValidate onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            {status && (
              <Grid size={12}>
                <Alert severity="error">{status}</Alert>
              </Grid>
            )}
            <Grid size={12}>
              <Stack sx={{ gap: 1 }}>
                <InputLabel htmlFor="correo-login">Correo</InputLabel>
                <OutlinedInput
                  id="correo-login"
                  type="email"
                  name="correo"
                  autoComplete="email"
                  value={values.correo}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  placeholder="correo@ejemplo.com"
                  fullWidth
                  error={Boolean(touched.correo && errors.correo)}
                />
              </Stack>
              {touched.correo && errors.correo && <FormHelperText error>{errors.correo}</FormHelperText>}
            </Grid>
            <Grid size={12}>
              <Stack sx={{ gap: 1 }}>
                <InputLabel htmlFor="clave-login">Clave</InputLabel>
                <OutlinedInput
                  id="clave-login"
                  type={mostrarClave ? 'text' : 'password'}
                  name="clave"
                  autoComplete="current-password"
                  value={values.clave}
                  onBlur={handleBlur}
                  onChange={handleChange}
                  placeholder="Su clave"
                  fullWidth
                  error={Boolean(touched.clave && errors.clave)}
                  endAdornment={
                    <InputAdornment position="end">
                      <IconButton
                        aria-label={mostrarClave ? 'Ocultar clave' : 'Mostrar clave'}
                        onClick={() => setMostrarClave(!mostrarClave)}
                        onMouseDown={(e) => e.preventDefault()}
                        edge="end"
                        color="secondary"
                      >
                        {mostrarClave ? <EyeOutlined /> : <EyeInvisibleOutlined />}
                      </IconButton>
                    </InputAdornment>
                  }
                />
              </Stack>
              {touched.clave && errors.clave && <FormHelperText error>{errors.clave}</FormHelperText>}
            </Grid>
            <Grid size={12}>
              <AnimateButton>
                <Button fullWidth size="large" type="submit" variant="contained" color="primary" disabled={isSubmitting}>
                  {isSubmitting ? 'Verificando…' : 'Iniciar sesión'}
                </Button>
              </AnimateButton>
            </Grid>
          </Grid>
        </form>
      )}
    </Formik>
  );
}
