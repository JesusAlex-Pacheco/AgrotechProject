import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import FormHelperText from '@mui/material/FormHelperText';
import Grid from '@mui/material/Grid';
import InputLabel from '@mui/material/InputLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import Stack from '@mui/material/Stack';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';

// third-party
import * as Yup from 'yup';
import { Formik } from 'formik';

// project imports
import AnimateButton from 'components/@extended/AnimateButton';
import { registrarUsuario } from 'api/agrotech';
import useSesion from 'hooks/useSesion';

const obligatorio = (campo) => `${campo} es obligatorio`;

// Reglas iguales a las del backend (DatosUsuarioRequest y Registro*Request)
const esquema = Yup.object().shape({
  rol: Yup.string().oneOf(['CLIENTE', 'AGRICULTOR', 'TRANSPORTADOR']),
  nombre: Yup.string().trim().max(120, 'Máximo 120 caracteres').required(obligatorio('El nombre')),
  correo: Yup.string().trim().email('El correo no tiene un formato válido').max(120).required(obligatorio('El correo')),
  clave: Yup.string()
    .min(8, 'La clave debe tener al menos 8 caracteres')
    .max(72, 'Máximo 72 caracteres')
    .required('La clave es obligatoria'),
  telefono: Yup.string().max(20, 'Máximo 20 caracteres'),
  direccion: Yup.string().max(200, 'Máximo 200 caracteres'),
  nombreFinca: Yup.string().when('rol', {
    is: 'AGRICULTOR',
    then: (s) => s.trim().max(120).required(obligatorio('El nombre de la finca'))
  }),
  municipio: Yup.string().when('rol', { is: 'AGRICULTOR', then: (s) => s.trim().max(80).required(obligatorio('El municipio')) }),
  vereda: Yup.string().max(80, 'Máximo 80 caracteres'),
  placa: Yup.string().when('rol', { is: 'TRANSPORTADOR', then: (s) => s.trim().max(10).required('La placa es obligatoria') }),
  tipoVehiculo: Yup.string().max(40, 'Máximo 40 caracteres')
});

const valoresIniciales = {
  rol: 'CLIENTE',
  nombre: '',
  correo: '',
  clave: '',
  telefono: '',
  direccion: '',
  nombreFinca: '',
  municipio: '',
  vereda: '',
  placa: '',
  tipoVehiculo: ''
};

const opcional = (valor) => (valor?.trim() ? valor.trim() : null);

/** Arma el cuerpo que espera el backend para cada rol. */
function cuerpoRegistro(v) {
  const usuario = { nombre: v.nombre.trim(), correo: v.correo.trim(), clave: v.clave, telefono: opcional(v.telefono) };
  if (v.rol === 'AGRICULTOR')
    return { usuario, nombreFinca: v.nombreFinca.trim(), municipio: v.municipio.trim(), vereda: opcional(v.vereda) };
  if (v.rol === 'TRANSPORTADOR') return { usuario, placa: v.placa.trim(), tipoVehiculo: opcional(v.tipoVehiculo) };
  return { usuario, direccion: opcional(v.direccion) };
}

// ============================|| CAMPO DEL FORMULARIO ||============================ //

function Campo({ nombre, etiqueta, formik, tipo = 'text', placeholder, autoComplete, size = 12 }) {
  const { values, errors, touched, handleBlur, handleChange } = formik;
  const conError = Boolean(touched[nombre] && errors[nombre]);

  return (
    <Grid size={size}>
      <Stack sx={{ gap: 1 }}>
        <InputLabel htmlFor={`registro-${nombre}`}>{etiqueta}</InputLabel>
        <OutlinedInput
          id={`registro-${nombre}`}
          name={nombre}
          type={tipo}
          value={values[nombre]}
          onBlur={handleBlur}
          onChange={handleChange}
          placeholder={placeholder}
          autoComplete={autoComplete}
          fullWidth
          error={conError}
        />
      </Stack>
      {conError && <FormHelperText error>{errors[nombre]}</FormHelperText>}
    </Grid>
  );
}

Campo.propTypes = {
  nombre: PropTypes.string,
  etiqueta: PropTypes.string,
  formik: PropTypes.object,
  tipo: PropTypes.string,
  placeholder: PropTypes.string,
  autoComplete: PropTypes.string,
  size: PropTypes.oneOfType([PropTypes.number, PropTypes.object])
};

