// ==============================|| TEMA AGROTECH - COLORES ||============================== //
//
// Color de marca: #2e7d32, el verde 800 de Material Design. Se usa su escala
// completa porque genera tonos claros limpios (las paletas de Ant Design dan
// tonos grisaceos para un verde tan oscuro).
//
// En modo oscuro el verde principal sube a #66bb6a (verde 400): #2e7d32 sobre
// fondo oscuro tendria poco contraste. Por la misma razon, los botones
// verdes llevan texto oscuro en ese modo.

const VERDE_CLARO = {
  lighter: '#e8f5e9',
  100: '#c8e6c9',
  200: '#a5d6a7',
  light: '#81c784',
  400: '#66bb6a',
  main: '#2e7d32',
  dark: '#1b5e20',
  700: '#1b5e20',
  darker: '#103d14',
  900: '#0a2e0d',
  contrastText: '#ffffff'
};

const VERDE_OSCURO = {
  lighter: '#1b2e1c',
  100: '#213a23',
  200: '#2b4d2d',
  light: '#388e3c',
  400: '#4caf50',
  main: '#66bb6a',
  dark: '#81c784',
  700: '#a5d6a7',
  darker: '#c8e6c9',
  900: '#e8f5e9',
  contrastText: '#0b1f0c'
};

/**
 * @param colors paletas de @ant-design/colors (claras u oscuras) y la escala de grises
 * @param modo 'light' o 'dark'
 */
export default function Default(colors, modo = 'light') {
  const { red, gold, cyan, green, grey } = colors;
  const oscuro = modo === 'dark';

  const greyColors = {
    0: grey[0],
    50: grey[1],
    100: grey[2],
    200: grey[3],
    300: grey[4],
    400: grey[5],
    500: grey[6],
    600: grey[7],
    700: grey[8],
    800: grey[9],
    900: grey[10],
    A50: grey[15],
    A100: grey[11],
    A200: grey[12],
    A400: grey[13],
    A700: grey[14],
    A800: grey[16]
  };
  const contrastText = '#fff';

  // Las paletas oscuras de Ant Design van de oscuro (0) a claro (9): para que
  // el texto se lea sobre fondo oscuro, el tono principal es el 6 y no el 5.
  const estado = (paleta, claro, extra = {}) =>
    oscuro
      ? { lighter: paleta[1], light: paleta[4], main: paleta[6], dark: paleta[7], darker: paleta[8], contrastText, ...extra }
      : { ...claro, contrastText, ...extra };

  return {
    primary: oscuro ? VERDE_OSCURO : VERDE_CLARO,
    secondary: {
      lighter: greyColors[100],
      100: greyColors[100],
      200: greyColors[200],
      light: greyColors[300],
      400: greyColors[400],
      main: greyColors[500],
      600: greyColors[600],
      dark: greyColors[700],
      800: greyColors[800],
      darker: greyColors[900],
      A100: greyColors[0],
      A200: greyColors.A400,
      A300: greyColors.A700,
      contrastText: greyColors[0]
    },
    error: estado(red, { lighter: red[0], light: red[2], main: red[4], dark: red[7], darker: red[9] }),
    warning: estado(
      gold,
      { lighter: gold[0], light: gold[3], main: gold[5], dark: gold[7], darker: gold[9] },
      { contrastText: greyColors[100] }
    ),
    info: estado(cyan, { lighter: cyan[0], light: cyan[3], main: cyan[5], dark: cyan[7], darker: cyan[9] }),
    success: estado(green, { lighter: green[0], light: green[3], main: green[5], dark: green[7], darker: green[9] }),
    grey: greyColors
  };
}
