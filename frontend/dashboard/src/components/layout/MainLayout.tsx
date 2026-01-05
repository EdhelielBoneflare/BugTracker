import React from 'react';
import { Box, Container, Typography } from '@mui/material';
import Header from './Header';

interface MainLayoutProps {
    children: React.ReactNode;
    maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | false;
    disableFooter?: boolean;
}

const MainLayout: React.FC<MainLayoutProps> = ({
                                                   children,
                                                   maxWidth = 'xl',
                                                   disableFooter = false
                                               }) => {
    const currentYear = new Date().getFullYear();

    return (
        <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            minHeight: '100vh',
            bgcolor: 'background.default'
        }}>
            <Header />
            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    py: { xs: 2, sm: 3 },
                    display: 'flex',
                    flexDirection: 'column'
                }}
            >
                <Container
                    maxWidth={maxWidth}
                    sx={{
                        flexGrow: 1,
                        display: 'flex',
                        flexDirection: 'column'
                    }}
                >
                    {children}
                </Container>
            </Box>

            {!disableFooter && (
                <Box
                    component="footer"
                    sx={{
                        py: 2,
                        bgcolor: 'grey.100',
                        borderTop: 1,
                        borderColor: 'divider',
                        mt: 'auto'
                    }}
                >
                    <Container maxWidth={maxWidth}>
                        <Typography
                            variant="body2"
                            color="text.secondary"
                            align="center"
                            sx={{ fontSize: '0.875rem' }}
                        >
                            © {currentYear} BugTracker — Error Monitoring Platform
                        </Typography>
                    </Container>
                </Box>
            )}
        </Box>
    );
};

export default MainLayout;