import { RouterProvider } from 'react-router-dom';

// project imports
import router from 'routes';
import ThemeCustomization from 'themes';
import { SesionProvider } from 'contexts/SesionContext';
import { CarritoProvider } from 'contexts/CarritoContext';

// ==============================|| APP - THEME, SESION, CARRITO, ROUTER ||============================== //

export default function App() {
  return (
    <ThemeCustomization>
      <SesionProvider>
        <CarritoProvider>
          <RouterProvider router={router} />
        </CarritoProvider>
      </SesionProvider>
    </ThemeCustomization>
  );
}
