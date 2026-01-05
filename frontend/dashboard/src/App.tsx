import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import MainLayout from './components/layout/MainLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ProjectReportsPage from './pages/ProjectReportsPage';
import ReportDetailsPage from './pages/ReportDetailsPage';
import UsersPage from './pages/UsersPage';

const PrivateRoute: React.FC<{ children: React.ReactNode; requireAdmin?: boolean }> = ({
                                                                                           children,
                                                                                           requireAdmin = false
                                                                                       }) => {
    const { user, loading, isAdmin } = useAuth();

    if (loading) {
        return (
            <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                height: '100vh'
            }}>
                Loading...
            </div>
        );
    }

    if (!user) {
        return <Navigate to="/login" />;
    }

    if (requireAdmin && !isAdmin()) {
        return <Navigate to="/dashboard" />;
    }

    return (
        <MainLayout>
            {children}
        </MainLayout>
    );
};

const App: React.FC = () => {
    return (
        <AuthProvider>
            <Router>
                <Routes>
                    {/* Публичные маршруты */}
                    <Route path="/login" element={<LoginPage />} />

                    {/* Приватные маршруты */}
                    <Route path="/dashboard" element={
                        <PrivateRoute>
                            <DashboardPage />
                        </PrivateRoute>
                    } />

                    <Route path="/projects/:projectId/reports" element={
                        <PrivateRoute>
                            <ProjectReportsPage />
                        </PrivateRoute>
                    } />

                    <Route path="/reports/:reportId" element={
                        <PrivateRoute>
                            <ReportDetailsPage />
                        </PrivateRoute>
                    } />

                    <Route path="/users" element={
                        <PrivateRoute>
                            <UsersPage />
                        </PrivateRoute>
                    } />

                    {/* Дефолтный маршрут */}
                    <Route path="/" element={<Navigate to="/dashboard" />} />
                </Routes>
            </Router>
        </AuthProvider>
    );
};

export default App;