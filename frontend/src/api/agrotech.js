import useSWR, { mutate } from 'swr';

// ==============================|| API AGROTECH ||============================== //

// En desarrollo, Vite reenvia /api al backend Spring Boot (ver vite.config.mjs).
const API_BASE = '/api';

// La direccion del backend viene de VITE_BACKEND_URL en el archivo .env
// (import.meta.env no existe al correr las pruebas con Node, de ahi el ?.)
const DIRECCION_BACKEND = import.meta.env?.VITE_BACKEND_URL || 'http://localhost:8080';
const MENSAJE_SIN_CONEXION = `No hay conexión con el backend. Verifique que Spring Boot esté corriendo en ${DIRECCION_BACKEND}.`;

/** Error con el mensaje que devuelve el backend (ManejadorGlobalErrores). */
export class ApiError extends Error {
  constructor(mensaje, estado, errores) {
    super(mensaje);
    this.name = 'ApiError';
    this.estado = estado;
    this.errores = errores ?? null;
  }
}

// ------------------------------ Token de sesion ------------------------------

// SesionContext entrega aqui el token al iniciar sesion y lo borra al cerrarla.
// Se envia en cada peticion como "Authorization: Bearer <token>".
let tokenActual = null;
let alVencerSesion = () => {};

export function configurarSesionApi(cambios) {
  // Solo se toca lo que viene: registrar alVencer no debe borrar el token
  if ('token' in cambios) tokenActual = cambios.token ?? null;
  if (cambios.alVencer) alVencerSesion = cambios.alVencer;
}

