import PropTypes from 'prop-types';

// project imports
import DrawerHeaderStyled from './DrawerHeaderStyled';
import Logo from 'components/logo';

// ==============================|| DRAWER HEADER ||============================== //

export default function DrawerHeader({ open }) {
  return (
    <DrawerHeaderStyled
      open={open}
      sx={{
        minHeight: '60px',
        width: 'initial',
        paddingTop: '8px',
        paddingBottom: '8px',
        paddingLeft: open ? '16px' : 0
      }}
    >
      {/* Abierto: logo completo a lo ancho del menu (260 px). Cerrado: solo el icono. */}
      <Logo isIcon={!open} alto={80} anchoMax={228} />
    </DrawerHeaderStyled>
  );
}

DrawerHeader.propTypes = { open: PropTypes.bool };
