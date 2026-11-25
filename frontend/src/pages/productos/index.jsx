import { useDeferredValue, useMemo, useState } from 'react';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Snackbar from '@mui/material/Snackbar';
import FormControlLabel from '@mui/material/FormControlLabel';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Switch from '@mui/material/Switch';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

// third-party
import { NumericFormat } from 'react-number-format';

// project imports
import MainCard from 'components/MainCard';
import Dot from 'components/@extended/Dot';
import DialogoConfirmacion from 'components/DialogoConfirmacion';
import FormularioProducto from 'sections/productos/FormularioProducto';
import DialogoProducto from 'sections/productos/DialogoProducto';
import FotoProducto from 'components/FotoProducto';
import { desactivarProducto, refrescarProductos, useCategorias, useProductos } from 'api/agrotech';
import useSesion from 'hooks/useSesion';
import useCarrito from 'hooks/useCarrito';
import { esAdmin, esAgricultor as sesionDeAgricultor, puedeComprar } from 'utils/roles';

// assets
import EditOutlined from '@ant-design/icons/EditOutlined';
import EyeOutlined from '@ant-design/icons/EyeOutlined';
import PlusOutlined from '@ant-design/icons/PlusOutlined';
import ShoppingCartOutlined from '@ant-design/icons/ShoppingCartOutlined';
import StopOutlined from '@ant-design/icons/StopOutlined';

// ==============================|| PRODUCTOS - ESTADO ||============================== //

function EstadoProducto({ disponible }) {
  return (
    <Stack direction="row" sx={{ gap: 1, alignItems: 'center' }}>
      <Dot color={disponible ? 'success' : 'error'} />
      <Typography>{disponible ? 'Disponible' : 'No disponible'}</Typography>
    </Stack>
  );
}

// ==============================|| PRODUCTOS ||============================== //

