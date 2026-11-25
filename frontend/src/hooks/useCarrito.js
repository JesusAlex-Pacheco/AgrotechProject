import { use } from 'react';

// project imports
import { CarritoContext } from 'contexts/CarritoContext';

// ==============================|| HOOK - CARRITO ||============================== //

export default function useCarrito() {
  const contexto = use(CarritoContext);
  if (!contexto) throw new Error('useCarrito debe usarse dentro de CarritoProvider');
  return contexto;
}
