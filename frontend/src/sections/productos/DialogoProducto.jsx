import PropTypes from 'prop-types';
import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import Rating from '@mui/material/Rating';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project imports
import FotoProducto from 'components/FotoProducto';
import { useResenas } from 'api/agrotech';
import useCarrito from 'hooks/useCarrito';
import useSesion from 'hooks/useSesion';
import { puedeComprar } from 'utils/roles';
import { cantidad, dia, pesos } from 'utils/formato';

// assets
import CloseOutlined from '@ant-design/icons/CloseOutlined';
import ShoppingCartOutlined from '@ant-design/icons/ShoppingCartOutlined';

// ==============================|| PRODUCTOS - FICHA DEL PRODUCTO ||============================== //

/** Ficha del producto: foto, datos, compra y reseñas públicas. */
export default function DialogoProducto({ producto, nombreCategoria, onCerrar }) {
  const { sesion } = useSesion();
  const { agregar } = useCarrito();
  const { resenas, error, cargando } = useResenas(producto?.id);
  const [agregado, setAgregado] = useState(false);

  const disponible = producto?.disponible && Number(producto.stock) > 0;

  const cerrar = () => {
    setAgregado(false);
    onCerrar();
  };

  return (
    <Dialog open={Boolean(producto)} onClose={cerrar} fullWidth maxWidth="md" aria-labelledby="titulo-producto">
      {producto && (
        <DialogContent sx={{ p: 0 }}>
          <IconButton
            aria-label="Cerrar"
            onClick={cerrar}
            sx={{ position: 'absolute', top: 8, right: 8, zIndex: 1, bgcolor: 'background.paper' }}
          >
            <CloseOutlined />
          </IconButton>
          <Grid container>
            <Grid size={{ xs: 12, md: 6 }}>
              <FotoProducto producto={producto} alto={{ xs: 240, md: '100%' }} redondeo={0} sx={{ minHeight: { md: 380 } }} />
            </Grid>
            <Grid size={{ xs: 12, md: 6 }}>
              <Stack sx={{ p: 3, gap: 1.5 }}>
                {nombreCategoria && (
                  <Chip label={nombreCategoria} size="small" color="primary" variant="light" sx={{ alignSelf: 'flex-start' }} />
                )}
                <Typography id="titulo-producto" variant="h3">
                  {producto.nombre}
                </Typography>
                {resenas?.total > 0 && (
                  <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
                    <Rating value={Number(resenas.promedio)} precision={0.1} size="small" readOnly />
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      {resenas.promedio} · {resenas.total} {resenas.total === 1 ? 'reseña' : 'reseñas'}
                    </Typography>
                  </Stack>
                )}
                <Typography variant="h4" sx={{ color: 'primary.main' }}>
                  {pesos(producto.precio)}{' '}
                  <Typography component="span" variant="body1" sx={{ color: 'text.secondary' }}>
                    / {producto.unidadMedida}
                  </Typography>
                </Typography>
                {producto.descripcion && <Typography>{producto.descripcion}</Typography>}
                <Typography variant="body2" sx={{ color: disponible ? 'success.dark' : 'error.main' }}>
                  {disponible ? `Disponible: ${cantidad(producto.stock)} ${producto.unidadMedida}` : 'Agotado por ahora'}
                </Typography>

                {puedeComprar(sesion) &&
                  (agregado ? (
                    <Alert
                      severity="success"
                      action={
                        <Button component={RouterLink} to="/carrito" color="inherit" size="small">
                          Ver carrito
                        </Button>
                      }
                    >
                      Se agregó al carrito
                    </Alert>
                  ) : (
                    <Button
                      variant="contained"
                      size="large"
                      startIcon={<ShoppingCartOutlined />}
                      disabled={!disponible}
                      onClick={() => {
                        agregar(producto, 1);
                        setAgregado(true);
                      }}
                    >
                      Agregar al carrito
                    </Button>
                  ))}
              </Stack>
            </Grid>
          </Grid>

          <Divider />
          {cargando && <LinearProgress />}
          <Stack sx={{ p: 3, gap: 2 }}>
            <Typography variant="h5">Reseñas</Typography>
            {error && <Alert severity="error">{error.message}</Alert>}
            {resenas?.total === 0 && (
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                Aún no tiene reseñas. Solo los clientes que lo recibieron pueden reseñarlo, desde «Mis compras».
              </Typography>
            )}
            {resenas?.resenas.map((resena) => (
              <Stack key={resena.id} sx={{ gap: 0.5 }}>
                <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
                  <Rating value={resena.calificacion} size="small" readOnly />
                  <Typography variant="subtitle2">{resena.nombreCliente}</Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary', ml: 'auto' }}>
                    {dia(resena.fecha)}
                  </Typography>
                </Stack>
                {resena.comentario && <Typography variant="body2">{resena.comentario}</Typography>}
              </Stack>
            ))}
          </Stack>
        </DialogContent>
      )}
    </Dialog>
  );
}

DialogoProducto.propTypes = { producto: PropTypes.object, nombreCategoria: PropTypes.string, onCerrar: PropTypes.func };
