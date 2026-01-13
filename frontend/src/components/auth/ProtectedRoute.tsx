import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { LoadingSpinner } from '../common/LoadingSpinner';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute = ({ children }: ProtectedRouteProps) => {
  const location = useLocation();
  const { token, isAuthenticated } = useAuthStore();

  // If no token, redirect to login
  if (!token) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // If we have a token but auth state is still loading, show spinner
  if (token && !isAuthenticated) {
    return <LoadingSpinner fullScreen message="Authenticating..." />;
  }

  return <>{children}</>;
};
