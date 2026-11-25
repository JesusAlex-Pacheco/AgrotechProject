// material-ui
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

// project import
import NavGroup from './NavGroup';
import menuItem from 'menu-items';
import useSesion from 'hooks/useSesion';

// ==============================|| DRAWER CONTENT - NAVIGATION ||============================== //

/** Una opcion sin `roles` la ve todo el mundo; con `roles`, solo esos (null = sin sesion). */
const esVisible = (item, rol) => !item.roles || item.roles.includes(rol);

export default function Navigation() {
  const { sesion } = useSesion();
  const rol = sesion?.rol ?? null;

  const navGroups = menuItem.items
    .map((grupo) => ({ ...grupo, children: grupo.children?.filter((item) => esVisible(item, rol)) }))
    .filter((grupo) => grupo.children?.length)
    .map((item) => {
      switch (item.type) {
        case 'group':
          return <NavGroup key={item.id} item={item} />;
        default:
          return (
            <Typography key={item.id} variant="h6" sx={{ color: 'error.main', textAlign: 'center' }}>
              Fix - Navigation Group
            </Typography>
          );
      }
    });

  return <Box sx={{ pt: 2 }}>{navGroups}</Box>;
}
