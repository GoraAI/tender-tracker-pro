import { useState } from 'react';
import {
  Box,
  Tab,
  Tabs,
  Typography,
  Button,
  Skeleton,
  TextField,
  Alert,
  Snackbar,
  Divider,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SaveIcon from '@mui/icons-material/Save';
import LockIcon from '@mui/icons-material/Lock';
import { GlassCard } from '../components/common/GlassCard';
import { CriteriaCard } from '../components/settings/CriteriaCard';
import { CriteriaEditor } from '../components/settings/CriteriaEditor';
import { SourceManager } from '../components/settings/SourceManager';
import { useCriteria } from '../hooks/useConfig';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/auth';
import type { Criteria, CriteriaCreate, CriteriaUpdate } from '../types';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

const TabPanel = ({ children, value, index }: TabPanelProps) => (
  <Box role="tabpanel" hidden={value !== index} sx={{ pt: 3 }}>
    {value === index && children}
  </Box>
);

export const SettingsPage = () => {
  const [tabValue, setTabValue] = useState(0);
  const [editingCriteria, setEditingCriteria] = useState<Criteria | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  // Profile state
  const { user, setUser } = useAuthStore();
  const [fullName, setFullName] = useState(user?.full_name || '');
  const [companyName, setCompanyName] = useState(user?.company_name || '');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileMessage, setProfileMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Password state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const {
    criteria,
    isLoading: loadingCriteria,
    createCriteria,
    updateCriteria,
    deleteCriteria,
    isCreating: isSavingCreate,
    isUpdating: isSavingUpdate,
  } = useCriteria();

  const handleSaveCriteria = async (data: CriteriaCreate | CriteriaUpdate) => {
    if (editingCriteria) {
      await updateCriteria(editingCriteria.id, data as CriteriaUpdate);
      setEditingCriteria(null);
    } else {
      await createCriteria(data as CriteriaCreate);
      setIsCreating(false);
    }
  };

  const handleDeleteCriteria = async (id: string) => {
    if (confirm('Are you sure you want to delete this criteria?')) {
      await deleteCriteria(id);
    }
  };

  const handleCancel = () => {
    setEditingCriteria(null);
    setIsCreating(false);
  };

  const handleSaveProfile = async () => {
    setIsSavingProfile(true);
    try {
      const updatedUser = await authApi.updateProfile({
        full_name: fullName || undefined,
        company_name: companyName || undefined,
      });
      setUser(updatedUser);
      setProfileMessage({ type: 'success', text: 'Profile updated successfully' });
    } catch {
      setProfileMessage({ type: 'error', text: 'Failed to update profile' });
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleChangePassword = async () => {
    if (newPassword !== confirmPassword) {
      setPasswordMessage({ type: 'error', text: 'New passwords do not match' });
      return;
    }
    if (newPassword.length < 6) {
      setPasswordMessage({ type: 'error', text: 'Password must be at least 6 characters' });
      return;
    }

    setIsChangingPassword(true);
    try {
      await authApi.changePassword({
        current_password: currentPassword,
        new_password: newPassword,
      });
      setPasswordMessage({ type: 'success', text: 'Password changed successfully' });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch {
      setPasswordMessage({ type: 'error', text: 'Failed to change password. Check your current password.' });
    } finally {
      setIsChangingPassword(false);
    }
  };

  return (
    <Box className="page-transition">
      <Typography variant="h4" sx={{ mb: 4, fontWeight: 700 }}>
        Settings
      </Typography>

      <GlassCard sx={{ mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={(_, v) => setTabValue(v)}
          sx={{
            '& .MuiTab-root': {
              color: 'text.secondary',
              '&.Mui-selected': {
                color: 'primary.main',
              },
            },
            '& .MuiTabs-indicator': {
              backgroundColor: 'primary.main',
            },
          }}
        >
          <Tab label="Scoring Criteria" />
          <Tab label="Sources" />
          <Tab label="Profile" />
        </Tabs>
      </GlassCard>

      <TabPanel value={tabValue} index={0}>
        {isCreating || editingCriteria ? (
          <CriteriaEditor
            criteria={editingCriteria}
            onSave={handleSaveCriteria}
            onCancel={handleCancel}
            isSaving={isSavingCreate || isSavingUpdate}
          />
        ) : (
          <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
              <Typography variant="h6">Your Scoring Criteria</Typography>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => setIsCreating(true)}
                sx={{
                  background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
                }}
              >
                New Criteria
              </Button>
            </Box>

            {loadingCriteria ? (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {[1, 2].map((i) => (
                  <Skeleton key={i} variant="rectangular" height={150} sx={{ borderRadius: 4 }} />
                ))}
              </Box>
            ) : criteria.length === 0 ? (
              <Box
                sx={{
                  textAlign: 'center',
                  py: 8,
                  background: 'rgba(30, 41, 59, 0.4)',
                  borderRadius: '16px',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                }}
              >
                <Typography variant="h6" sx={{ color: 'text.secondary', mb: 1 }}>
                  No scoring criteria yet
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  Create scoring criteria to personalize tender relevance scores.
                </Typography>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => setIsCreating(true)}
                  sx={{
                    background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
                  }}
                >
                  Create Your First Criteria
                </Button>
              </Box>
            ) : (
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                {criteria.map((c) => (
                  <CriteriaCard
                    key={c.id}
                    criteria={c}
                    onEdit={setEditingCriteria}
                    onDelete={handleDeleteCriteria}
                  />
                ))}
              </Box>
            )}
          </>
        )}
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Typography variant="h6" sx={{ mb: 3 }}>
          Tender Sources
        </Typography>
        <SourceManager />
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {/* Profile Information */}
          <GlassCard>
            <Typography variant="h6" sx={{ mb: 3 }}>
              Profile Information
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField
                label="Email"
                value={user?.email || ''}
                disabled
                fullWidth
                sx={{
                  '& .MuiOutlinedInput-root': {
                    background: 'rgba(30, 41, 59, 0.4)',
                  },
                }}
              />
              <TextField
                label="Full Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                fullWidth
                sx={{
                  '& .MuiOutlinedInput-root': {
                    background: 'rgba(30, 41, 59, 0.4)',
                  },
                }}
              />
              <TextField
                label="Company Name"
                value={companyName}
                onChange={(e) => setCompanyName(e.target.value)}
                fullWidth
                sx={{
                  '& .MuiOutlinedInput-root': {
                    background: 'rgba(30, 41, 59, 0.4)',
                  },
                }}
              />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  startIcon={<SaveIcon />}
                  onClick={handleSaveProfile}
                  disabled={isSavingProfile}
                  sx={{
                    background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
                  }}
                >
                  {isSavingProfile ? 'Saving...' : 'Save Profile'}
                </Button>
              </Box>
            </Box>
          </GlassCard>

          {/* Change Password */}
          <GlassCard>
            <Typography variant="h6" sx={{ mb: 3 }}>
              Change Password
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField
                label="Current Password"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                fullWidth
                sx={{
                  '& .MuiOutlinedInput-root': {
                    background: 'rgba(30, 41, 59, 0.4)',
                  },
                }}
              />
              <Divider sx={{ my: 1 }} />
              <TextField
                label="New Password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                fullWidth
                helperText="Minimum 6 characters"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    background: 'rgba(30, 41, 59, 0.4)',
                  },
                }}
              />
              <TextField
                label="Confirm New Password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                fullWidth
                error={confirmPassword !== '' && newPassword !== confirmPassword}
                helperText={confirmPassword !== '' && newPassword !== confirmPassword ? 'Passwords do not match' : ''}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    background: 'rgba(30, 41, 59, 0.4)',
                  },
                }}
              />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  startIcon={<LockIcon />}
                  onClick={handleChangePassword}
                  disabled={isChangingPassword || !currentPassword || !newPassword || !confirmPassword}
                  sx={{
                    background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
                  }}
                >
                  {isChangingPassword ? 'Changing...' : 'Change Password'}
                </Button>
              </Box>
            </Box>
          </GlassCard>

          {/* Account Info */}
          <GlassCard>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Account Information
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                <strong>Member since:</strong>{' '}
                {user?.created_at ? new Date(user.created_at).toLocaleDateString() : 'N/A'}
              </Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                <strong>Last updated:</strong>{' '}
                {user?.updated_at ? new Date(user.updated_at).toLocaleDateString() : 'N/A'}
              </Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                <strong>Account status:</strong>{' '}
                <span style={{ color: user?.is_active ? '#4ade80' : '#f87171' }}>
                  {user?.is_active ? 'Active' : 'Inactive'}
                </span>
              </Typography>
            </Box>
          </GlassCard>
        </Box>
      </TabPanel>

      {/* Snackbar for profile messages */}
      <Snackbar
        open={profileMessage !== null}
        autoHideDuration={4000}
        onClose={() => setProfileMessage(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={() => setProfileMessage(null)}
          severity={profileMessage?.type}
          sx={{ width: '100%' }}
        >
          {profileMessage?.text}
        </Alert>
      </Snackbar>

      {/* Snackbar for password messages */}
      <Snackbar
        open={passwordMessage !== null}
        autoHideDuration={4000}
        onClose={() => setPasswordMessage(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert
          onClose={() => setPasswordMessage(null)}
          severity={passwordMessage?.type}
          sx={{ width: '100%' }}
        >
          {passwordMessage?.text}
        </Alert>
      </Snackbar>
    </Box>
  );
};