export default function Productos() {
  const { sesion } = useSesion();
  // Desde Inicio se llega con ?categoria=<id> para ver solo esa categoria
  const [parametros] = useSearchParams();
  const [nombre, setNombre] = useState('');
  const [idCategoria, setIdCategoria] = useState(() => Number(parametros.get('categoria')) || '');
  const [soloMios, setSoloMios] = useState(false);
  const [formulario, setFormulario] = useState({ abierto: false, producto: null });
  const [porDesactivar, setPorDesactivar] = useState(null);
  const [enFicha, setEnFicha] = useState(null);
  const [agregado, setAgregado] = useState(null);
  const { agregar } = useCarrito();

  // Evita una peticion por cada tecla mientras se escribe el nombre
  const nombreBuscado = useDeferredValue(nombre);

  const { categorias } = useCategorias();
  const { productos, error, cargando, recargar } = useProductos({ idCategoria, nombre: nombreBuscado });

  const nombreCategoria = useMemo(() => new Map(categorias.map((c) => [c.id, c.nombre])), [categorias]);

  // Solo un agricultor publica productos, y solo puede modificar los suyos.
  // El administrador puede editar o retirar cualquiera (moderacion).
  const esAgricultor = sesionDeAgricultor(sesion);
  const esMio = (producto) => esAgricultor && producto.idAgricultor === sesion.idPerfil;
  const puedeModificar = (producto) => esMio(producto) || esAdmin(sesion);
  const visibles = soloMios ? productos.filter(esMio) : productos;
  // Los visitantes y los clientes pueden llenar el carrito
  const compra = puedeComprar(sesion);

  const agregarAlCarrito = (producto) => {
    agregar(producto, 1);
    setAgregado(producto.nombre);
  };

  const desactivar = async () => {
    await desactivarProducto(porDesactivar.id);
    await refrescarProductos();
  };

  return (
    <MainCard
      title="Productos"
      content={false}
      secondary={
        esAgricultor && (
          <Button variant="contained" startIcon={<PlusOutlined />} onClick={() => setFormulario({ abierto: true, producto: null })}>
            Nuevo producto
          </Button>
        )
      }
    >
      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ gap: 2, p: 2.5, alignItems: { sm: 'center' } }}>
        <TextField
          id="filtro-nombre"
          label="Buscar por nombre"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          size="small"
          sx={{ minWidth: 240 }}
        />
        <TextField
          id="filtro-categoria"
          select
          label="Categoría"
          value={idCategoria}
          onChange={(e) => setIdCategoria(e.target.value)}
          size="small"
          sx={{ minWidth: 220 }}
        >
          <MenuItem value="">Todas</MenuItem>
          {categorias.map((categoria) => (
            <MenuItem key={categoria.id} value={categoria.id}>
              {categoria.nombre}
            </MenuItem>
          ))}
        </TextField>
        {esAgricultor && (
          <FormControlLabel
            control={<Switch checked={soloMios} onChange={(e) => setSoloMios(e.target.checked)} />}
            label="Solo mis productos"
          />
        )}
      </Stack>

      {cargando && <LinearProgress />}

      {error && (
        <Alert
          severity="error"
          sx={{ mx: 2.5, mb: 2.5 }}
          action={
            <Button color="inherit" size="small" onClick={() => recargar()}>
              Reintentar
            </Button>
          }
        >
          {error.message}
        </Alert>
      )}

      {!error && (
        <TableContainer sx={{ width: '100%', overflowX: 'auto', '& td, & th': { whiteSpace: 'nowrap' } }}>
          <Table aria-label="Lista de productos">
            <TableHead>
              <TableRow>
                <TableCell>Producto</TableCell>
                <TableCell>Categoría</TableCell>
                <TableCell align="right">Precio</TableCell>
                <TableCell>Unidad</TableCell>
                <TableCell align="right">Stock</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell align="right">Acciones</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visibles.map((producto) => (
                <TableRow hover key={producto.id} sx={{ '&:last-child td': { border: 0 } }}>
                  <TableCell>
                    <Stack direction="row" sx={{ gap: 1.5, alignItems: 'center', cursor: 'pointer' }} onClick={() => setEnFicha(producto)}>
                      <FotoProducto producto={producto} alto={48} ancho={48} redondeo={1} />
                      <Stack>
                        <Typography variant="subtitle1">{producto.nombre}</Typography>
                        {producto.descripcion && (
                          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                            {producto.descripcion}
                          </Typography>
                        )}
                      </Stack>
                    </Stack>
                  </TableCell>
                  <TableCell>{nombreCategoria.get(producto.idCategoria) ?? '—'}</TableCell>
                  <TableCell align="right">
                    <NumericFormat value={producto.precio} displayType="text" thousandSeparator="." decimalSeparator="," prefix="$ " />
                  </TableCell>
                  <TableCell>{producto.unidadMedida}</TableCell>
                  <TableCell align="right">
                    <NumericFormat value={producto.stock} displayType="text" thousandSeparator="." decimalSeparator="," />
                  </TableCell>
                  <TableCell>
                    <EstadoProducto disponible={producto.disponible} />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Ver ficha y reseñas">
                      <IconButton aria-label={`Ver ficha de ${producto.nombre}`} color="secondary" onClick={() => setEnFicha(producto)}>
                        <EyeOutlined />
                      </IconButton>
                    </Tooltip>
                    {compra && (
                      <Tooltip title={producto.disponible ? 'Agregar al carrito' : 'No disponible'}>
                        <span>
                          <IconButton
                            aria-label={`Agregar ${producto.nombre} al carrito`}
                            color="primary"
                            disabled={!producto.disponible || Number(producto.stock) <= 0}
                            onClick={() => agregarAlCarrito(producto)}
                          >
                            <ShoppingCartOutlined />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )}
                    {puedeModificar(producto) && (
                      <>
                        <Tooltip title="Editar">
                          <IconButton
                            aria-label={`Editar ${producto.nombre}`}
                            color="primary"
                            onClick={() => setFormulario({ abierto: true, producto })}
                          >
                            <EditOutlined />
                          </IconButton>
                        </Tooltip>
                        {producto.disponible && (
                          <Tooltip title="Desactivar">
                            <IconButton
                              aria-label={`Desactivar ${producto.nombre}`}
                              color="error"
                              onClick={() => setPorDesactivar(producto)}
                            >
                              <StopOutlined />
                            </IconButton>
                          </Tooltip>
                        )}
                      </>
                    )}
                  </TableCell>
                </TableRow>
              ))}

              {!cargando && visibles.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    No hay productos que coincidan con la búsqueda.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <FormularioProducto
        abierto={formulario.abierto}
        producto={formulario.producto}
        idAgricultor={sesion?.idPerfil}
        categorias={categorias}
        onCerrar={() => setFormulario({ abierto: false, producto: null })}
      />

      <DialogoConfirmacion
        abierto={Boolean(porDesactivar)}
        titulo="Desactivar producto"
        mensaje={`«${porDesactivar?.nombre}» dejará de estar disponible para la venta. Puede volver a activarlo editándolo con existencias.`}
        textoConfirmar="Desactivar"
        onConfirmar={desactivar}
        onCerrar={() => setPorDesactivar(null)}
      />

      <DialogoProducto producto={enFicha} nombreCategoria={nombreCategoria.get(enFicha?.idCategoria)} onCerrar={() => setEnFicha(null)} />

      <Snackbar open={Boolean(agregado)} autoHideDuration={3000} onClose={() => setAgregado(null)}>
        <Alert
          severity="success"
          variant="filled"
          onClose={() => setAgregado(null)}
          action={
            <Button component={RouterLink} to="/carrito" color="inherit" size="small">
              Ver carrito
            </Button>
          }
        >
          «{agregado}» se agregó al carrito
        </Alert>
      </Snackbar>
    </MainCard>
  );
}
