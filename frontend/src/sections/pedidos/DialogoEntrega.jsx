import PropTypes from 'prop-types';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';

// third-party
import * as Yup from 'yup';
import { Formik } from 'formik';

// project imports
import { programarEntrega, refrescarPedidos, useTransportadores } from 'api/agrotech';
import { hoyISO } from 'utils/formato';

// Mismas reglas que EntregaRequest en el backend
const esquema = Yup.object().shape({
  idTransportador: Yup.number().typeError('Elija un transportador').required('Elija un transportador'),
  fechaProgramada: Yup.string()
    .required('La fecha es obligatoria')
    .test('no-pasada', 'La fecha no puede estar en el pasado', (valor) => !valor || valor >= hoyISO()),
  direccion: Yup.string().max(200, 'Máximo 200 caracteres')
});

// ==============================|| PEDIDOS - PROGRAMAR ENTREGA ||============================== //

export default function DialogoEntrega({ pedido, onCerrar }) {
  const { transportadores, error: errorTransportadores } = useTransportadores(Boolean(pedido));

  const enviar = async (valores, { setStatus }) => {
    setStatus(null);
    try {
      await programarEntrega(pedido.id, {
        idTransportador: Number(valores.idTransportador),
        fechaProgramada: valores.fechaProgramada,
        direccion: valores.direccion.trim() || null
      });
      await refrescarPedidos();
      onCerrar();
    } catch (error) {
      setStatus(error.message);
    }
  };

  return (
    <Dialog open={Boolean(pedido)} onClose={onCerrar} fullWidth maxWidth="sm">
      <Formik
        initialValues={{ idTransportador: '', fechaProgramada: hoyISO(), direccion: '' }}
        validationSchema={esquema}
        onSubmit={enviar}
      >
        {({ values, errors, touched, handleChange, handleBlur, handleSubmit, isSubmitting, status }) => (
          <form noValidate onSubmit={handleSubmit}>
            <DialogTitle>Programar la entrega del pedido #{pedido?.id}</DialogTitle>
            <DialogContent dividers>
              <Stack sx={{ gap: 2.5 }}>
                {(status || errorTransportadores) && <Alert severity="error">{status ?? errorTransportadores.message}</Alert>}
                {transportadores.length === 0 && !errorTransportadores && (
                  <Alert severity="warning">Aún no hay transportadores registrados.</Alert>
                )}

                <TextField
                  select
                  name="idTransportador"
                  label="Transportador"
                  value={values.idTransportador}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={Boolean(touched.idTransportador && errors.idTransportador)}
                  helperText={touched.idTransportador && errors.idTransportador}
                >
                  {transportadores.map((t) => (
                    <MenuItem key={t.id} value={t.id}>
                      {t.usuario.nombre} · {t.placa}
                      {t.tipoVehiculo ? ` (${t.tipoVehiculo})` : ''}
                    </MenuItem>
                  ))}
                </TextField>

                <TextField
                  type="date"
                  name="fechaProgramada"
                  label="Fecha de entrega"
                  value={values.fechaProgramada}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={Boolean(touched.fechaProgramada && errors.fechaProgramada)}
                  helperText={touched.fechaProgramada && errors.fechaProgramada}
                  slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: hoyISO() } }}
                />

                <TextField
                  name="direccion"
                  label="Dirección de entrega"
                  value={values.direccion}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  error={Boolean(touched.direccion && errors.direccion)}
                  helperText={(touched.direccion && errors.direccion) || 'Si la deja vacía, se usa la dirección registrada del cliente'}
                />
              </Stack>
            </DialogContent>
            <DialogActions>
              <Button onClick={onCerrar} color="secondary">
                Cancelar
              </Button>
              <Button type="submit" variant="contained" disabled={isSubmitting}>
                {isSubmitting ? 'Programando…' : 'Programar'}
              </Button>
            </DialogActions>
          </form>
        )}
      </Formik>
    </Dialog>
  );
}

DialogoEntrega.propTypes = { pedido: PropTypes.object, onCerrar: PropTypes.func };
