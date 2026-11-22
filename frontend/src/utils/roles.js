// ==============================|| ROLES - QUIEN PUEDE QUE ||============================== //
// Reflejan las reglas del backend (SeguridadConfig y ControlAcceso). Solo
// sirven para mostrar u ocultar botones: la verdadera validacion la hace el
// backend, que responde 401 o 403 si alguien llama la API sin permiso.

export const NOMBRE_ROL = {
  AGRICULTOR: 'Agricultor',
  CLIENTE: 'Cliente',
  TRANSPORTADOR: 'Transportador',
  ADMIN: 'Administrador'
};

/** Sin sesion se puede armar el carrito; al comprar se pide iniciar sesion como cliente. */
export const puedeComprar = (sesion) => !sesion || sesion.rol === 'CLIENTE';

export const esCliente = (sesion) => sesion?.rol === 'CLIENTE' && Boolean(sesion.idPerfil);
export const esAgricultor = (sesion) => sesion?.rol === 'AGRICULTOR' && Boolean(sesion.idPerfil);
export const esTransportador = (sesion) => sesion?.rol === 'TRANSPORTADOR' && Boolean(sesion.idPerfil);
export const esAdmin = (sesion) => sesion?.rol === 'ADMIN';

/** Confirman pagos, programan entregas y despachan: el agricultor (en sus ventas) y el administrador. */
export const gestionaVentas = (sesion) => esAgricultor(sesion) || esAdmin(sesion);
