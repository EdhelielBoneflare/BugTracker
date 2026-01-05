import React, {useEffect, useState} from 'react';
import {Alert, Box, CircularProgress, Typography,} from '@mui/material';
import {AdminPanelSettings, Engineering, ManageAccounts,} from '@mui/icons-material';
import {useAuth} from '../contexts/AuthContext';
import {api} from '../api/Api';
import {Project, User, UserRole} from '../types/types';
import UserTable from '../components/user/UserTable';
import RoleChangeDialog from '../components/user/RoleChangeDialog';
import AssignProjectDialog from '../components/user/AssignProjectDialog';

const UsersPage: React.FC = () => {
    const { user: currentUser, isAdmin, isPM, isDeveloper } = useAuth();
    const [users, setUsers] = useState<User[]>([]);
    const [projects, setProjects] = useState<Project[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [openRoleDialog, setOpenRoleDialog] = useState(false);
    const [openAssignDialog, setOpenAssignDialog] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [selectedProjectId, setSelectedProjectId] = useState<string>('');
    const [selectedRole, setSelectedRole] = useState<UserRole>(UserRole.DEVELOPER);
    const [userProjects, setUserProjects] = useState<Map<string, Project[]>>(new Map());

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            setLoading(true);

            const usersResponse = await api.getAllUsers(0, 100);
            const usersList = usersResponse.content;
            setUsers(usersList);

            let projectsData: Project[] = [];

            if (isAdmin()) {
                projectsData = await api.getAllProjects();
            } else if (isPM()) {
                projectsData = await api.getMyProjects();
            } else if (isDeveloper()) {
                try {
                    projectsData = await api.getMyProjects();
                } catch (error) {
                    console.log('Developer has no projects assigned, using empty array');
                    projectsData = [];
                }
            }

            setProjects(projectsData);

            const projectsMap = new Map<string, Project[]>();

            for (const user of usersList) {
                const userProjectIds = user.projectIds || [];

                const userProjectObjects = projectsData.length > 0
                    ? projectsData.filter(project =>
                        userProjectIds.includes(project.id)
                    )
                    : [];

                projectsMap.set(user.id, userProjectObjects);
            }

            setUserProjects(projectsMap);
            setError(null);
        } catch (err: any) {
            setError(err.message || 'Failed to load data');
        } finally {
            setLoading(false);
        }
    };

    const handleAssignProject = async () => {
        if (!selectedUser || !selectedProjectId) return;

        try {
            console.log('Assigning user to project:', {
                userId: selectedUser.id,
                projectId: selectedProjectId,
                isAdmin: isAdmin(),
                isPM: isPM()
            });

            await api.assignUserToProject(selectedUser.id, selectedProjectId);

            await fetchData();

            setOpenAssignDialog(false);
            setSelectedUser(null);
            setSelectedProjectId('');
            setError(null);

        } catch (err: any) {
            console.error('Assign project error:', err);
            setError(err.message || 'Failed to assign project');
        }
    };

    const handleRemoveProject = async (userId: string, projectId: string) => {
        const user = users.find(u => u.id === userId);
        if (!user) return;

        const projectName = projects.find(p => p.id === projectId)?.name || projectId;
        const userName = user.username;

        if (!window.confirm(`Are you sure you want to remove ${userName} from project "${projectName}"?`)) return;

        try {
            console.log('Removing user from project:', {
                userId,
                projectId,
                userRole: user.role,
                isAdmin: isAdmin(),
                isPM: isPM()
            });

            await api.removeUserFromProject(userId, projectId);

            await fetchData();

            setError(null);

        } catch (err: any) {
            console.error('Remove project error:', err);
            setError(err.message || 'Failed to remove from project');
        }
    };

    const canChangeRole = (user: User) => {
        if (!isAdmin()) return false;
        if (user.id === currentUser?.id) return false;
        return true;
    };

    const canAssignProject = (user: User) => {
        if (isDeveloper()) return false;

        if (isAdmin()) return true;

        if (isPM() && user.role === UserRole.DEVELOPER) return true;

        return false;
    };

    const canRemoveFromProject = (userId: string, projectId: string) => {
        const user = users.find(u => u.id === userId);
        if (!user) return false;

        if (isDeveloper()) return false;

        if (isAdmin()) return true;

        if (isPM() && user.role === UserRole.DEVELOPER) {
            return projects.some(p => p.id === projectId);
        }

        return false;
    };

    const handleRoleChange = async () => {
        if (!selectedUser || !selectedRole) return;

        try {
            await api.updateUserRole(selectedUser.id, selectedRole);
            fetchData(); // Перезагружаем данные
            setOpenRoleDialog(false);
            setSelectedUser(null);
            setSelectedRole(UserRole.DEVELOPER);
        } catch (err: any) {
            setError(err.message || 'Failed to update role');
        }
    };

    const getRoleIcon = (role: UserRole) => {
        switch (role) {
            case UserRole.ADMIN: return <AdminPanelSettings />;
            case UserRole.PM: return <ManageAccounts />;
            case UserRole.DEVELOPER: return <Engineering />;
            default: return <Engineering />;
        }
    };

    const getRoleColor = (role: UserRole) => {
        switch (role) {
            case UserRole.ADMIN: return 'error';
            case UserRole.PM: return 'warning';
            case UserRole.DEVELOPER: return 'primary';
            default: return 'default';
        }
    };

    const getUserAvatar = (username: string) => {
        return username.charAt(0).toUpperCase();
    };

    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Box>
            <Box sx={{ mb: 3 }}>
                <Typography variant="h4" gutterBottom>
                    User Management
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    {isAdmin()
                        ? 'Manage user roles and project assignments'
                        : isPM()
                            ? 'Manage project assignments for developers'
                            : 'View users and their project assignments'}
                </Typography>
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            {/* Users List Table */}
            <UserTable
                users={users}
                currentUser={currentUser}
                userProjects={userProjects}
                projects={projects}
                isAdmin={isAdmin}
                isPM={isPM}
                isDeveloper={isDeveloper}
                canChangeRole={canChangeRole}
                canAssignProject={canAssignProject}
                canRemoveFromProject={canRemoveFromProject}
                onRoleChangeClick={(user) => {
                    setSelectedUser(user);
                    setSelectedRole(user.role as UserRole);
                    setOpenRoleDialog(true);
                }}
                onAssignProjectClick={(user) => {
                    setSelectedUser(user);
                    setSelectedProjectId('');
                    setOpenAssignDialog(true);
                }}
                onRemoveProjectClick={handleRemoveProject}
                getRoleIcon={getRoleIcon}
                getRoleColor={getRoleColor}
                getUserAvatar={getUserAvatar}
            />

            {/* Role Change Dialog - ТОЛЬКО ДЛЯ АДМИНА */}
            {isAdmin() && (
                <RoleChangeDialog
                    open={openRoleDialog}
                    selectedUser={selectedUser}
                    selectedRole={selectedRole}
                    onClose={() => setOpenRoleDialog(false)}
                    onRoleChange={handleRoleChange}
                    onRoleSelect={setSelectedRole}
                    getUserAvatar={getUserAvatar}
                />
            )}

            {/* Assign Project Dialog - ТОЛЬКО ДЛЯ АДМИНА И PM */}
            {(isAdmin() || isPM()) && (
                <AssignProjectDialog
                    open={openAssignDialog}
                    selectedUser={selectedUser}
                    selectedProjectId={selectedProjectId}
                    projects={projects}
                    userProjects={userProjects}
                    onClose={() => setOpenAssignDialog(false)}
                    onAssignProject={handleAssignProject}
                    onProjectSelect={setSelectedProjectId}
                    getUserAvatar={getUserAvatar}
                    getRoleColor={getRoleColor}
                />
            )}
        </Box>
    );
};

export default UsersPage;