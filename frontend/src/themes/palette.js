// third-party
import { presetDarkPalettes, presetPalettes } from '@ant-design/colors';

// project imports
import ThemeOption from './theme';
import { extendPaletteWithChannels } from 'utils/colorUtils';

// ==============================|| GREY COLORS BUILDER ||============================== //
// Orden de cada escala (17 tonos), igual en los dos modos:
//   0-10  grises base (0 = fondo de las tarjetas, 10 = el extremo opuesto)
//   11-14 A100, A200, A400, A700
//   15    A50: fondo de la pagina
//   16    A800: borde de las tarjetas
// En modo oscuro la escala va al reves: los componentes que usan, por
// ejemplo, grey[700] para el texto lo obtienen claro sobre fondo oscuro.

function buildGrey() {
  const greyPrimary = [
    '#ffffff',
    '#fafafa',
    '#f5f5f5',
    '#f0f0f0',
    '#d9d9d9',
    '#bfbfbf',
    '#8c8c8c',
    '#595959',
    '#262626',
    '#141414',
    '#000000'
  ];
  const greyAscent = ['#fafafa', '#bfbfbf', '#434343', '#1f1f1f'];
  const greyConstant = ['#fafafb', '#e6ebf1'];

  return [...greyPrimary, ...greyAscent, ...greyConstant];
}

function buildGreyDark() {
  const greyPrimary = [
    '#1e1e1e',
    '#232323',
    '#2a2a2a',
    '#333333',
    '#454545',
    '#5e5e5e',
    '#9a9a9a',
    '#b3b3b3',
    '#e0e0e0',
    '#f0f0f0',
    '#ffffff'
  ];
  const greyAscent = ['#262626', '#434343', '#bfbfbf', '#fafafa'];
  const greyConstant = ['#121212', '#2e2e2e'];

  return [...greyPrimary, ...greyAscent, ...greyConstant];
}

// ==============================|| DEFAULT THEME - PALETTE ||============================== //

/** Arma la paleta de un modo con la misma estructura para claro y oscuro. */
function paletaDelModo(modo, colores) {
  const extendida = extendPaletteWithChannels(ThemeOption(colores, modo));
  const comunes = extendPaletteWithChannels({ common: { black: '#000', white: '#fff' } });

  return {
    mode: modo,
    ...comunes,
    ...extendida,
    text: {
      primary: extendida.grey[700],
      secondary: extendida.grey[500],
      disabled: extendida.grey[400]
    },
    action: { disabled: extendida.grey[300] },
    divider: extendida.grey[200],
    background: {
      paper: extendida.grey[0],
      default: extendida.grey.A50
    }
  };
}

export function buildPalette() {
  return {
    light: paletaDelModo('light', { ...presetPalettes, grey: buildGrey() }),
    dark: paletaDelModo('dark', { ...presetDarkPalettes, grey: buildGreyDark() })
  };
}
