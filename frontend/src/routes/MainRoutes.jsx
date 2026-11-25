import { lazy } from 'react';

// project imports
import Loadable from 'components/Loadable';
import DashboardLayout from 'layout/Dashboard';

// render - paginas de AgroTech
const Inicio = Loadable(lazy(() => import('pages/inicio')));
const Productos = Loadable(lazy(() => import('pages/productos')));
const Categorias = Loadable(lazy(() => import('pages/categorias')));
const Carrito = Loadable(lazy(() => import('pages/carrito')));
const Pedidos = Loadable(lazy(() => import('pages/pedidos')));
const Entregas = Loadable(lazy(() => import('pages/entregas')));
const MisResenas = Loadable(lazy(() => import('pages/resenas')));

// ==============================|| MAIN ROUTING ||============================== //
// Todas las paginas abren sin sesion; las que muestran datos privados piden
// iniciar sesion dentro de la propia pagina (y el backend los protege igual).

const MainRoutes = {
  path: '/',
  element: <DashboardLayout />,
  children: [
    { path: '/', element: <Inicio /> },
    { path: 'productos', element: <Productos /> },
    { path: 'categorias', element: <Categorias /> },
    { path: 'carrito', element: <Carrito /> },
    { path: 'pedidos', element: <Pedidos /> },
    { path: 'entregas', element: <Entregas /> },
    { path: 'resenas', element: <MisResenas /> }
  ]
};

export default MainRoutes;
