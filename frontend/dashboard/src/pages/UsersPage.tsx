import React, { useEffect, useState } from 'react';
       import { Alert, Box, CircularProgress, Typography } from '@mui/material';
       import { AdminPanelSettings, Engineering, ManageAccounts } from '@mui/icons-material';
       import { useAuth } from '../contexts/AuthContext';
       import { api } from '../api/Api';
       import { NotificationState, Project, User, UserRole } from '../types/types';
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
           const [notificationStates, setNotificationStates] = useState<Map<string, NotificationState>>(new Map());
           const [setupLoadingUserId, setSetupLoadingUserId] = useState<string | null>(null);

           const makeNotifKey = (userId: string, projectId: string) => `${userId}:${projectId}`;

           useEffect(() => {
               fetchData();
           }, []);

           const loadNotificationStates = async (projectsData: Project[]) => {
               const stateMap = new Map<string, NotificationState>();

               await Promise.all(
                   projectsData.map(async (project) => {
                       try {
                           const items = await api.getNotificationStates(project.id);

                           items.forEach((item) => {
                               // Keep compatibility if backend still returns devId instead of userId
                               const userId = item.userId ?? item.devId;
                               if (!userId) return;

                               stateMap.set(makeNotifKey(userId, project.id), item.state);
                           });
                       } catch (err) {
                           console.warn(`Failed to load notification states for project ${project.id}:`, err);
                       }
                   })
               );

               setNotificationStates(stateMap);
           };

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
                       } catch {
                           console.log('Developer has no projects assigned, using empty array');
                           projectsData = [];
                       }
                   }

                   setProjects(projectsData);

                   const projectsMap = new Map<string, Project[]>();
                   for (const user of usersList) {
                       const userProjectIds = user.projectIds || [];
                       const userProjectObjects =
                           projectsData.length > 0
                               ? projectsData.filter((project) => userProjectIds.includes(project.id))
                               : [];

                       projectsMap.set(user.id, userProjectObjects);
                   }

                   setUserProjects(projectsMap);
                   await loadNotificationStates(projectsData);
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
                   await api.assignUserToProject(selectedUser.id, selectedProjectId);
                   await fetchData();

                   setOpenAssignDialog(false);
                   setSelectedUser(null);
                   setSelectedProjectId('');
                   setError(null);
               } catch (err: any) {
                   setError(err.message || 'Failed to assign project');
               }
           };

           const handleRemoveProject = async (userId: string, projectId: string) => {
               const user = users.find((u) => u.id === userId);
               if (!user) return;

               const projectName = projects.find((p) => p.id === projectId)?.name || projectId;
               if (!window.confirm(`Are you sure you want to remove ${user.username} from project "${projectName}"?`)) return;

               try {
                   await api.removeUserFromProject(userId, projectId);
                   await fetchData();
                   setError(null);
               } catch (err: any) {
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
               const user = users.find((u) => u.id === userId);
               if (!user) return false;

               if (isDeveloper()) return false;
               if (isAdmin()) return true;

               if (isPM() && user.role === UserRole.DEVELOPER) {
                   return projects.some((p) => p.id === projectId);
               }

               return false;
           };

           const getNotificationState = (userId: string, projectId: string): NotificationState | null => {
               return notificationStates.get(makeNotifKey(userId, projectId)) ?? null;
           };

           const canSetupNotification = (user: User) => {
               if (isDeveloper()) return false;
               if (isAdmin()) return true;
               return isPM() && user.role === UserRole.DEVELOPER;
           };

           const handleSetupNotification = async (userId: string, projectId: string) => {
               const loadingKey = makeNotifKey(userId, projectId);

               try {
                   setSetupLoadingUserId(loadingKey);

                   const response = await api.setupUserNotifications(userId, projectId);

                   if (!response?.setupUrl) {
                       throw new Error('Setup URL was not returned by backend');
                   }

                   // Requirement: redirect to returned link
                   window.location.href = response.setupUrl;
               } catch (err: any) {
                   setError(err.message || 'Failed to setup notifications');
               } finally {
                   setSetupLoadingUserId(null);
               }
           };

           const handleRoleChange = async () => {
               if (!selectedUser || !selectedRole) return;

               try {
                   await api.updateUserRole(selectedUser.id, selectedRole);
                   await fetchData();

                   setOpenRoleDialog(false);
                   setSelectedUser(null);
                   setSelectedRole(UserRole.DEVELOPER);
               } catch (err: any) {
                   setError(err.message || 'Failed to update role');
               }
           };

           const getRoleIcon = (role: UserRole) => {
               switch (role) {
                   case UserRole.ADMIN:
                       return <AdminPanelSettings />;
                   case UserRole.PM:
                       return <ManageAccounts />;
                   case UserRole.DEVELOPER:
                       return <Engineering />;
                   default:
                       return <Engineering />;
               }
           };

           const getRoleColor = (role: UserRole) => {
               switch (role) {
                   case UserRole.ADMIN:
                       return 'error';
                   case UserRole.PM:
                       return 'warning';
                   case UserRole.DEVELOPER:
                       return 'primary';
                   default:
                       return 'default';
               }
           };

           const getUserAvatar = (username: string) => username.charAt(0).toUpperCase();

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
                       getNotificationState={getNotificationState}
                       canSetupNotification={canSetupNotification}
                       setupLoadingUserId={setupLoadingUserId}
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
                       onSetupNotificationClick={handleSetupNotification}
                       getRoleIcon={getRoleIcon}
                       getRoleColor={getRoleColor}
                       getUserAvatar={getUserAvatar}
                   />

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