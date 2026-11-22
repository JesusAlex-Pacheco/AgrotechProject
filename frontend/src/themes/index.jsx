import PropTypes from 'prop-types';
import { useMemo } from 'react';

// material-ui
import { createTheme, StyledEngineProvider, ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

// project imports
import useConfig from 'hooks/useConfig';
import CustomShadows from './custom-shadows';
import componentsOverride from './overrides';
import { buildPalette } from './palette';
import Typography from './typography';

// ==============================|| DEFAULT THEME - MAIN ||============================== //

export default function ThemeCustomization({ children }) {
  const { state } = useConfig();

  const themeTypography = useMemo(() => Typography(state.fontFamily), [state.fontFamily]);

  const palette = useMemo(() => buildPalette(), []);

  const themeOptions = useMemo(
    () => ({
      breakpoints: {
        values: {
          xs: 0,
          sm: 768,
          md: 1024,
          lg: 1266,
          xl: 1440
        }
      },
      direction: 'ltr',
      mixins: {
        toolbar: {
          minHeight: 60,
          paddingTop: 8,
          paddingBottom: 8
        }
      },
      typography: themeTypography,
      // Modo claro y oscuro. MUI genera variables CSS para los dos y cambia
      // de uno a otro con el atributo data-color-scheme del <html>.
      colorSchemes: {
        light: {
          palette: palette.light,
          customShadows: CustomShadows(palette.light, 'light')
        },
        dark: {
          palette: palette.dark,
          customShadows: CustomShadows(palette.dark, 'dark')
        }
      },
      cssVariables: {
        cssVarPrefix: '',
        colorSchemeSelector: 'data-color-scheme'
      }
    }),
    [themeTypography, palette]
  );

  const themes = createTheme(themeOptions);
  themes.components = componentsOverride(themes);

  return (
    <StyledEngineProvider injectFirst>
      {/* defaultMode="system": arranca con el modo del dispositivo; la eleccion
          del boton sol/luna se recuerda en el navegador (theme-mode) */}
      <ThemeProvider disableTransitionOnChange theme={themes} modeStorageKey="theme-mode" defaultMode="system">
        <CssBaseline enableColorScheme />
        {children}
      </ThemeProvider>
    </StyledEngineProvider>
  );
}

ThemeCustomization.propTypes = { children: PropTypes.node };
