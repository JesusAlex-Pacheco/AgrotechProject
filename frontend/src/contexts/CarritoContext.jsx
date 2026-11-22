import PropTypes from 'prop-types';
import { createContext, useCallback, useEffect, useMemo, useState } from 'react';

// El carrito vive en el navegador: se puede armar sin sesion y se pide
// iniciar sesion como cliente solo al confirmar la compra.
const CLAVE_ALMACENAMIENTO = 'agrotech-carrito';

function leerCarrito() {
  try {
    const texto = localStorage.getItem(CLAVE_ALMACENAMIENTO);
    const items = texto ? JSON.parse(texto) : [];
    return Array.isArray(items) ? items : [];
  } catch {
    return [];
  }
}

// ==============================|| CARRITO CONTEXT ||============================== //

export const CarritoContext = createContext(undefined);

export function CarritoProvider({ children }) {
  const [items, setItems] = useState(leerCarrito);

  useEffect(() => {
    try {
      localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(items));
    } catch {
      // Sin almacenamiento, el carrito dura hasta recargar la pagina
    }
  }, [items]);

  /** Agrega el producto o suma la cantidad si ya estaba, sin pasar de las existencias. */
  const agregar = useCallback((producto, cantidad) => {
    setItems((actuales) => {
      const existente = actuales.find((item) => item.idProducto === producto.id);
      const total = Math.min((existente?.cantidad ?? 0) + cantidad, Number(producto.stock));
      const item = {
        idProducto: producto.id,
        nombre: producto.nombre,
        precio: Number(producto.precio),
        unidadMedida: producto.unidadMedida,
        stock: Number(producto.stock),
        cantidad: total
      };
      return existente ? actuales.map((i) => (i.idProducto === producto.id ? item : i)) : [...actuales, item];
    });
  }, []);

  const cambiarCantidad = useCallback((idProducto, cantidad) => {
    setItems((actuales) => actuales.map((i) => (i.idProducto === idProducto ? { ...i, cantidad } : i)));
  }, []);

  const quitar = useCallback((idProducto) => {
    setItems((actuales) => actuales.filter((i) => i.idProducto !== idProducto));
  }, []);

  const vaciar = useCallback(() => setItems([]), []);

  const valor = useMemo(() => {
    const total = items.reduce((suma, i) => suma + i.precio * (Number(i.cantidad) || 0), 0);
    return { items, total, cantidadProductos: items.length, agregar, cambiarCantidad, quitar, vaciar };
  }, [items, agregar, cambiarCantidad, quitar, vaciar]);

  return <CarritoContext.Provider value={valor}>{children}</CarritoContext.Provider>;
}

CarritoProvider.propTypes = { children: PropTypes.node };
