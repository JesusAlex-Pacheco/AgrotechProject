// ==============================|| FORMATO - PESOS, FECHAS Y ESTADOS ||============================== //

const formatoPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const formatoCantidad = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 });
const formatoFecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
const formatoDia = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });

export const pesos = (valor) => formatoPesos.format(Number(valor ?? 0));
export const cantidad = (valor) => formatoCantidad.format(Number(valor ?? 0));
export const fechaHora = (texto) => (texto ? formatoFecha.format(new Date(texto)) : '—');
/** Fechas sin hora del backend ('2026-12-01'): se leen como fecha local, no UTC. */
export const dia = (texto) => (texto ? formatoDia.format(new Date(`${texto}T00:00:00`)) : '—');

/** Hoy en formato AAAA-MM-DD (hora local), para los campos de fecha. */
export const hoyISO = () => {
  const hoy = new Date();
  return new Date(hoy.getTime() - hoy.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};

/** Promedio de las calificaciones con un decimal, o null si no hay resenas. */
export function promedioCalificaciones(resenas) {
  if (!resenas?.length) return null;
  const suma = resenas.reduce((total, r) => total + r.calificacion, 0);
  return Math.round((suma / resenas.length) * 10) / 10;
}

export const ESTADO_PEDIDO = {
  CREADO: { texto: 'Creado', color: 'secondary' },
  PAGADO: { texto: 'Pagado', color: 'info' },
  EN_RUTA: { texto: 'En ruta', color: 'warning' },
  ENTREGADO: { texto: 'Entregado', color: 'success' },
  CANCELADO: { texto: 'Cancelado', color: 'error' }
};

export const ESTADO_ENTREGA = {
  PROGRAMADA: { texto: 'Programada', color: 'secondary' },
  EN_RUTA: { texto: 'En ruta', color: 'warning' },
  ENTREGADA: { texto: 'Entregada', color: 'success' },
  FALLIDA: { texto: 'Fallida', color: 'error' }
};

export const METODO_PAGO = {
  PSE: 'PSE',
  TARJETA: 'Tarjeta',
  CONTRA_ENTREGA: 'Contra entrega'
};

export const ESTADO_PAGO = {
  PENDIENTE: 'pendiente',
  APROBADO: 'aprobado',
  RECHAZADO: 'rechazado'
};
