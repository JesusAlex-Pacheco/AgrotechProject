// assets
import { HomeOutlined } from '@ant-design/icons';

// icons
const icons = {
  HomeOutlined
};

// ==============================|| MENU ITEMS - INICIO ||============================== //

const dashboard = {
  id: 'group-inicio',
  title: 'Navegación',
  type: 'group',
  children: [
    {
      id: 'inicio',
      title: 'Inicio',
      type: 'item',
      url: '/',
      icon: icons.HomeOutlined,
      breadcrumbs: false
    }
  ]
};

export default dashboard;
