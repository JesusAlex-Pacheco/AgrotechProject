import PropTypes from 'prop-types';
import { useState } from 'react';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Grid from '@mui/material/Grid';
import InputAdornment from '@mui/material/InputAdornment';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';

// third-party
import * as Yup from 'yup';
import { Formik } from 'formik';

// project imports
import SelectorFoto from './SelectorFoto';
import { esFotoValida } from 'components/FotoProducto';
import { actualizarProducto, crearProducto, quitarFotoProducto, refrescarProductos, subirFotoProducto } from 'api/agrotech';

export const UNIDADES = ['kg', 'lb', 'unidad', 'manojo', 'docena', 'canasta', 'litro', 'frasco', 'paquete'];

// Reglas iguales a las de ProductoRequest en el backend
const esquema = Yup.object().shape({
  nombre: Yup.string().trim().max(120, 'Máximo 120 caracteres').required('El nombre es obligatorio'),
  idCategoria: Yup.number().typeError('La categoría es obligatoria').required('La categoría es obligatoria'),
  descripcion: Yup.string().max(500, 'Máximo 500 caracteres'),
  precio: Yup.number().typeError('Escriba un número').moreThan(0, 'El precio debe ser mayor que cero').required('El precio es obligatorio'),
  unidadMedida: Yup.string().required('La unidad es obligatoria'),
  stock: Yup.number().typeError('Escriba un número').min(0, 'El stock no puede ser negativo').required('El stock es obligatorio')
});

// archivo: foto elegida y aun no subida; quitarFoto: quitar la que ya tiene
const vacio = { nombre: '', idCategoria: '', descripcion: '', precio: '', unidadMedida: 'kg', stock: '', archivo: null, quitarFoto: false };

function valoresDe(producto) {
  if (!producto) return vacio;
  return {
    ...vacio,
    nombre: producto.nombre,
    idCategoria: producto.idCategoria,
    descripcion: producto.descripcion ?? '',
    precio: producto.precio,
    unidadMedida: producto.unidadMedida,
    stock: producto.stock
  };
}

// ==============================|| PRODUCTOS - FORMULARIO ||============================== //

/**
 * Crea un producto (producto = null) o edita uno existente. El producto
 * siempre queda a nombre del agricultor que lo publico (idAgricultor).
 * La foto se sube despues de guardar los datos, con su propio endpoint.
 */
export default function FormularioProducto({ abierto, producto, idAgricultor, categorias, onCerrar }) {
  // Si el producto se creo pero la foto fallo, el siguiente intento edita en vez de crear otro
  const [creado, setCreado] = useState(null);
  const actual = producto ?? creado;
  const editando = Boolean(actual);

  const cerrar = () => {
    setCreado(null);
    onCerrar();
  };

  const enviar = async (v, { setStatus, setErrors }) => {
    setStatus(null);
    const datos = {
      idAgricultor: editando ? actual.idAgricultor : idAgricultor,
      idCategoria: Number(v.idCategoria),
      nombre: v.nombre.trim(),
      descripcion: v.descripcion.trim() || null,
      precio: Number(v.precio),
      unidadMedida: v.unidadMedida,
      stock: Number(v.stock),
      // La foto no se cambia aqui: se conserva la que tenga (si es valida)
      fotoUrl: esFotoValida(actual?.fotoUrl) ? actual.fotoUrl : null
    };

    let guardado;
    try {
      guardado = editando ? await actualizarProducto(actual.id, datos) : await crearProducto(datos);
    } catch (error) {
      if (error.errores) setErrors(error.errores);
      setStatus(error.message);
      return;
    }

    try {
      if (v.archivo) await subirFotoProducto(guardado.id, v.archivo);
      else if (v.quitarFoto && esFotoValida(actual?.fotoUrl)) await quitarFotoProducto(guardado.id);
      await refrescarProductos();
      cerrar();
    } catch (error) {
      await refrescarProductos();
      setCreado(guardado);
      setStatus(`El producto se guardó, pero la foto no: ${error.message}`);
    }
  };

  return (
    <Dialog open={abierto} onClose={cerrar} fullWidth maxWidth="sm">
      <Formik initialValues={valoresDe(producto)} validationSchema={esquema} onSubmit={enviar} enableReinitialize>
        {({ errors, touched, values, handleBlur, handleChange, handleSubmit, isSubmitting, status, setFieldValue }) => {
          // Propiedades comunes de cada campo de texto
          const campo = (nombre) => ({
            id: `producto-${nombre}`,
            name: nombre,
            value: values[nombre],
            onChange: handleChange,
            onBlur: handleBlur,
            error: Boolean(touched[nombre] && errors[nombre]),
            helperText: touched[nombre] && errors[nombre],
            fullWidth: true
          });

          return (
            <form noValidate onSubmit={handleSubmit}>
              <DialogTitle>{editando ? 'Editar producto' : 'Nuevo producto'}</DialogTitle>
              <DialogContent dividers>
                <Grid container spacing={2.5}>
                  {status && (
                    <Grid size={12}>
                      <Alert severity="error">{status}</Alert>
                    </Grid>
                  )}
                  <Grid size={12}>
                    <TextField {...campo('nombre')} label="Nombre*" autoFocus />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField {...campo('idCategoria')} select label="Categoría*">
                      {categorias.map((c) => (
                        <MenuItem key={c.id} value={c.id}>
                          {c.nombre}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField {...campo('unidadMedida')} select label="Unidad de medida*">
                      {UNIDADES.map((u) => (
                        <MenuItem key={u} value={u}>
                          {u}
                        </MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      {...campo('precio')}
                      type="number"
                      label="Precio*"
                      slotProps={{ input: { startAdornment: <InputAdornment position="start">$</InputAdornment> }, htmlInput: { min: 0 } }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField {...campo('stock')} type="number" label="Existencias*" slotProps={{ htmlInput: { min: 0 } }} />
                  </Grid>
                  <Grid size={12}>
                    <TextField {...campo('descripcion')} label="Descripción" multiline minRows={2} />
                  </Grid>
                  <Grid size={12}>
                    <SelectorFoto
                      nombre={values.nombre}
                      fotoActual={actual?.fotoUrl}
                      archivo={values.archivo}
                      quitar={values.quitarFoto}
                      onElegir={(archivo) => {
                        setFieldValue('archivo', archivo);
                        setFieldValue('quitarFoto', false);
                      }}
                      onQuitar={() => {
                        setFieldValue('archivo', null);
                        setFieldValue('quitarFoto', true);
                      }}
                    />
                  </Grid>
                </Grid>
              </DialogContent>
              <DialogActions sx={{ px: 3, py: 2 }}>
                <Button onClick={cerrar} color="secondary" disabled={isSubmitting}>
                  Cancelar
                </Button>
                <Button type="submit" variant="contained" disabled={isSubmitting}>
                  {isSubmitting ? 'Guardando…' : editando ? 'Guardar cambios' : 'Publicar producto'}
                </Button>
              </DialogActions>
            </form>
          );
        }}
      </Formik>
    </Dialog>
  );
}

FormularioProducto.propTypes = {
  abierto: PropTypes.bool,
  producto: PropTypes.object,
  idAgricultor: PropTypes.number,
  categorias: PropTypes.array,
  onCerrar: PropTypes.func
};
