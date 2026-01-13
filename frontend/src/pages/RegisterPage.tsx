import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, TextField, Typography, Alert, Link } from '@mui/material';
import { GlassCard } from '../components/common/GlassCard';
import { useAuth } from '../hooks/useAuth';

export const RegisterPage = () => {
  const { register, isRegistering, registerError } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [companyName, setCompanyName] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await register({
        email,
        password,
        full_name: fullName || undefined,
        company_name: companyName || undefined,
      });
    } catch (error) {
      // Error is handled by the hook
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 3,
      }}
    >
      <GlassCard sx={{ maxWidth: 400, width: '100%' }}>
        <Box sx={{ textAlign: 'center', mb: 4 }}>
          <Typography
            variant="h4"
            sx={{
              fontWeight: 700,
              background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              mb: 1,
            }}
          >
            Create Account
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            Start discovering tenders today
          </Typography>
        </Box>

        {registerError && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {(registerError as Error).message || 'Registration failed'}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            helperText="Min 8 chars, 1 uppercase, 1 lowercase, 1 digit"
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            label="Full Name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            label="Company Name"
            value={companyName}
            onChange={(e) => setCompanyName(e.target.value)}
            sx={{ mb: 3 }}
          />
          <Button
            type="submit"
            variant="contained"
            fullWidth
            size="large"
            disabled={isRegistering}
          >
            {isRegistering ? 'Creating account...' : 'Create Account'}
          </Button>
        </form>

        <Box sx={{ mt: 3, textAlign: 'center' }}>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            Already have an account?{' '}
            <Link component={RouterLink} to="/login" sx={{ color: 'primary.main' }}>
              Sign in
            </Link>
          </Typography>
        </Box>
      </GlassCard>
    </Box>
  );
};
