import PropTypes from 'prop-types';

// material-ui
import Button from '@mui/material/Button';
import CardActionArea from '@mui/material/CardActionArea';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project imports
import MainCard from 'components/MainCard';
import FotoProducto from 'components/FotoProducto';
import { pesos } from 'utils/formato';

// assets
import ShoppingCartOutlined from '@ant-design/icons/ShoppingCartOutlined';

// ==============================|| INICIO - TARJETA DE PRODUCTO ||============================== //

export default function TarjetaProducto({ producto, nombreCategoria, puedeComprar, onVer, onAgregar }) {
  return (
    <MainCard content={false} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardActionArea onClick={() => onVer(producto)} aria-label={`Ver ${producto.nombre}`}>
        <FotoProducto producto={producto} alto={170} redondeo={0} />
        <Stack sx={{ p: 2, gap: 0.5 }}>
          {nombreCategoria && (
            <Chip label={nombreCategoria} size="small" variant="light" color="primary" sx={{ alignSelf: 'flex-start' }} />
          )}
          <Typography variant="subtitle1" noWrap>
            {producto.nombre}
          </Typography>
          <Typography variant="h5">
            {pesos(producto.precio)}{' '}
            <Typography component="span" variant="caption" sx={{ color: 'text.secondary' }}>
              / {producto.unidadMedida}
            </Typography>
          </Typography>
        </Stack>
      </CardActionArea>
      {puedeComprar && (
        <Stack sx={{ px: 2, pb: 2, mt: 'auto' }}>
          <Button variant="outlined" size="small" startIcon={<ShoppingCartOutlined />} onClick={() => onAgregar(producto)}>
            Agregar
          </Button>
        </Stack>
      )}
    </MainCard>
  );
}

TarjetaProducto.propTypes = {
  producto: PropTypes.object,
  nombreCategoria: PropTypes.string,
  puedeComprar: PropTypes.bool,
  onVer: PropTypes.func,
  onAgregar: PropTypes.func
};
