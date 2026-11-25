// ==============================|| OVERRIDES - PAPER ||============================== //

// En modo oscuro MUI aclara los dialogos y menus segun su elevacion con una
// capa gris; aqui se quita para que tengan el mismo tono que las tarjetas.
export default function Paper() {
  return {
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: 'none' }
      }
    }
  };
}
