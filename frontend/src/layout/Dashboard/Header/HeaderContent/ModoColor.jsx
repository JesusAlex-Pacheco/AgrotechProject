// material-ui
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import { useColorScheme } from '@mui/material/styles';

// assets
import MoonOutlined from '@ant-design/icons/MoonOutlined';
import SunOutlined from '@ant-design/icons/SunOutlined';

// ==============================|| HEADER CONTENT - MODO CLARO / OSCURO ||============================== //

/**
 * Cambia entre modo claro y oscuro. Al principio se usa el modo del
 * dispositivo; cuando la persona elige uno, MUI lo recuerda en el navegador.
 */
export default function ModoColor() {
  const { mode, systemMode, setMode } = useColorScheme();
  // Antes de montar, mode es undefined; "system" se resuelve con systemMode
  const actual = (mode === 'system' ? systemMode : mode) ?? 'light';
  const siguiente = actual === 'dark' ? 'light' : 'dark';
  const texto = siguiente === 'dark' ? 'Cambiar a modo oscuro' : 'Cambiar a modo claro';

  return (
    <Tooltip title={texto}>
      <IconButton aria-label={texto} color="secondary" onClick={() => setMode(siguiente)} sx={{ mr: 0.5, flexShrink: 0 }}>
        {actual === 'dark' ? <SunOutlined style={{ fontSize: '1.15rem' }} /> : <MoonOutlined style={{ fontSize: '1.15rem' }} />}
      </IconButton>
    </Tooltip>
  );
}
