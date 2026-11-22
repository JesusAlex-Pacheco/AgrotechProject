import PropTypes from 'prop-types';
import { useEffect, useMemo, useState } from 'react';

// material-ui
import Button from '@mui/material/Button';
import FormHelperText from '@mui/material/FormHelperText';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project imports
import FotoProducto, { esFotoValida } from 'components/FotoProducto';

// assets
import CameraOutlined from '@ant-design/icons/CameraOutlined';
import DeleteOutlined from '@ant-design/icons/DeleteOutlined';

// Mismos limites que el backend (FotoProductoServiceImpl y multipart.max-file-size)
const TIPOS = ['image/jpeg', 'image/png', 'image/webp'];
const MAXIMO_MB = 5;

// ==============================|| PRODUCTOS - SELECTOR DE FOTO ||============================== //

/**
 * Elige la foto del producto desde el computador o el celular (en el celular
 * ofrece tambien la camara) y muestra como quedara antes de guardar.
 */
export default function SelectorFoto({ nombre, fotoActual, archivo, quitar, onElegir, onQuitar }) {
  const [error, setError] = useState(null);

  // Vista previa del archivo elegido, sin subirlo todavia
  const previa = useMemo(() => (archivo ? URL.createObjectURL(archivo) : null), [archivo]);
  useEffect(() => () => previa && URL.revokeObjectURL(previa), [previa]);

  const fotoUrl = previa ?? (quitar || !esFotoValida(fotoActual) ? null : fotoActual);

  const elegir = (evento) => {
    const elegido = evento.target.files?.[0];
    evento.target.value = ''; // permite volver a elegir el mismo archivo
    if (!elegido) return;
    if (!TIPOS.includes(elegido.type)) return setError('La foto debe ser JPG, PNG o WEBP.');
    if (elegido.size > MAXIMO_MB * 1024 * 1024) return setError(`La foto no puede pesar más de ${MAXIMO_MB} MB.`);
    setError(null);
    onElegir(elegido);
  };

  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, alignItems: { sm: 'center' } }}>
      <FotoProducto producto={{ id: 0, nombre: nombre || 'Producto', fotoUrl }} alto={110} ancho={140} />
      <Stack sx={{ gap: 1 }}>
        <Typography variant="subtitle2">Foto del producto</Typography>
        <Stack direction="row" sx={{ gap: 1, flexWrap: 'wrap' }}>
          <Button component="label" variant="outlined" size="small" startIcon={<CameraOutlined />}>
            {fotoUrl ? 'Cambiar foto' : 'Elegir foto'}
            <input hidden type="file" accept={TIPOS.join(',')} onChange={elegir} />
          </Button>
          {fotoUrl && (
            <Button size="small" color="error" startIcon={<DeleteOutlined />} onClick={onQuitar}>
              Quitar
            </Button>
          )}
        </Stack>
        {error ? (
          <FormHelperText error>{error}</FormHelperText>
        ) : (
          <FormHelperText>JPG, PNG o WEBP de hasta {MAXIMO_MB} MB. Se guarda al guardar el producto.</FormHelperText>
        )}
      </Stack>
    </Stack>
  );
}

SelectorFoto.propTypes = {
  nombre: PropTypes.string,
  fotoActual: PropTypes.string,
  archivo: PropTypes.object,
  quitar: PropTypes.bool,
  onElegir: PropTypes.func,
  onQuitar: PropTypes.func
};
