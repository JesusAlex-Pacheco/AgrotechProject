import PropTypes from 'prop-types';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';

// third-party
import * as Yup from 'yup';
import { Formik } from 'formik';

// project imports
import { actualizarCategoria, crearCategoria, refrescarCategorias } from 'api/agrotech';

const esquema = Yup.object().shape({
  nombre: Yup.string().trim().max(60, 'Máximo 60 caracteres').required('El nombre es obligatorio')
});

// ==============================|| CATEGORIAS - FORMULARIO ||============================== //

/** Crea una categoria (categoria = null) o cambia el nombre de una existente. */
export default function FormularioCategoria({ abierto, categoria, onCerrar }) {
  const editando = Boolean(categoria);

  const enviar = async ({ nombre }, { setStatus }) => {
    setStatus(null);
    try {
      if (editando) await actualizarCategoria(categoria.id, nombre.trim());
      else await crearCategoria(nombre.trim());
      await refrescarCategorias();
      onCerrar();
    } catch (error) {
      setStatus(error.message);
    }
  };

  return (
    <Dialog open={abierto} onClose={onCerrar} fullWidth maxWidth="xs">
      <Formik initialValues={{ nombre: categoria?.nombre ?? '' }} validationSchema={esquema} onSubmit={enviar} enableReinitialize>
        {({ errors, touched, values, handleBlur, handleChange, handleSubmit, isSubmitting, status }) => (
          <form noValidate onSubmit={handleSubmit}>
            <DialogTitle>{editando ? 'Cambiar nombre de la categoría' : 'Nueva categoría'}</DialogTitle>
            <DialogContent dividers>
              <Stack sx={{ gap: 2 }}>
                {status && <Alert severity="error">{status}</Alert>}
                <TextField
                  id="categoria-nombre"
                  name="nombre"
                  label="Nombre*"
                  value={values.nombre}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={Boolean(touched.nombre && errors.nombre)}
                  helperText={touched.nombre && errors.nombre}
                  autoFocus
                  fullWidth
                />
              </Stack>
            </DialogContent>
            <DialogActions sx={{ px: 3, py: 2 }}>
              <Button onClick={onCerrar} color="secondary" disabled={isSubmitting}>
                Cancelar
              </Button>
              <Button type="submit" variant="contained" disabled={isSubmitting}>
                {isSubmitting ? 'Guardando…' : 'Guardar'}
              </Button>
            </DialogActions>
          </form>
        )}
      </Formik>
    </Dialog>
  );
}

FormularioCategoria.propTypes = { abierto: PropTypes.bool, categoria: PropTypes.object, onCerrar: PropTypes.func };