/** Hace una peticion al backend y devuelve el JSON, o lanza ApiError. */
export async function solicitar(ruta, opciones = {}, conToken = true) {
  const token = conToken ? tokenActual : null;
  let respuesta;
  try {
    respuesta = await fetch(`${API_BASE}${ruta}`, {
      ...opciones,
      headers: {
        // Con FormData (fotos) el navegador pone el Content-Type multipart con su separador
        ...(opciones.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...opciones.headers
      }
    });
  } catch {
    throw new ApiError(MENSAJE_SIN_CONEXION, 0);
  }

  // Token vencido o invalido (por ejemplo, el backend se reinicio): se cierra
  // la sesion y, si era una consulta, se repite sin token (el catalogo es publico).
  if (respuesta.status === 401 && token) {
    alVencerSesion();
    if (!opciones.method || opciones.method === 'GET') return solicitar(ruta, opciones, false);
  }

  if (respuesta.status === 204) return null;

  const cuerpo = await respuesta.json().catch(() => null);

  if (!respuesta.ok) {
    if (!cuerpo && respuesta.status >= 500) throw new ApiError(MENSAJE_SIN_CONEXION, respuesta.status);
    throw new ApiError(cuerpo?.mensaje ?? `El servidor respondió con el error ${respuesta.status}.`, respuesta.status, cuerpo?.errores);
  }

  return cuerpo;
}

const fetcher = (ruta) => solicitar(ruta);

/** Para recursos opcionales: un 404 significa "no hay" y se devuelve null. */
const fetcherOpcional = (ruta) =>
  solicitar(ruta).catch((error) => {
    if (error.estado === 404) return null;
    throw error;
  });

const opcionesSWR = { revalidateOnFocus: false };

const enviar = (metodo, ruta, datos) => solicitar(ruta, { method: metodo, body: datos === undefined ? undefined : JSON.stringify(datos) });

const conFiltroEstado = (ruta, estado) => {
  if (!estado) return ruta;
  return `${ruta}${ruta.includes('?') ? '&' : '?'}estado=${estado}`;
};

// ------------------------------ Sesion y registro ------------------------------

/** Verifica correo y clave. Devuelve { idUsuario, nombre, correo, rol, idPerfil, token, expira }. */
export const iniciarSesion = (correo, clave) => solicitar('/sesiones', { method: 'POST', body: JSON.stringify({ correo, clave }) }, false);

// Ruta de registro segun el rol elegido en el formulario
const RUTA_REGISTRO = { CLIENTE: '/clientes', AGRICULTOR: '/agricultores', TRANSPORTADOR: '/transportadores' };

/** Registra un usuario con su perfil. `datos` ya tiene la forma que espera el backend. */
export const registrarUsuario = (rol, datos) => enviar('POST', RUTA_REGISTRO[rol], datos);

// ------------------------------ Escritura ------------------------------

export const crearProducto = (datos) => enviar('POST', '/productos', datos);
export const actualizarProducto = (id, datos) => enviar('PUT', `/productos/${id}`, datos);
/** El backend no borra el producto: lo marca como no disponible. */
export const desactivarProducto = (id) => enviar('DELETE', `/productos/${id}`);

/** Sube la foto (JPG, PNG o WEBP, hasta 5 MB) y devuelve el producto con su nuevo fotoUrl. */
export function subirFotoProducto(id, archivo) {
  const formulario = new FormData();
  formulario.append('archivo', archivo);
  return solicitar(`/productos/${id}/foto`, { method: 'PUT', body: formulario });
}
export const quitarFotoProducto = (id) => enviar('DELETE', `/productos/${id}/foto`);

export const crearCategoria = (nombre) => enviar('POST', '/categorias', { nombre });
export const actualizarCategoria = (id, nombre) => enviar('PUT', `/categorias/${id}`, { nombre });
export const eliminarCategoria = (id) => enviar('DELETE', `/categorias/${id}`);

/** { idCliente, metodoPago, items: [{ idProducto, cantidad }] } */
export const registrarPedido = (datos) => enviar('POST', '/pedidos', datos);
export const cambiarEstadoPedido = (id, estado) => enviar('PATCH', `/pedidos/${id}/estado`, { estado });
/** { idTransportador, direccion?, fechaProgramada: 'AAAA-MM-DD' } */
export const programarEntrega = (idPedido, datos) => enviar('POST', `/pedidos/${idPedido}/entrega`, datos);
/** { idCliente, calificacion, comentario? } */
export const registrarResena = (idProducto, datos) => enviar('POST', `/productos/${idProducto}/resenas`, datos);

// ------------------------------ Recargar datos ------------------------------

// Despues de guardar, se vuelven a pedir todas las listas afectadas (con
// cualquier filtro), para que todas las pantallas muestren lo nuevo.
const empiezaPor =
  (...prefijos) =>
  (clave) =>
    typeof clave === 'string' && prefijos.some((prefijo) => clave.startsWith(prefijo));

export const refrescarProductos = () => mutate(empiezaPor('/productos'));
export const refrescarCategorias = () => mutate(empiezaPor('/categorias'));
export const refrescarPedidos = () => mutate(empiezaPor('/pedidos', '/agricultores/', '/transportadores/'));

/** Al cerrar sesion se borran los datos privados que quedaron en memoria (pedidos, agenda...). */
export const olvidarDatosPrivados = () =>
  mutate(empiezaPor('/pedidos', '/agricultores/', '/transportadores', '/clientes/'), undefined, { revalidate: false });

// ------------------------------ Productos ------------------------------

/** Lista de productos, con filtros opcionales por categoria y nombre. */
export function useProductos({ idCategoria, nombre } = {}) {
  const parametros = new URLSearchParams();
  if (idCategoria) parametros.set('idCategoria', idCategoria);
  if (nombre?.trim()) parametros.set('nombre', nombre.trim());
  const consulta = parametros.toString();

  const { data, error, isLoading, mutate } = useSWR(`/productos${consulta ? `?${consulta}` : ''}`, fetcher, opcionesSWR);

  return { productos: data ?? [], error, cargando: isLoading, recargar: mutate };
}

/** Resenas de un producto con su promedio. `idProducto` nulo no consulta nada. */
export function useResenas(idProducto) {
  const { data, error, isLoading } = useSWR(idProducto ? `/productos/${idProducto}/resenas` : null, fetcher, opcionesSWR);
  return { resenas: data, error, cargando: isLoading };
}

/** Resenas que escribio el cliente, de la mas reciente a la mas antigua. `idCliente` nulo no consulta nada. */
export function useResenasCliente(idCliente) {
  const { data, error, isLoading, mutate } = useSWR(idCliente ? `/clientes/${idCliente}/resenas` : null, fetcher, opcionesSWR);
  return { resenas: data ?? [], error, cargando: isLoading, recargar: mutate };
}

// ------------------------------ Categorias ------------------------------

/** Lista de todas las categorias. */
export function useCategorias() {
  const { data, error, isLoading, mutate } = useSWR('/categorias', fetcher, opcionesSWR);

  return { categorias: data ?? [], error, cargando: isLoading, recargar: mutate };
}

// ------------------------------ Pedidos ------------------------------

/** Que lista de pedidos ve cada rol. */
function rutaPedidos(sesion) {
  switch (sesion?.rol) {
    case 'CLIENTE':
      return `/pedidos?idCliente=${sesion.idPerfil}`;
    case 'AGRICULTOR':
      return `/agricultores/${sesion.idPerfil}/pedidos`;
    case 'ADMIN':
      return '/pedidos';
    default:
      return null;
  }
}

/** Pedidos del usuario segun su rol: sus compras, sus ventas o todos (ADMIN). */
export function usePedidos(sesion, estado) {
  const ruta = rutaPedidos(sesion);
  const { data, error, isLoading, mutate } = useSWR(ruta ? conFiltroEstado(ruta, estado) : null, fetcher, opcionesSWR);
  return { pedidos: data ?? [], error, cargando: isLoading, recargar: mutate };
}

/** Entrega programada del pedido, o null si aun no tiene. */
export function useEntrega(idPedido) {
  const { data, error, isLoading } = useSWR(idPedido ? `/pedidos/${idPedido}/entrega` : null, fetcherOpcional, opcionesSWR);
  return { entrega: data ?? null, error, cargando: isLoading };
}

/** Transportadores para asignar una entrega (solo AGRICULTOR o ADMIN). */
export function useTransportadores(activo = true) {
  const { data, error, isLoading } = useSWR(activo ? '/transportadores' : null, fetcher, opcionesSWR);
  return { transportadores: data ?? [], error, cargando: isLoading };
}

/** Agenda de entregas del transportador. */
export function useAgenda(idTransportador, estado) {
  const ruta = idTransportador ? conFiltroEstado(`/transportadores/${idTransportador}/entregas`, estado) : null;
  const { data, error, isLoading, mutate } = useSWR(ruta, fetcher, opcionesSWR);
  return { entregas: data ?? [], error, cargando: isLoading, recargar: mutate };
}
