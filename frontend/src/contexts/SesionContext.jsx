import PropTypes from 'prop-types';
import { createContext, useCallback, useEffect, useMemo, useState } from 'react';

// project imports
import { configurarSesionApi, iniciarSesion as iniciarSesionApi, olvidarDatosPrivados } from 'api/agrotech';

// La sesion (con su token) se guarda en el navegador para no perderla al recargar la pagina
const CLAVE_ALMACENAMIENTO = 'agrotech-sesion';

const estaVencida = (sesion) => !sesion?.token || (sesion.expira && new Date(sesion.expira) <= new Date());

function leerSesionGuardada() {
  try {
    const texto = localStorage.getItem(CLAVE_ALMACENAMIENTO);
    const sesion = texto ? JSON.parse(texto) : null;
    // Una sesion vieja sin token, o ya vencida, no sirve: se descarta
    return sesion && !estaVencida(sesion) ? sesion : null;
  } catch {
    return null;
  }
}

function guardarSesion(sesion) {
  try {
    if (sesion) localStorage.setItem(CLAVE_ALMACENAMIENTO, JSON.stringify(sesion));
    else localStorage.removeItem(CLAVE_ALMACENAMIENTO);
  } catch {
    // Si el navegador no permite guardar, la sesion dura hasta recargar la pagina
  }
}

// ==============================|| SESION CONTEXT ||============================== //

export const SesionContext = createContext(undefined);

// ==============================|| SESION PROVIDER ||============================== //

export function SesionProvider({ children }) {
  const [sesion, setSesion] = useState(() => {
    const guardada = leerSesionGuardada();
    // El token se entrega a la API antes del primer render, para que las
    // primeras consultas ya salgan autenticadas
    configurarSesionApi({ token: guardada?.token });
    return guardada;
  });
  // Aviso que se muestra cuando el backend rechaza un token vencido
  const [aviso, setAviso] = useState(null);

  const terminar = useCallback((mensaje) => {
    configurarSesionApi({ token: null });
    guardarSesion(null);
    setSesion(null);
    setAviso(mensaje);
    olvidarDatosPrivados();
  }, []);

  useEffect(() => {
    configurarSesionApi({ alVencer: () => terminar('Su sesión expiró. Inicie sesión de nuevo.') });
  }, [terminar]);

  const iniciarSesion = useCallback(async (correo, clave) => {
    const datos = await iniciarSesionApi(correo, clave);
    configurarSesionApi({ token: datos.token });
    guardarSesion(datos);
    setSesion(datos);
    setAviso(null);
    return datos;
  }, []);

  const cerrarSesion = useCallback(() => terminar(null), [terminar]);
  const descartarAviso = useCallback(() => setAviso(null), []);

  const valor = useMemo(
    () => ({ sesion, iniciarSesion, cerrarSesion, aviso, descartarAviso }),
    [sesion, iniciarSesion, cerrarSesion, aviso, descartarAviso]
  );

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}

SesionProvider.propTypes = { children: PropTypes.node };
