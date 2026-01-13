import { Box } from '@mui/material';
import { Outlet } from 'react-router-dom';
import { Sidebar, DRAWER_WIDTH } from './Sidebar';
import { Header } from './Header';

export const AppLayout = () => {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar />
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          ml: `${DRAWER_WIDTH}px`,
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Header />
        <Box
          className="page-transition"
          sx={{
            flexGrow: 1,
            p: 3,
            mt: 8, // Account for header height
          }}
        >
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
};
