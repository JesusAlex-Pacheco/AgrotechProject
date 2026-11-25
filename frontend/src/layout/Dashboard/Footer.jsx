// material-ui
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

// ==============================|| DASHBOARD - FOOTER ||============================== //

export default function Footer() {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      sx={{ gap: 1.5, alignItems: 'center', justifyContent: 'space-between', p: '24px 16px 0px', mt: 'auto' }}
    >
      <Typography variant="caption">AgroTech: del campo a tu mesa, sin intermediarios · ITM 2026</Typography>
      <Typography variant="caption">
        Interfaz basada en{' '}
        <Link href="https://github.com/codedthemes/mantis-free-react-admin-template" target="_blank" underline="hover">
          Mantis
        </Link>
      </Typography>
    </Stack>
  );
}
