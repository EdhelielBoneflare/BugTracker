import React from 'react';
import {
    Card,
    CardContent,
    Typography,
    Chip,
    Box,
    Divider,
    Paper,
} from '@mui/material';
import {
    Computer,
    Language,
    Web,
    DesktopWindows,
    Smartphone,
    Tablet,
} from '@mui/icons-material';
import { Session } from '../../types/types';

interface SessionDetailsCardProps {
    session: Session;
    getDeviceIcon: (deviceType?: string) => string;
}

const SessionDetailsCard: React.FC<SessionDetailsCardProps> = ({ session, getDeviceIcon }) => {
    const DeviceIcon = (deviceType?: string) => {
        const iconType = getDeviceIcon(deviceType);
        switch(iconType) {
            case 'Smartphone': return <Smartphone fontSize="small" />;
            case 'Tablet': return <Tablet fontSize="small" />;
            default: return <DesktopWindows fontSize="small" />;
        }
    };

    return (
        <Card>
            <CardContent>
                <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1} component="div">
                    <Computer />
                    Session Information
                </Typography>

                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' }, gap: 2, mb: 2 }}>
                    <Box>
                        <Typography variant="caption" color="text.secondary" display="block" component="div">
                            Status
                        </Typography>
                        <Chip
                            label={session.isActive ? 'Active' : 'Inactive'}
                            size="small"
                            color={session.isActive ? 'success' : 'default'}
                            sx={{ mt: 0.5 }}
                        />
                    </Box>
                </Box>

                <Divider sx={{ my: 2 }} />

                <Typography variant="subtitle2" gutterBottom sx={{ mb: 2 }} component="div">
                    Device Information
                </Typography>

                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' }, gap: 2, mb: 2 }}>
                    {session.browser && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Browser
                            </Typography>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <Computer fontSize="small" />
                                {session.browser} {session.browserVersion}
                            </Typography>
                        </Box>
                    )}

                    {session.os && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Operating System
                            </Typography>
                            <Typography variant="body2" component="div">
                                {session.os}
                            </Typography>
                        </Box>
                    )}

                    {session.deviceType && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Device Type
                            </Typography>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                {DeviceIcon(session.deviceType)}
                                {session.deviceType}
                            </Typography>
                        </Box>
                    )}

                    {session.ipAddress && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                IP Address
                            </Typography>
                            <Typography variant="body2" component="div">
                                {session.ipAddress}
                            </Typography>
                        </Box>
                    )}

                    {session.language && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Language
                            </Typography>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <Language fontSize="small" />
                                {session.language}
                            </Typography>
                        </Box>
                    )}

                    {session.screenResolution && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Screen Resolution
                            </Typography>
                            <Typography variant="body2" component="div">
                                {session.screenResolution}
                            </Typography>
                        </Box>
                    )}

                    {session.viewportSize && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Viewport Size
                            </Typography>
                            <Typography variant="body2" component="div">
                                {session.viewportSize}
                            </Typography>
                        </Box>
                    )}

                    {session.cookiesHash && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Cookies hash
                            </Typography>
                            <Typography variant="body2" component="div">
                                {session.cookiesHash}
                            </Typography>
                        </Box>
                    )}
                </Box>

                <Divider sx={{ my: 2 }} />

                <Typography variant="subtitle2" gutterBottom sx={{ mb: 2 }} component="div">
                    Session Timeline
                </Typography>

                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2, mb: 2 }}>
                    {session.startTime && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                Start Time
                            </Typography>
                            <Typography variant="body2" component="div">
                                {new Date(session.startTime).toLocaleString()}
                            </Typography>
                        </Box>
                    )}

                    {session.endTime && (
                        <Box>
                            <Typography variant="caption" color="text.secondary" display="block" component="div">
                                End Time
                            </Typography>
                            <Typography variant="body2" component="div">
                                {new Date(session.endTime).toLocaleString()}
                            </Typography>
                        </Box>
                    )}
                </Box>

                {session.userAgent && (
                    <>
                        <Divider sx={{ my: 2 }} />
                        <Typography variant="subtitle2" gutterBottom sx={{ mb: 2 }} component="div">
                            User Agent Details
                        </Typography>
                        <Paper variant="outlined" sx={{ p: 2, bgcolor: 'grey.50' }}>
                            <Typography variant="body2" sx={{
                                fontFamily: 'monospace',
                                fontSize: '0.8rem',
                                wordBreak: 'break-all'
                            }} component="div">
                                {session.userAgent}
                            </Typography>
                        </Paper>
                    </>
                )}
            </CardContent>
        </Card>
    );
};

export default SessionDetailsCard;