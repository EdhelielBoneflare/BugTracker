import React, { useState, useEffect } from 'react';
import {
    Box,
    Card,
    CardContent,
    Typography,
    Button,
    CircularProgress,
    Alert,
    TextField,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    IconButton,
    Stack,
} from '@mui/material';
import { Add, Edit, Delete, ArrowForward, Error as ErrorIcon} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { api } from '../api/Api';
import { Project } from '../types/types';

const DashboardPage: React.FC = () => {
    const [projects, setProjects] = useState<Project[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [openDialog, setOpenDialog] = useState(false);
    const [newProjectName, setNewProjectName] = useState('');
    const [editingProject, setEditingProject] = useState<Project | null>(null);
    const navigate = useNavigate();
    const { isAdmin} = useAuth();

    useEffect(() => {
        fetchProjects();
    }, []);

    const fetchProjects = async () => {
        try {
            setLoading(true);
            const data = isAdmin()
                ? await api.getAllProjects()
                : await api.getMyProjects();
            setProjects(Array.isArray(data) ? data : []);
            setError(null);
        } catch (err: any) {
            setError(err.message || 'Failed to load projects');
            setProjects([]);
        } finally {
            setLoading(false);
        }
    };

    const canEditProject = () => {
        return isAdmin();
    };

    const canDeleteProject = () => {
        return isAdmin();
    };

    const handleCreateProject = async () => {
        try {
            await api.createProject({
                projectName: newProjectName.trim(),
            });
            setOpenDialog(false);
            setNewProjectName('');
            fetchProjects();
        } catch (err: any) {
            setError(err.message || 'Failed to create project');
        }
    };

    const handleUpdateProject = async () => {
        if (!editingProject) return;

        try {
            await api.updateProject(editingProject.id, {
                projectName: newProjectName.trim(),
            });
            setOpenDialog(false);
            setEditingProject(null);
            setNewProjectName('');
            fetchProjects();
        } catch (err: any) {
            setError(err.message || 'Failed to update project');
        }
    };

    const handleDeleteProject = async (projectId: string) => {
        if (!window.confirm('Are you sure you want to delete this project? All reports and sessions will be deleted.')) return;

        try {
            await api.deleteProject(projectId);
            fetchProjects();
        } catch (err: any) {
            const errorMessage = err.message || 'Failed to delete project';

            if (errorMessage.includes('assigned')) {
                setError('Cannot delete project with assigned users. Remove users first.');
            } else {
                setError(errorMessage);
            }
        }
    };

    const handleProjectClick = (projectId: string) => {
        navigate(`/projects/${projectId}/reports`);
    };

    const handleEditClick = (project: Project) => {
        setEditingProject(project);
        setNewProjectName(project.name || project.projectName || '');
        setOpenDialog(true);
    };

    const handleDialogClose = () => {
        setOpenDialog(false);
        setEditingProject(null);
        setNewProjectName('');
    };

    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    const safeProjects = Array.isArray(projects) ? projects : [];

    return (
        <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
                <Typography variant="h4">
                    {isAdmin() ? 'All Projects' : 'My Projects'}
                </Typography>
                {canEditProject() && (
                    <Button
                        variant="contained"
                        startIcon={<Add />}
                        onClick={() => setOpenDialog(true)}
                    >
                        New Project
                    </Button>
                )}
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            {safeProjects.length === 0 ? (
                <Card>
                    <CardContent sx={{ textAlign: 'center', py: 4 }}>
                        <ErrorIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                        <Typography variant="h6" color="text.secondary">
                            No projects found
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                            {canEditProject()
                                ? 'Create your first project to get started'
                                : 'You are not assigned to any projects yet'}
                        </Typography>
                        {canEditProject() && (
                            <Button
                                variant="outlined"
                                startIcon={<Add />}
                                onClick={() => setOpenDialog(true)}
                                sx={{ mt: 2 }}
                            >
                                Create Project
                            </Button>
                        )}
                    </CardContent>
                </Card>
            ) : (
                <Box sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' },
                    gap: 3
                }}>
                    {safeProjects.map((project) => (
                        <Card
                            key={project.id}
                            sx={{
                                height: '100%',
                                display: 'flex',
                                flexDirection: 'column',
                                transition: 'transform 0.2s',
                                '&:hover': {
                                    transform: 'translateY(-4px)',
                                    boxShadow: 3,
                                }
                            }}
                        >
                            <CardContent sx={{ flexGrow: 1 }}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                                    <Box>
                                        <Typography variant="h6" component="div">
                                            {project.name || project.projectName}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            ID: {project.id.substring(0, 8)}
                                        </Typography>
                                    </Box>
                                    {canEditProject() && (
                                        <Stack direction="row" spacing={0.5}>
                                            <IconButton
                                                size="small"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleEditClick(project);
                                                }}
                                                disabled={!canEditProject()}
                                            >
                                                <Edit fontSize="small" />
                                            </IconButton>
                                            <IconButton
                                                size="small"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    handleDeleteProject(project.id);
                                                }}
                                                disabled={!canDeleteProject()}
                                            >
                                                <Delete fontSize="small" />
                                            </IconButton>
                                        </Stack>
                                    )}
                                </Box>
                                {project.createdAt && (
                                    <Typography variant="caption" color="text.secondary" display="block">
                                        Created: {new Date(project.createdAt).toLocaleDateString()}
                                    </Typography>
                                )}
                            </CardContent>
                            <Box sx={{ p: 2, pt: 0 }}>
                                <Button
                                    fullWidth
                                    variant="outlined"
                                    endIcon={<ArrowForward />}
                                    onClick={() => handleProjectClick(project.id)}
                                >
                                    View Reports
                                </Button>
                            </Box>
                        </Card>
                    ))}
                </Box>
            )}

            <Dialog open={openDialog} onClose={handleDialogClose} maxWidth="sm" fullWidth>
                <DialogTitle>
                    {editingProject ? 'Edit Project' : 'Create New Project'}
                </DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="Project Name"
                        type="text"
                        fullWidth
                        value={newProjectName}
                        onChange={(e) => setNewProjectName(e.target.value)}
                        sx={{ mb: 2 }}
                        error={!newProjectName.trim()}
                        helperText={!newProjectName.trim() ? "Project name is required" : ""}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleDialogClose}>Cancel</Button>
                    <Button
                        onClick={editingProject ? handleUpdateProject : handleCreateProject}
                        variant="contained"
                        disabled={!newProjectName.trim()}
                    >
                        {editingProject ? 'Update' : 'Create'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default DashboardPage;