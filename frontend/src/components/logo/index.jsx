import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';

// material-ui
import ButtonBase from '@mui/material/ButtonBase';

// project imports
import Logo from './LogoMain';
import LogoIcon from './LogoIcon';
import { APP_DEFAULT_PATH } from 'config';

// ==============================|| MAIN LOGO ||============================== //

export default function LogoSection({ isIcon, alto, anchoMax, sx, to }) {
  return (
    <ButtonBase disableRipple component={Link} to={to || APP_DEFAULT_PATH} sx={sx} aria-label="Ir al inicio">
      {isIcon ? <LogoIcon /> : <Logo alto={alto} anchoMax={anchoMax} />}
    </ButtonBase>
  );
}

LogoSection.propTypes = { isIcon: PropTypes.bool, alto: PropTypes.any, anchoMax: PropTypes.any, sx: PropTypes.any, to: PropTypes.any };
