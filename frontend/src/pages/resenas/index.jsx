import { useMemo } from 'react';
import { Link as RouterLink } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import LinearProgress from '@mui/material/LinearProgress';
import Rating from '@mui/material/Rating';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// project imports
import MainCard from 'components/MainCard';
import FotoProducto from 'components/FotoProducto';
import { useProductos, useResenasCliente } from 'api/agrotech';
import useSesion from 'hooks/useSesion';
import { esCliente } from 'utils/roles';
import { dia, promedioCalificaciones } from 'utils/formato';

// ==============================|| MIS RESEÑAS (CLIENTE) ||============================== //

export default function MisResenas() {
  const { sesion } = useSesion();
  const cliente = esCliente(sesion);

  const { resenas, error, cargando, recargar } = useResenasCliente(cliente ? sesion.idPerfil : null);
  // El catalogo trae todos los productos (tambien los retirados): da nombre y foto
  const { productos } = useProductos();
  const porId = useMemo(() => new Map(productos.map((p) => [p.id, p])), [productos]);
  const promedio = promedioCalificaciones(resenas);

  if (!sesion) {
    return (
      <MainCard title="Mis reseñas">
        <Alert
          severity="info"
          action={
            <Button component={RouterLink} to="/login" state={{ desde: '/resenas' }} color="inherit" size="small">
              Iniciar sesión
            </Button>
          }
        >
          Inicie sesión como cliente para ver las reseñas que ha escrito.
        </Alert>
      </MainCard>
    );
  }

  if (!cliente) {
    return (
      <MainCard title="Mis reseñas">
        <Alert severity="info">Las reseñas las escriben los clientes sobre los productos que recibieron.</Alert>
      </MainCard>
    );
  }

  return (
    <MainCard
      title="Mis reseñas"
      content={false}
      secondary={
        promedio !== null && (
          <Stack direction="row" sx={{ alignItems: 'center', gap: 1 }}>
            <Rating value={promedio} precision={0.1} size="small" readOnly />
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {promedio} en promedio · {resenas.length} {resenas.length === 1 ? 'reseña' : 'reseñas'}
            </Typography>
          </Stack>
        )
      }
    >
      {cargando && <LinearProgress />}

      {error && (
        <Alert
          severity="error"
          sx={{ m: 2.5 }}
          action={
            <Button color="inherit" size="small" onClick={() => recargar()}>
              Reintentar
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}

      {!error && !cargando && resenas.length === 0 && (
        <Stack sx={{ alignItems: 'center', gap: 1.5, py: 5, px: 2.5, textAlign: 'center' }}>
          <Typography variant="h5">Aún no ha escrito reseñas</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary', maxWidth: 440 }}>
            Cuando reciba un pedido, ábralo en Mis compras y califique cada producto. Su opinión ayuda a otros clientes y a los
            agricultores.
          </Typography>
          <Button component={RouterLink} to="/pedidos" variant="contained">
            Ir a Mis compras
          </Button>
        </Stack>
      )}

      {!error &&
        resenas.map((resena, indice) => {
          const producto = porId.get(resena.idProducto);
          return (
            <Stack key={resena.id}>
              {indice > 0 && <Divider />}
              <Stack direction="row" sx={{ gap: 2, p: 2.5, alignItems: 'flex-start' }}>
                <FotoProducto producto={producto ?? { id: resena.idProducto, nombre: '?' }} alto={64} ancho={64} />
                <Stack sx={{ gap: 0.5, minWidth: 0, flex: 1 }}>
                  <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 0.5 }}>
                    <Typography variant="subtitle1">{producto?.nombre ?? `Producto #${resena.idProducto}`}</Typography>
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      {dia(resena.fecha)}
                    </Typography>
                  </Stack>
                  <Rating value={resena.calificacion} size="small" readOnly />
                  <Typography
                    variant="body2"
                    sx={{ color: resena.comentario ? 'text.primary' : 'text.secondary', overflowWrap: 'anywhere' }}
                  >
                    {resena.comentario || 'Sin comentario.'}
                  </Typography>
                </Stack>
              </Stack>
            </Stack>
          );
        })}
    </MainCard>
  );
}
