// ==============================|| TONOS DECORATIVOS ||============================== //
// Fondos suaves para los productos sin foto y las categorias, cada uno con su
// version para modo oscuro (fondo apagado y texto claro).

const TONOS = [
  { fondo: '#e8f5e9', texto: '#2e7d32', fondoOscuro: '#1b2e1c', textoOscuro: '#81c784' },
  { fondo: '#fff8e1', texto: '#b26a00', fondoOscuro: '#2e2614', textoOscuro: '#ffcc80' },
  { fondo: '#fbe9e7', texto: '#bf360c', fondoOscuro: '#2f1d18', textoOscuro: '#ffab91' },
  { fondo: '#f1f8e9', texto: '#558b2f', fondoOscuro: '#1f2b16', textoOscuro: '#aed581' },
  { fondo: '#fff3e0', texto: '#e65100', fondoOscuro: '#302214', textoOscuro: '#ffb74d' },
  { fondo: '#e0f2f1', texto: '#00695c', fondoOscuro: '#14292a', textoOscuro: '#80cbc4' }
];

/** Estilo `sx` con fondo y color de texto del tono n, segun el modo claro u oscuro. */
export const estiloTono = (n) => (theme) => {
  const tono = TONOS[Math.abs(n) % TONOS.length];
  return {
    bgcolor: tono.fondo,
    color: tono.texto,
    ...theme.applyStyles('dark', { bgcolor: tono.fondoOscuro, color: tono.textoOscuro })
  };
};
