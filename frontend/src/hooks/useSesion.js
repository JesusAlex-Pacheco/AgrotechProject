import { use } from 'react';
import { SesionContext } from 'contexts/SesionContext';

// ==============================|| SESION - HOOK ||============================== //

/** Usuario que inicio sesion (o null) y las funciones para iniciar y cerrar sesion. */
export default function useSesion() {
  const context = use(SesionContext);

  if (!context) throw new Error('useSesion se debe usar dentro de SesionProvider');

  return context;
}
