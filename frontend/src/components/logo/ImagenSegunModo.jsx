import PropTypes from 'prop-types';

// material-ui
import Box from '@mui/material/Box';

// ==============================|| IMAGEN SEGUN MODO CLARO / OSCURO ||============================== //

/**
 * Pinta las dos versiones de la imagen y deja visible solo la del modo activo.
 * Se hace con CSS (no con JavaScript) para que no parpadee al cargar la pagina.
 */
export default function ImagenSegunModo({ claro, oscuro, alt, sx }) {
  const base = { display: 'block', maxWidth: '100%', ...sx };
  return (
    <>
      <Box component="img" src={claro} alt={alt} sx={(theme) => ({ ...base, ...theme.applyStyles('dark', { display: 'none' }) })} />
      <Box
        component="img"
        src={oscuro}
        alt={alt}
        sx={(theme) => ({ ...base, display: 'none', ...theme.applyStyles('dark', { display: 'block' }) })}
      />
    </>
  );
}

ImagenSegunModo.propTypes = { claro: PropTypes.string, oscuro: PropTypes.string, alt: PropTypes.string, sx: PropTypes.object };
