import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Box,
    CircularProgress,
    Alert,
    Stack,
} from '@mui/material';
import { api } from '../api/Api';
import { Report, Session, Event, ReportStatus, CriticalityLevel } from '../types/types';
import { useAuth } from '../contexts/AuthContext';
import ReportHeader from '../components/report/ReportHeader';
import ReportDetailsCard from '../components/report/ReportDetailsCard';
import SessionDetailsCard from '../components/report/SessionDetailsCard';
import EventsTable from '../components/report/EventsTable';

const ReportDetailsPage: React.FC = () => {
    const { reportId } = useParams<{ reportId: string }>();
    const navigate = useNavigate();
    const { isAdmin } = useAuth();
    const [report, setReport] = useState<Report | null>(null);
    const [session, setSession] = useState<Session | null>(null);
    const [events, setEvents] = useState<Event[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [editing, setEditing] = useState(false);
    const [editData, setEditData] = useState({
        status: ReportStatus.NEW,
        criticality: CriticalityLevel.UNKNOWN,
        comments: '',
        projectId: '', // Добавлено
        developerName: '', // Добавлено
    });

    const canEditReport = () => {
        return isAdmin();
    };

    const normalizeCriticality = (data: any): CriticalityLevel => {
        if (data.criticality && Object.values(CriticalityLevel).includes(data.criticality)) {
            return data.criticality;
        }
        if (data.level && Object.values(CriticalityLevel).includes(data.level)) {
            return data.level;
        }
        return CriticalityLevel.UNKNOWN;
    };

    useEffect(() => {
        if (reportId) {
            fetchData();
        }
    }, [reportId]);

    const fetchData = async () => {
        try {
            setLoading(true);
            const reportData = await api.getReport(parseInt(reportId!));

            const processedReport = {
                ...reportData,
                criticality: normalizeCriticality(reportData),
                developerName: reportData.developerName || (reportData as any)?.developerName || null,
                status: reportData.status || ReportStatus.NEW,
                comments: reportData.comments || '',
                userEmail: reportData.userEmail || (reportData as any)?.userEmail || null,
                userProvided: Boolean(reportData.userProvided || (reportData as any)?.userProvided || false),
                sessionId: reportData.sessionId || (reportData as any)?.sessionId || null,
                currentUrl: reportData.currentUrl || (reportData as any)?.currentUrl || '',
                screen: reportData.screen || (reportData as any)?.screen || '',
                tags: reportData.tags || (reportData as any)?.tags || [],
            };

            console.log('📊 Report data:', processedReport);

            setReport(processedReport as Report);

            setEditData({
                status: processedReport.status,
                criticality: normalizeCriticality(processedReport),
                comments: processedReport.comments,
                projectId: processedReport.projectId || '', // Добавлено
                developerName: processedReport.developerName || '', // Добавлено
            });

            if (processedReport.sessionId) {
                try {
                    const sessionData = await api.getSession(processedReport.sessionId.toString());

                    const sessionPlugins = (sessionData as any).plugins;
                    let plugins: string[] = [];

                    if (sessionPlugins) {
                        plugins = sessionPlugins.filter((p: any) => typeof p === 'string' && p.trim() !== '');
                    }

                    const processedSession = {
                        ...sessionData,
                        plugins: plugins
                    };

                    setSession(processedSession as Session);

                    const eventsData = await api.getSessionEvents(processedReport.sessionId.toString());

                    const processedEvents = eventsData.map((event: any) => ({
                        ...event,
                        eventId: event.eventId || event.id,
                        fileName: event.fileName || event.metadata?.fileName || '',
                        lineNumber: event.lineNumber || event.metadata?.lineNumber || '',
                        statusCode: event.statusCode || event.metadata?.statusCode || '',
                        element: event.element || event.metadata?.element || '',
                        log: event.log || '',
                        stackTrace: event.stackTrace || '',
                        metadata: event.metadata || null
                    }));

                    setEvents(processedEvents);
                } catch (err) {
                    console.warn('Failed to load session/events:', err);
                    setEvents([]);
                }
            } else {
                setEvents([]);
            }
            setError(null);
        } catch (err: any) {
            setError(err.message || 'Failed to load report details');
        } finally {
            setLoading(false);
        }
    };

    const getCriticalityColor = (criticality: CriticalityLevel) => {
        switch(criticality) {
            case CriticalityLevel.CRITICAL: return 'error';
            case CriticalityLevel.HIGH: return 'error';
            case CriticalityLevel.MEDIUM: return 'warning';
            case CriticalityLevel.LOW: return 'info';
            default: return 'default';
        }
    };

    const getCriticalityLabel = (criticality: CriticalityLevel) => {
        switch(criticality) {
            case CriticalityLevel.CRITICAL: return 'Critical';
            case CriticalityLevel.HIGH: return 'High';
            case CriticalityLevel.MEDIUM: return 'Medium';
            case CriticalityLevel.LOW: return 'Low';
            default: return String(criticality);
        }
    };

    const getStatusColor = (status: ReportStatus) => {
        switch(status) {
            case ReportStatus.NEW: return 'error';
            case ReportStatus.IN_PROGRESS: return 'warning';
            case ReportStatus.DONE: return 'success';
            default: return 'default';
        }
    };

    const handleSave = async () => {
        if (!report) return;

        try {
            const updatedReport = await api.updateReportDashboard(report.id, {
                status: editData.status,
                criticality: editData.criticality,
                comments: editData.comments,
                projectId: editData.projectId || undefined, // Добавлено
                developerName: editData.developerName || undefined, // Добавлено
            });

            const processedReport = {
                ...updatedReport,
                criticality: normalizeCriticality(updatedReport),
            };

            setReport(processedReport as Report);
            setEditing(false);
            setError(null);
        } catch (err: any) {
            setError(err.message || 'Failed to update report');
        }
    };

    const handleCancel = () => {
        if (!report) return;
        setEditData({
            status: report.status || ReportStatus.NEW,
            criticality: normalizeCriticality(report),
            comments: report.comments || '',
            projectId: report.projectId || '', // Добавлено
            developerName: report.developerName || '', // Добавлено
        });
        setEditing(false);
    };

    const handleEditDataChange = (field: keyof typeof editData, value: any) => {
        setEditData(prev => ({ ...prev, [field]: value }));
    };

    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    if (!report) {
        return (
            <Box>
                <Alert severity="error">
                    Report not found
                </Alert>
                <Box
                    onClick={() => navigate(-1)}
                    sx={{ mt: 2 }}
                >
                    Go Back
                </Box>
            </Box>
        );
    }

    return (
        <Box>
            <ReportHeader
                report={report}
                editing={editing}
                canEditReport={canEditReport()}
                onBack={() => navigate(-1)}
                onEdit={() => setEditing(true)}
                onSave={handleSave}
                onCancel={handleCancel}
            />

            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            <Stack spacing={3}>
                {/* Report Details */}
                <ReportDetailsCard
                    report={report}
                    editing={editing}
                    editData={editData}
                    onEditDataChange={handleEditDataChange}
                    getCriticalityLabel={getCriticalityLabel}
                    getCriticalityColor={getCriticalityColor}
                    getStatusColor={getStatusColor}
                />

                {/* Session Details */}
                {report.sessionId && session && (
                    <SessionDetailsCard
                        session={session}
                        getDeviceIcon={(deviceType?: string) => {
                            if (!deviceType) return 'DesktopWindows';
                            const typeUpper = deviceType.toUpperCase();
                            if (typeUpper.includes('MOBILE') || typeUpper.includes('PHONE')) return 'Smartphone';
                            if (typeUpper.includes('TABLET')) return 'Tablet';
                            return 'DesktopWindows';
                        }}
                    />
                )}

                {/* Events List */}
                <EventsTable events={events} />
            </Stack>
        </Box>
    );
};

export default ReportDetailsPage;