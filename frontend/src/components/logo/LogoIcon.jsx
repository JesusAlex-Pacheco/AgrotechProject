// material-ui
import Box from '@mui/material/Box';

// Verde de la marca. Es el mismo en modo claro y oscuro: el circulo verde con la
// hoja blanca se distingue bien sobre ambos fondos.
const VERDE = '#2e7d32';

// ==============================|| LOGO - SOLO ICONO ||============================== //

/**
 * Se muestra arriba a la izquierda cuando el menu lateral esta cerrado.
 * Es la "o" con hoja del logo de AgroTech, dibujada en SVG para que se vea
 * nitida a 40 px (la ilustracion completa pierde todo el detalle a ese tamano).
 */
export default function LogoIcon() {
  return (
    <Box component="svg" viewBox="0 0 64 64" role="img" aria-label="AgroTech" sx={{ width: 40, height: 40, display: 'block' }}>
      <circle cx="32" cy="32" r="31" fill={VERDE} />
      {/* Hoja */}
      <path d="M19 45C18 29 30 18 48 16C49 33 38 45 22 46Z" fill="#fff" />
      {/* Nervio central, del color del circulo */}
      <path d="M21 44Q30 33 42 23" stroke={VERDE} strokeWidth="2.6" strokeLinecap="round" fill="none" />
      {/* Tallo que sale de la hoja */}
      <path d="M21 44L15 50" stroke="#fff" strokeWidth="3" strokeLinecap="round" />
    </Box>
  );
}
