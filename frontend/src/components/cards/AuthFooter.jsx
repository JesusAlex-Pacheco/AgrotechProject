// material-ui
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';

// project imports
import ContainerWrapper from 'components/ContainerWrapper';

// ==============================|| FOOTER - AUTHENTICATION ||============================== //

export default function AuthFooter() {
  return (
    <ContainerWrapper>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        sx={{ gap: 2, justifyContent: { xs: 'center', sm: 'space-between' }, textAlign: { xs: 'center', sm: 'inherit' }, py: 2 }}
      >
        <Typography variant="subtitle2" sx={{ color: 'secondary.main' }}>
          AgroTech: del campo a tu mesa, sin intermediarios
        </Typography>
        <Typography variant="subtitle2" sx={{ color: 'secondary.main' }}>
          Interfaz basada en{' '}
          <Link href="https://github.com/codedthemes/mantis-free-react-admin-template" target="_blank" underline="hover">
            Mantis
          </Link>
        </Typography>
      </Stack>
    </ContainerWrapper>
  );
}
