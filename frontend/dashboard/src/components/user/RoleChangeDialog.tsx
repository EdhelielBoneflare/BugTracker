import React from 'react';
import {
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Stack,
    Avatar,
    Box,
    Typography,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Button,
} from '@mui/material';
import { User, UserRole } from '../../types/types';

interface RoleChangeDialogProps {
    open: boolean;
    selectedUser: User | null;
    selectedRole: UserRole;
    onClose: () => void;
    onRoleChange: () => void;
    onRoleSelect: (role: UserRole) => void;
    getUserAvatar: (username: string) => string;
}

const RoleChangeDialog: React.FC<RoleChangeDialogProps> = ({
                                                               open,
                                                               selectedUser,
                                                               selectedRole,
                                                               onClose,
                                                               onRoleChange,
                                                               onRoleSelect,
                                                               getUserAvatar,
                                                           }) => {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Change User Role</DialogTitle>
            <DialogContent>
                <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 3 }}>
                    <Avatar sx={{ bgcolor: 'primary.main' }}>
                        {selectedUser && getUserAvatar(selectedUser.username)}
                    </Avatar>
                    <Box>
                        <Typography variant="subtitle1" fontWeight="medium">
                            {selectedUser?.username}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                            ID: #{selectedUser?.id.substring(0, 8)}...
                        </Typography>
                    </Box>
                </Stack>

                <Typography variant="body2" gutterBottom>
                    Current role: <strong>{selectedUser?.role}</strong>
                </Typography>

                <FormControl fullWidth sx={{ mt: 3 }}>
                    <InputLabel>New Role</InputLabel>
                    <Select
                        value={selectedRole}
                        onChange={(e) => onRoleSelect(e.target.value as UserRole)}
                        label="New Role"
                    >
                        <MenuItem value={UserRole.DEVELOPER}>Developer</MenuItem>
                        <MenuItem value={UserRole.PM}>Project Manager</MenuItem>
                        <MenuItem value={UserRole.ADMIN}>Admin</MenuItem>
                    </Select>
                </FormControl>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button
                    onClick={onRoleChange}
                    variant="contained"
                >
                    Update Role
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default RoleChangeDialog;