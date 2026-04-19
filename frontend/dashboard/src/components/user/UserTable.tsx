import React from 'react';
                        import {
                            Table,
                            TableBody,
                            TableCell,
                            TableContainer,
                            TableHead,
                            TableRow,
                            Paper,
                            Chip,
                            Stack,
                            Button,
                            Tooltip,
                            Avatar,
                            IconButton,
                            Box,
                            Typography,
                        } from '@mui/material';
                        import { Edit, Assignment, PersonRemove } from '@mui/icons-material';
                        import { NotificationState, User, Project, UserRole } from '../../types/types';

                        interface UserTableProps {
                            users: User[];
                            currentUser: User | null;
                            userProjects: Map<string, Project[]>;
                            projects: Project[];
                            isAdmin: () => boolean;
                            isPM: () => boolean;
                            isDeveloper: () => boolean;
                            canChangeRole: (user: User) => boolean;
                            canAssignProject: (user: User) => boolean;
                            canRemoveFromProject: (userId: string, projectId: string) => boolean;
                            getNotificationState: (userId: string, projectId: string) => NotificationState | null;
                            canSetupNotification: (user: User) => boolean;
                            setupLoadingUserId: string | null;
                            onRoleChangeClick: (user: User) => void;
                            onAssignProjectClick: (user: User) => void;
                            onRemoveProjectClick: (userId: string, projectId: string) => void;
                            onSetupNotificationClick: (userId: string, projectId: string) => void;
                            getRoleIcon: (role: UserRole) => React.ReactElement | undefined;
                            getRoleColor: (role: UserRole) => 'error' | 'warning' | 'primary' | 'default';
                            getUserAvatar: (username: string) => string;
                        }

                        const UserTable: React.FC<UserTableProps> = ({
                            users,
                            currentUser,
                            userProjects,
                            isAdmin,
                            isDeveloper,
                            canChangeRole,
                            canAssignProject,
                            canRemoveFromProject,
                            getNotificationState,
                            canSetupNotification,
                            setupLoadingUserId,
                            onRoleChangeClick,
                            onAssignProjectClick,
                            onRemoveProjectClick,
                            onSetupNotificationClick,
                            getRoleIcon,
                            getRoleColor,
                            getUserAvatar,
                        }) => {
                            const showActionsColumn = !isDeveloper();

                            return (
                                <TableContainer component={Paper}>
                                    <Table>
                                        <TableHead>
                                            <TableRow>
                                                <TableCell>User</TableCell>
                                                <TableCell>Role</TableCell>
                                                <TableCell>Assigned Projects & Notifications</TableCell>
                                                {showActionsColumn && <TableCell>Actions</TableCell>}
                                            </TableRow>
                                        </TableHead>

                                        <TableBody>
                                            {users.map((user) => {
                                                const userProjectsList = userProjects.get(user.id) || [];
                                                const canSetupForUser = canSetupNotification(user);

                                                return (
                                                    <TableRow key={user.id} hover>
                                                        <TableCell>
                                                            <Stack direction="row" alignItems="center" spacing={2}>
                                                                <Avatar sx={{ bgcolor: 'primary.main' }}>
                                                                    {getUserAvatar(user.username)}
                                                                </Avatar>

                                                                <Box>
                                                                    <Typography variant="body1" fontWeight="medium">
                                                                        {user.username}
                                                                    </Typography>
                                                                    <Typography variant="caption" color="text.secondary">
                                                                        ID: #{user.id.substring(0, 8)}...
                                                                    </Typography>
                                                                    {user.id === currentUser?.id && (
                                                                        <Chip label="You" size="small" color="primary" sx={{ mt: 0.5 }} />
                                                                    )}
                                                                </Box>
                                                            </Stack>
                                                        </TableCell>

                                                        <TableCell>
                                                            <Chip
                                                                icon={getRoleIcon(user.role as UserRole)}
                                                                label={user.role}
                                                                color={getRoleColor(user.role as UserRole)}
                                                                size="small"
                                                            />
                                                        </TableCell>

                                                        <TableCell>
                                                            {userProjectsList.length > 0 ? (
                                                                <Stack spacing={1}>
                                                                    {userProjectsList.map((project) => {
                                                                        const canRemove = canRemoveFromProject(user.id, project.id);
                                                                        const notifState = getNotificationState(user.id, project.id);
                                                                        const isActive = notifState === 'ACTIVE';
                                                                        const loadingKey = `${user.id}:${project.id}`;
                                                                        const isLoading = setupLoadingUserId === loadingKey;

                                                                        return (
                                                                            <Box
                                                                                key={project.id}
                                                                                sx={{
                                                                                    display: 'flex',
                                                                                    alignItems: 'center',
                                                                                    gap: 1,
                                                                                    flexWrap: 'wrap',
                                                                                }}
                                                                            >
                                                                                <Tooltip title={project.name || project.projectName || project.id}>
                                                                                    <Chip
                                                                                        label={project.name || project.projectName || project.id}
                                                                                        size="small"
                                                                                        variant="outlined"
                                                                                        color="primary"
                                                                                    />
                                                                                </Tooltip>

                                                                                {isActive ? (
                                                                                    <Box
                                                                                        sx={{
                                                                                            px: 1,
                                                                                            py: 0.25,
                                                                                            borderRadius: 1,
                                                                                            bgcolor: 'success.light',
                                                                                            color: 'success.dark',
                                                                                            fontSize: 12,
                                                                                            fontWeight: 600,
                                                                                        }}
                                                                                    >
                                                                                        Active
                                                                                    </Box>
                                                                                ) : (
                                                                                    canSetupForUser && (
                                                                                        <Button
                                                                                            size="small"
                                                                                            variant="text"
                                                                                            onClick={() => onSetupNotificationClick(user.id, project.id)}
                                                                                            disabled={isLoading}
                                                                                            sx={{ whiteSpace: 'nowrap' }}
                                                                                        >
                                                                                            {isLoading ? 'Setting up...' : 'Setup'}
                                                                                        </Button>
                                                                                    )
                                                                                )}

                                                                                {canRemove && (
                                                                                    <Tooltip title="Remove from project">
                                                                                        <IconButton
                                                                                            size="small"
                                                                                            onClick={() => onRemoveProjectClick(user.id, project.id)}
                                                                                            color="error"
                                                                                        >
                                                                                            <PersonRemove fontSize="small" />
                                                                                        </IconButton>
                                                                                    </Tooltip>
                                                                                )}
                                                                            </Box>
                                                                        );
                                                                    })}

                                                                    <Typography variant="caption" color="text.secondary">
                                                                        {userProjectsList.length} project(s) assigned
                                                                    </Typography>
                                                                </Stack>
                                                            ) : (
                                                                <Typography variant="caption" color="text.secondary">
                                                                    No projects assigned
                                                                </Typography>
                                                            )}
                                                        </TableCell>

                                                        {showActionsColumn && (
                                                            <TableCell>
                                                                <Stack direction="row" spacing={1}>
                                                                    {isAdmin() && (
                                                                        <Tooltip title="Change Role">
                                                                            <span>
                                                                                <Button
                                                                                    size="small"
                                                                                    variant="outlined"
                                                                                    startIcon={<Edit />}
                                                                                    onClick={() => onRoleChangeClick(user)}
                                                                                    disabled={!canChangeRole(user)}
                                                                                >
                                                                                    Role
                                                                                </Button>
                                                                            </span>
                                                                        </Tooltip>
                                                                    )}

                                                                    <Tooltip title="Assign Project">
                                                                        <span>
                                                                            <Button
                                                                                size="small"
                                                                                variant="outlined"
                                                                                startIcon={<Assignment />}
                                                                                onClick={() => onAssignProjectClick(user)}
                                                                                disabled={!canAssignProject(user)}
                                                                            >
                                                                                Assign
                                                                            </Button>
                                                                        </span>
                                                                    </Tooltip>
                                                                </Stack>
                                                            </TableCell>
                                                        )}
                                                    </TableRow>
                                                );
                                            })}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            );
                        };

                        export default UserTable;