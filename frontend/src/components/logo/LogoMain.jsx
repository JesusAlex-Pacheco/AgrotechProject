import PropTypes from 'prop-types';

// project imports
import ImagenSegunModo from './ImagenSegunModo';

// assets: versiones livianas de logo-claro.png y logo-oscuro.png (npm run imagenes)
// (recortadas, sin fondo, en WebP de 640 px de ancho)
import logoClaro from 'assets/images/logo-claro-app.webp';
import logoOscuro from 'assets/images/logo-oscuro-app.webp';

// ==============================|| LOGO COMPLETO ||============================== //

// El logo oscuro es mas ancho que el claro: con anchoMax se encoge sin deformarse
// para no salirse de su espacio (por ejemplo, el menu lateral).
export default function LogoMain({ alto = 64, anchoMax = 'none' }) {
  return (
    <ImagenSegunModo
      claro={logoClaro}
      oscuro={logoOscuro}
      alt="AgroTech"
      sx={{ height: alto, width: 'auto', maxWidth: anchoMax, objectFit: 'contain' }}
    />
  );
}

LogoMain.propTypes = {
  alto: PropTypes.oneOfType([PropTypes.number, PropTypes.object]),
  anchoMax: PropTypes.oneOfType([PropTypes.number, PropTypes.string])
};
