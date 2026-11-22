import { useMemo, useState } from 'react';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Tooltip from '@mui/material/Tooltip';

// project imports
import MainCard from 'components/MainCard';
import DialogoConfirmacion from 'components/DialogoConfirmacion';
import FormularioCategoria from 'sections/categorias/FormularioCategoria';
import { eliminarCategoria, refrescarCategorias, useCategorias, useProductos } from 'api/agrotech';
import useSesion from 'hooks/useSesion';

// assets
import DeleteOutlined from '@ant-design/icons/DeleteOutlined';
import EditOutlined from '@ant-design/icons/EditOutlined';
import PlusOutlined from '@ant-design/icons/PlusOutlined';

// Roles que pueden organizar el catalogo
const ROLES_GESTION = ['AGRICULTOR', 'ADMIN'];

// ==============================|| CATEGORIAS ||============================== //

export default function Categorias() {
  const { sesion } = useSesion();
  const { categorias, error, cargando, recargar } = useCategorias();
  const { productos } = useProductos();
  const [formulario, setFormulario] = useState({ abierto: false, categoria: null });
  const [porEliminar, setPorEliminar] = useState(null);

  const puedeGestionar = ROLES_GESTION.includes(sesion?.rol);

  // Cuantos productos tiene cada categoria
  const productosPorCategoria = useMemo(() => {
    const conteo = new Map();
    productos.forEach((p) => conteo.set(p.idCategoria, (conteo.get(p.idCategoria) ?? 0) + 1));
    return conteo;
  }, [productos]);

  const eliminar = async () => {
    await eliminarCategoria(porEliminar.id);
    await refrescarCategorias();
  };

  return (
    <MainCard
      title="Categorías"
      content={false}
      secondary={
        puedeGestionar && (
          <Button variant="contained" startIcon={<PlusOutlined />} onClick={() => setFormulario({ abierto: true, categoria: null })}>
            Nueva categoría
          </Button>
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

      {!error && (
        <TableContainer>
          <Table aria-label="Lista de categorías">
            <TableHead>
              <TableRow>
                <TableCell>Código</TableCell>
                <TableCell>Nombre</TableCell>
                <TableCell align="right">Productos</TableCell>
                {puedeGestionar && <TableCell align="right">Acciones</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {categorias.map((categoria) => {
                const cantidad = productosPorCategoria.get(categoria.id) ?? 0;
                return (
                  <TableRow hover key={categoria.id} sx={{ '&:last-child td': { border: 0 } }}>
                    <TableCell>{categoria.id}</TableCell>
                    <TableCell>{categoria.nombre}</TableCell>
                    <TableCell align="right">{cantidad}</TableCell>
                    {puedeGestionar && (
                      <TableCell align="right">
                        <Tooltip title="Cambiar nombre">
                          <IconButton
                            aria-label={`Cambiar nombre de ${categoria.nombre}`}
                            color="primary"
                            onClick={() => setFormulario({ abierto: true, categoria })}
                          >
                            <EditOutlined />
                          </IconButton>
                        </Tooltip>
                        {/* Una categoria con productos no se puede eliminar (regla del backend) */}
                        <Tooltip title={cantidad > 0 ? 'Tiene productos: no se puede eliminar' : 'Eliminar'}>
                          <span>
                            <IconButton
                              aria-label={`Eliminar ${categoria.nombre}`}
                              color="error"
                              disabled={cantidad > 0}
                              onClick={() => setPorEliminar(categoria)}
                            >
                              <DeleteOutlined />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </TableCell>
                    )}
                  </TableRow>
                );
              })}

              {!cargando && categorias.length === 0 && (
                <TableRow>
                  <TableCell colSpan={puedeGestionar ? 4 : 3} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                    Todavía no hay categorías registradas.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <FormularioCategoria
        abierto={formulario.abierto}
        categoria={formulario.categoria}
        onCerrar={() => setFormulario({ abierto: false, categoria: null })}
      />

      <DialogoConfirmacion
        abierto={Boolean(porEliminar)}
        titulo="Eliminar categoría"
        mensaje={`Se eliminará la categoría «${porEliminar?.nombre}». Esta acción no se puede deshacer.`}
        textoConfirmar="Eliminar"
        onConfirmar={eliminar}
        onCerrar={() => setPorEliminar(null)}
      />
    </MainCard>
  );
}
