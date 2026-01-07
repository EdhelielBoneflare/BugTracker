import React, { useState } from 'react';
import {
    Container,
    Paper,
    Tabs,
    Tab,
    Box,
    Typography,
    Alert,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import '../styles/styles.css';

const LoginPage: React.FC = () => {
    const [activeTab, setActiveTab] = useState(0);
    const [username, setUsername] = useState('admin');
    const [password, setPassword] = useState('admin123');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const { login, register } = useAuth();

    const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
        setActiveTab(newValue);
        setMessage(null);
        setUsername('');
        setPassword('');
        setConfirmPassword('');
    };

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setMessage(null);

        try {
            const success = await login(username, password);
            if (success) {
                setMessage({ type: 'success', text: 'Login successful! Redirecting...' });
                setTimeout(() => navigate('/dashboard'), 1000);
            } else {
                setMessage({ type: 'error', text: 'Invalid username or password' });
            }
        } catch (error) {
            setMessage({ type: 'error', text: 'An error occurred during login' });
        } finally {
            setLoading(false);
        }
    };

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();

        if (password !== confirmPassword) {
            setMessage({ type: 'error', text: 'Passwords do not match' });
            return;
        }

        if (password.length < 6) {
            setMessage({ type: 'error', text: 'Password must be at least 6 characters long' });
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            const success = await register(username, password);
            if (success) {
                setMessage({ type: 'success', text: 'Registration successful! Please sign in.' });
                setActiveTab(0);
                setUsername('');
                setPassword('');
                setConfirmPassword('');
            } else {
                setMessage({ type: 'error', text: 'Registration failed. Username may already exist.' });
            }
        } catch (error) {
            setMessage({ type: 'error', text: 'An error occurred during registration' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <Container maxWidth="sm">
                <Box sx={{ mt: 8, mb: 4 }}>
                    <Typography variant="h3" component="h1" align="center" gutterBottom className="auth-title">
                        BugTracker
                    </Typography>
                    <Typography variant="subtitle1" align="center" color="text.secondary">
                        Error Monitoring & Analytics Platform
                    </Typography>
                </Box>

                <Paper elevation={3} className="auth-paper">
                    {message && (
                        <Alert
                            severity={message.type}
                            sx={{ mb: 2 }}
                            onClose={() => setMessage(null)}
                        >
                            {message.text}
                        </Alert>
                    )}

                    <Tabs
                        value={activeTab}
                        onChange={handleTabChange}
                        variant="fullWidth"
                        indicatorColor="primary"
                        textColor="primary"
                    >
                        <Tab label="Sign In" />
                        <Tab label="Sign Up" />
                    </Tabs>

                    <Box sx={{ p: 3 }}>
                        {activeTab === 0 ? (
                            <form onSubmit={handleLogin}>
                                <input type="text" hidden autoComplete="username" />
                                <input type="password" hidden autoComplete="current-password" />

                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                    <input
                                        type="text"
                                        placeholder="Username"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        disabled={loading}
                                        required
                                        style={{
                                            padding: '12px',
                                            border: '1px solid #ccc',
                                            borderRadius: '4px',
                                            fontSize: '16px',
                                        }}
                                    />
                                    <input
                                        type="password"
                                        placeholder="Password"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        disabled={loading}
                                        required
                                        style={{
                                            padding: '12px',
                                            border: '1px solid #ccc',
                                            borderRadius: '4px',
                                            fontSize: '16px',
                                        }}
                                    />
                                    <button
                                        type="submit"
                                        disabled={loading || !username || !password}
                                        style={{
                                            padding: '12px',
                                            backgroundColor: loading ? '#ccc' : '#667eea',
                                            color: 'white',
                                            border: 'none',
                                            borderRadius: '4px',
                                            fontSize: '16px',
                                            cursor: loading ? 'not-allowed' : 'pointer',
                                        }}
                                    >
                                        {loading ? 'Signing in...' : 'Sign In'}
                                    </button>
                                </Box>
                            </form>
                        ) : (
                            <form onSubmit={handleRegister}>
                                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                    <input
                                        type="text"
                                        placeholder="Username"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        disabled={loading}
                                        required
                                        style={{
                                            padding: '12px',
                                            border: '1px solid #ccc',
                                            borderRadius: '4px',
                                            fontSize: '16px',
                                        }}
                                    />
                                    <input
                                        type="password"
                                        placeholder="Password (min 6 characters)"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        disabled={loading}
                                        required
                                        style={{
                                            padding: '12px',
                                            border: '1px solid #ccc',
                                            borderRadius: '4px',
                                            fontSize: '16px',
                                        }}
                                    />
                                    <input
                                        type="password"
                                        placeholder="Confirm Password"
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        disabled={loading}
                                        required
                                        style={{
                                            padding: '12px',
                                            border: '1px solid #ccc',
                                            borderRadius: '4px',
                                            fontSize: '16px',
                                        }}
                                    />
                                    <button
                                        type="submit"
                                        disabled={loading || !username || !password || !confirmPassword}
                                        style={{
                                            padding: '12px',
                                            backgroundColor: loading ? '#ccc' : '#667eea',
                                            color: 'white',
                                            border: 'none',
                                            borderRadius: '4px',
                                            fontSize: '16px',
                                            cursor: loading ? 'not-allowed' : 'pointer',
                                        }}
                                    >
                                        {loading ? 'Registering...' : 'Sign Up'}
                                    </button>
                                </Box>
                            </form>
                        )}
                    </Box>
                </Paper>

                <Box sx={{ mt: 4, textAlign: 'center' }}>
                    <Typography variant="body2" color="text.secondary">
                        Test credentials: admin / admin123
                    </Typography>
                </Box>
            </Container>
        </div>
    );
};

export default LoginPage;