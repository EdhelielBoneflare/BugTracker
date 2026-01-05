import React, { useState } from 'react';
import {
    AppBar,
    Toolbar,
    Typography,
    IconButton,
    Avatar,
    Menu,
    MenuItem,
    Box,
    Button,
} from '@mui/material';
import { BugReport, Dashboard as DashboardIcon, People, ExitToApp } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

const Header: React.FC = () => {
    const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
    const navigate = useNavigate();
    const { user, logout, isAdmin } = useAuth();

    const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setAnchorEl(null);
    };

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const handleDashboard = () => {
        navigate('/dashboard');
    };

    const handleUsers = () => {
        navigate('/users');
    };

    return (
        <AppBar position="static" elevation={1}>
            <Toolbar>
                <BugReport sx={{ mr: 2 }} />
                <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                    BugTracker
                </Typography>

                {user && (
                    <>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Button
                                color="inherit"
                                startIcon={<DashboardIcon />}
                                onClick={handleDashboard}
                            >
                                Dashboard
                            </Button>

                            {(
                                <Button
                                    color="inherit"
                                    startIcon={<People />}
                                    onClick={handleUsers}
                                >
                                    Users
                                </Button>
                            )}
                        </Box>

                        <IconButton onClick={handleMenuOpen} color="inherit">
                            <Avatar sx={{ bgcolor: 'secondary.main', width: 32, height: 32 }}>
                                {user.username.charAt(0).toUpperCase()}
                            </Avatar>
                        </IconButton>

                        <Menu
                            anchorEl={anchorEl}
                            open={Boolean(anchorEl)}
                            onClose={handleMenuClose}
                        >
                            <MenuItem disabled>
                                <Typography variant="body2" color="text.secondary">
                                    {user.username} ({user.role})
                                </Typography>
                            </MenuItem>
                            <MenuItem onClick={handleLogout}>
                                <ExitToApp sx={{ mr: 1, fontSize: 20 }} />
                                Logout
                            </MenuItem>
                        </Menu>
                    </>
                )}
            </Toolbar>
        </AppBar>
    );
};

export default Header;