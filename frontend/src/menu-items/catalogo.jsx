// assets
import { AppstoreOutlined, ShoppingOutlined } from '@ant-design/icons';

// icons
const icons = {
  ShoppingOutlined,
  AppstoreOutlined
};

// ==============================|| MENU ITEMS - CATALOGO ||============================== //

const catalogo = {
  id: 'group-catalogo',
  title: 'Catálogo',
  type: 'group',
  children: [
    {
      id: 'productos',
      title: 'Productos',
      type: 'item',
      url: '/productos',
      icon: icons.ShoppingOutlined
    },
    {
      id: 'categorias',
      title: 'Categorías',
      type: 'item',
      url: '/categorias',
      icon: icons.AppstoreOutlined
    }
  ]
};

export default catalogo;
