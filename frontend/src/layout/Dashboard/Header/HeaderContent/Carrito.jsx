import { Link as RouterLink } from 'react-router-dom';

// material-ui
import Badge from '@mui/material/Badge';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';

// project imports
import useCarrito from 'hooks/useCarrito';
import useSesion from 'hooks/useSesion';
import { puedeComprar } from 'utils/roles';

// assets
import ShoppingCartOutlined from '@ant-design/icons/ShoppingCartOutlined';

// ==============================|| HEADER CONTENT - CARRITO ||============================== //

export default function Carrito() {
  const { sesion } = useSesion();
  const { cantidadProductos } = useCarrito();

  // Agricultores, transportadores y el administrador no compran
  if (!puedeComprar(sesion)) return null;

  return (
    <Tooltip title="Carrito">
      <IconButton
        component={RouterLink}
        to="/carrito"
        aria-label={`Carrito, ${cantidadProductos} productos`}
        color="secondary"
        sx={{ mr: 1.5 }}
      >
        <Badge badgeContent={cantidadProductos} color="primary">
          <ShoppingCartOutlined style={{ fontSize: '1.25rem' }} />
        </Badge>
      </IconButton>
    </Tooltip>
  );
}