// ============================|| REGISTRO - FORMULARIO ||============================ //

export default function AuthRegister() {
  const { iniciarSesion } = useSesion();
  const navigate = useNavigate();

  const enviar = async (valores, { setStatus, setErrors }) => {
    setStatus(null);
    try {
      await registrarUsuario(valores.rol, cuerpoRegistro(valores));
      // Registrado: se inicia la sesion de una vez
      await iniciarSesion(valores.correo.trim(), valores.clave);
      navigate('/');
    } catch (error) {
      // Errores por campo del backend: "usuario.correo" -> "correo"
      if (error.errores) {
        setErrors(Object.fromEntries(Object.entries(error.errores).map(([campo, mensaje]) => [campo.split('.').pop(), mensaje])));
      }
      setStatus(error.message);
    }
  };

  return (
    <Formik initialValues={valoresIniciales} validationSchema={esquema} onSubmit={enviar}>
      {(formik) => (
        <form noValidate onSubmit={formik.handleSubmit}>
          <Grid container spacing={3}>
            {formik.status && (
              <Grid size={12}>
                <Alert severity="error">{formik.status}</Alert>
              </Grid>
            )}

            <Grid size={12}>
              <Stack sx={{ gap: 1 }}>
                <InputLabel id="registro-rol">Quiero registrarme como</InputLabel>
                <ToggleButtonGroup
                  aria-labelledby="registro-rol"
                  exclusive
                  fullWidth
                  color="primary"
                  size="small"
                  value={formik.values.rol}
                  onChange={(_, rol) => rol && formik.setFieldValue('rol', rol)}
                >
                  <ToggleButton value="CLIENTE">Cliente</ToggleButton>
                  <ToggleButton value="AGRICULTOR">Agricultor</ToggleButton>
                  <ToggleButton value="TRANSPORTADOR">Transportador</ToggleButton>
                </ToggleButtonGroup>
              </Stack>
            </Grid>

            <Campo formik={formik} nombre="nombre" etiqueta="Nombre completo*" placeholder="Marta Lucía Ospina" autoComplete="name" />
            <Campo
              formik={formik}
              nombre="correo"
              etiqueta="Correo*"
              tipo="email"
              placeholder="correo@ejemplo.com"
              autoComplete="email"
              size={{ xs: 12, md: 6 }}
            />
            <Campo
              formik={formik}
              nombre="telefono"
              etiqueta="Teléfono"
              placeholder="3107778899"
              autoComplete="tel"
              size={{ xs: 12, md: 6 }}
            />
            <Campo
              formik={formik}
              nombre="clave"
              etiqueta="Clave*"
              tipo="password"
              placeholder="Mínimo 8 caracteres"
              autoComplete="new-password"
            />

            {formik.values.rol === 'CLIENTE' && (
              <Campo formik={formik} nombre="direccion" etiqueta="Dirección de entrega" placeholder="Carrera 43A # 1-50, Medellín" />
            )}

            {formik.values.rol === 'AGRICULTOR' && (
              <>
                <Campo formik={formik} nombre="nombreFinca" etiqueta="Nombre de la finca*" placeholder="Finca El Roble" />
                <Campo formik={formik} nombre="municipio" etiqueta="Municipio*" placeholder="Rionegro" size={{ xs: 12, md: 6 }} />
                <Campo formik={formik} nombre="vereda" etiqueta="Vereda" placeholder="Llanogrande" size={{ xs: 12, md: 6 }} />
              </>
            )}

            {formik.values.rol === 'TRANSPORTADOR' && (
              <>
                <Campo formik={formik} nombre="placa" etiqueta="Placa*" placeholder="ABC123" size={{ xs: 12, md: 6 }} />
                <Campo formik={formik} nombre="tipoVehiculo" etiqueta="Tipo de vehículo" placeholder="Camioneta" size={{ xs: 12, md: 6 }} />
              </>
            )}

            <Grid size={12}>
              <AnimateButton>
                <Button fullWidth size="large" type="submit" variant="contained" color="primary" disabled={formik.isSubmitting}>
                  {formik.isSubmitting ? 'Creando la cuenta…' : 'Crear cuenta'}
                </Button>
              </AnimateButton>
            </Grid>
          </Grid>
        </form>
      )}
    </Formik>
  );
}
