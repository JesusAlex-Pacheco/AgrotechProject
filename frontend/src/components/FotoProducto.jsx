import PropTypes from 'prop-types';
import { useState } from 'react';

// material-ui
import Box from '@mui/material/Box';

// project imports
import { estiloTono } from 'utils/tonos';

// ==============================|| FOTO DEL PRODUCTO ||============================== //

/**
 * Una foto valida: subida a AgroTech, un enlace http(s) o la vista previa
 * local (blob:) del formulario. Cualquier otro texto se ignora.
 */
export const esFotoValida = (url) => typeof url === 'string' && /^(https?:\/\/|\/api\/fotos\/|blob:)/.test(url);

/**
 * Muestra la foto del producto. Si no tiene, o no carga, muestra un recuadro
 * con la inicial del producto y una hoja, para que el catalogo no se vea vacio.
 */
export default function FotoProducto({ producto, alto = 160, ancho = '100%', redondeo = 1.5, sx }) {
  const [fallo, setFallo] = useState(null);
  const url = producto?.fotoUrl;
  const mostrarFoto = esFotoValida(url) && fallo !== url;

  return (
    <Box
      sx={[
        {
          width: ancho,
          height: alto,
          flexShrink: 0,
          borderRadius: redondeo,
          overflow: 'hidden',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        },
        // Siempre el mismo tono para el mismo producto, con su version oscura
        estiloTono(producto?.id ?? 0),
        sx ?? {}
      ]}
    >
      {mostrarFoto ? (
        <Box
          component="img"
          src={url}
          alt={producto.nombre}
          loading="lazy"
          onError={() => setFallo(url)}
          sx={{ width: '100%', height: '100%', objectFit: 'cover', display: 'block' }}
        />
      ) : (
        <Box
          component="svg"
          viewBox="0 0 64 64"
          role="img"
          aria-label={`${producto?.nombre ?? 'Producto'} sin foto`}
          sx={{ width: '55%', height: '55%', maxWidth: 96 }}
        >
          <path
            d="M44 14c-14 0-26 9-26 24 0 3 1 6 2 8 10-12 18-17 28-20-9 5-16 12-22 23 3 1 5 1 8 1 13 0 21-12 21-27 0-3 0-6-1-9h-10z"
            fill="currentColor"
            opacity="0.25"
          />
          <text x="30" y="40" textAnchor="middle" fontFamily="'Public Sans', sans-serif" fontSize="24" fontWeight="700" fill="currentColor">
            {(producto?.nombre ?? '?').charAt(0).toUpperCase()}
          </text>
        </Box>
      )}
    </Box>
  );
}

FotoProducto.propTypes = {
  producto: PropTypes.object,
  alto: PropTypes.oneOfType([PropTypes.number, PropTypes.string, PropTypes.object]),
  ancho: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  redondeo: PropTypes.number,
  sx: PropTypes.object
};
