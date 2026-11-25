import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';

// material-ui
import Button from '@mui/material/Button';
import ButtonBase from '@mui/material/ButtonBase';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import ListItemIcon from '@mui/material/ListItemIcon';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

// project imports
import Avatar from 'components/@extended/Avatar';
import useSesion from 'hooks/useSesion';
import { NOMBRE_ROL } from 'utils/roles';

// assets
import LoginOutlined from '@ant-design/icons/LoginOutlined';
import LogoutOutlined from '@ant-design/icons/LogoutOutlined';

function iniciales(nombre) {
  return nombre
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join('');
}

// ==============================|| HEADER CONTENT - SESION ||============================== //

export default function Sesion() {
  const { sesion, cerrarSesion } = useSesion();
  const navigate = useNavigate();
  const [anchor, setAnchor] = useState(null);

  // Sin sesion: la aplicacion se puede usar igual; solo se ofrece iniciar sesion
  if (!sesion) {
    return (
      <Button component={RouterLink} to="/login" variant="contained" size="small" startIcon={<LoginOutlined />} sx={{ flexShrink: 0 }}>
        Iniciar sesión
      </Button>
    );
  }

  const salir = () => {
    setAnchor(null);
    cerrarSesion();
    navigate('/');
  };

  return (
    <Box sx={{ flexShrink: 0 }}>
      <ButtonBase
        onClick={(e) => setAnchor(e.currentTarget)}
        aria-label="Abrir menú de la sesión"
        aria-haspopup="true"
        sx={{ borderRadius: 1, p: 0.5, gap: 1 }}
      >
        <Avatar size="sm" color="primary" type="filled">
          {iniciales(sesion.nombre)}
        </Avatar>
        <Typography variant="subtitle1" sx={{ display: { xs: 'none', sm: 'block' } }}>
          {sesion.nombre}
        </Typography>
      </ButtonBase>

      <Menu
        anchorEl={anchor}
        open={Boolean(anchor)}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        slotProps={{ paper: { sx: { minWidth: 240, mt: 1 } } }}
      >
        <Stack sx={{ px: 2, py: 1, gap: 0.5 }}>
          <Typography variant="h6">{sesion.nombre}</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {sesion.correo}
          </Typography>
          <Chip
            label={NOMBRE_ROL[sesion.rol] ?? sesion.rol}
            size="small"
            color="primary"
            variant="light"
            sx={{ alignSelf: 'flex-start' }}
          />
        </Stack>
        <Divider sx={{ my: 1 }} />
        <MenuItem onClick={salir}>
          <ListItemIcon>
            <LogoutOutlined />
          </ListItemIcon>
          Cerrar sesión
        </MenuItem>
      </Menu>
    </Box>
  );
}
