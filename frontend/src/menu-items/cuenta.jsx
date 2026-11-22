// assets
import { CarOutlined, FileTextOutlined, ShoppingCartOutlined, ShopOutlined, StarOutlined } from '@ant-design/icons';

// ==============================|| MENU ITEMS - MI CUENTA ||============================== //
// `roles` indica quien ve cada opcion; null significa "tambien sin sesion".
// Navigation oculta las opciones (y el grupo) que no correspondan al usuario.

const cuenta = {
  id: 'group-cuenta',
  title: 'Mi cuenta',
  type: 'group',
  children: [
    {
      id: 'carrito',
      title: 'Carrito',
      type: 'item',
      url: '/carrito',
      icon: ShoppingCartOutlined,
      roles: [null, 'CLIENTE']
    },
    {
      id: 'mis-compras',
      title: 'Mis compras',
      type: 'item',
      url: '/pedidos',
      icon: FileTextOutlined,
      roles: ['CLIENTE']
    },
    {
      id: 'mis-resenas',
      title: 'Mis reseñas',
      type: 'item',
      url: '/resenas',
      icon: StarOutlined,
      roles: ['CLIENTE']
    },
    {
      id: 'mis-ventas',
      title: 'Mis ventas',
      type: 'item',
      url: '/pedidos',
      icon: ShopOutlined,
      roles: ['AGRICULTOR']
    },
    {
      id: 'pedidos',
      title: 'Pedidos',
      type: 'item',
      url: '/pedidos',
      icon: FileTextOutlined,
      roles: ['ADMIN']
    },
    {
      id: 'entregas',
      title: 'Mis entregas',
      type: 'item',
      url: '/entregas',
      icon: CarOutlined,
      roles: ['TRANSPORTADOR']
    }
  ]
};

export default cuenta;
