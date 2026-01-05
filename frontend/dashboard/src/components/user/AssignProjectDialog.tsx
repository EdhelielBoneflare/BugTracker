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
    Chip,
    Alert,
} from '@mui/material';
import { User, Project, UserRole } from '../../types/types';

interface AssignProjectDialogProps {
    open: boolean;
    selectedUser: User | null;
    selectedProjectId: string;
    projects: Project[];
    userProjects: Map<string, Project[]>;
    onClose: () => void;
    onAssignProject: () => void;
    onProjectSelect: (projectId: string) => void;
    getUserAvatar: (username: string) => string;
    getRoleColor: (role: UserRole) => 'error' | 'warning' | 'primary' | 'default';
}

const AssignProjectDialog: React.FC<AssignProjectDialogProps> = ({
                                                                     open,
                                                                     selectedUser,
                                                                     selectedProjectId,
                                                                     projects,
                                                                     userProjects,
                                                                     onClose,
                                                                     onAssignProject,
                                                                     onProjectSelect,
                                                                     getUserAvatar,
                                                                     getRoleColor,
                                                                 }) => {
    const userProjectsList = selectedUser ? userProjects.get(selectedUser.id) || [] : [];

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>
                Assign Project
            </DialogTitle>
            <DialogContent>
                {selectedUser && (
                    <>
                        <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 3 }}>
                            <Avatar sx={{ bgcolor: 'primary.main' }}>
                                {getUserAvatar(selectedUser.username)}
                            </Avatar>
                            <Box>
                                <Typography variant="subtitle1" fontWeight="medium">
                                    {selectedUser.username}
                                </Typography>
                                <Stack direction="row" spacing={1} alignItems="center">
                                    <Chip
                                        label={selectedUser.role}
                                        size="small"
                                        color={getRoleColor(selectedUser.role as UserRole)}
                                    />
                                </Stack>
                            </Box>
                        </Stack>
                    </>
                )}

                <Typography variant="body2" gutterBottom sx={{ mb: 2 }}>
                    Select project to assign:
                </Typography>

                <FormControl fullWidth>
                    <InputLabel>Project</InputLabel>
                    <Select
                        value={selectedProjectId}
                        onChange={(e) => onProjectSelect(e.target.value as string)}
                        label="Project"
                    >
                        {projects.map((project) => {
                            const isAssigned = userProjectsList.some(p => p.id === project.id);

                            return (
                                <MenuItem
                                    key={project.id}
                                    value={project.id}
                                >
                                    <Stack direction="row" justifyContent="space-between" sx={{ width: '100%' }}>
                                        <Box>
                                            <Typography>{project.name || project.projectName}</Typography>
                                        </Box>
                                        {isAssigned && (
                                            <Chip
                                                label="Assigned"
                                                size="small"
                                                color="info"
                                                variant="outlined"
                                            />
                                        )}
                                    </Stack>
                                </MenuItem>
                            );
                        })}
                    </Select>
                </FormControl>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button
                    onClick={onAssignProject}
                    variant="contained"
                    disabled={!selectedProjectId}
                >
                    Assign Project
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default AssignProjectDialog;