import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    Box,
    Typography,
    Card,
    CardContent,
    CircularProgress,
    Alert,
    IconButton,
} from '@mui/material';
import {
    ArrowBack,
    Error as ErrorIcon,
    Warning,
    CheckCircle,
    Circle,
    BugReport as BugReportIcon,
} from '@mui/icons-material';
import { api } from '../api/Api';
import { Report, ReportStatus, CriticalityLevel } from '../types/types';
import { useAuth } from '../contexts/AuthContext';
import ReportsFilter from '../components/report/ReportsFilter';
import ReportsStats from '../components/report/ReportsStats';
import ReportsTable from '../components/report/ReportsTable';
import EditReportDialog from '../components/report/EditReportDialog';

const ProjectReportsPage: React.FC = () => {
    const { projectId } = useParams<{ projectId: string }>();
    const navigate = useNavigate();
    const { isAdmin } = useAuth();
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [filterStatus, setFilterStatus] = useState<string>('ALL');
    const [filterCriticality, setFilterCriticality] = useState<string>('ALL');
    const [openEditDialog, setOpenEditDialog] = useState(false);
    const [editingReport, setEditingReport] = useState<Report | null>(null);
    const [editData, setEditData] = useState({
        status: ReportStatus.NEW,
        criticality: CriticalityLevel.UNKNOWN,
        comments: '',
    });

    // Функция проверки прав - только админ может редактировать/удалять
    const canEditReport = () => {
        return isAdmin();
    };

    useEffect(() => {
        if (projectId) {
            fetchReports();
        }
    }, [projectId]);

    const fetchReports = async () => {
        try {
            setLoading(true);
            const response = await api.getProjectReports(projectId!, 0, 50);

            let filteredReports = response.content.map((report: any) => ({
                ...report,
                criticality: report.level || CriticalityLevel.UNKNOWN,
                comments: report.comments || ''
            }));

            // Фильтрация на клиенте
            if (filterStatus !== 'ALL') {
                filteredReports = filteredReports.filter(report =>
                    report.status === filterStatus
                );
            }

            if (filterCriticality !== 'ALL') {
                filteredReports = filteredReports.filter(report =>
                    report.criticality === filterCriticality
                );
            }

            setReports(filteredReports);
            setError(null);
        } catch (err: any) {
            setError(err.message || 'Failed to load reports');
        } finally {
            setLoading(false);
        }
    };

    const getStatusIcon = (status: ReportStatus) => {
        switch(status) {
            case ReportStatus.NEW: return <ErrorIcon color="error" fontSize="small" />;
            case ReportStatus.IN_PROGRESS: return <Warning color="warning" fontSize="small" />;
            case ReportStatus.DONE: return <CheckCircle color="success" fontSize="small" />;
            default: return <Circle fontSize="small" />;
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

    const getCriticalityLabel = (criticality: CriticalityLevel | undefined | null) => {
        if (!criticality || criticality === CriticalityLevel.UNKNOWN) return 'Unknown';

        switch(criticality) {
            case CriticalityLevel.CRITICAL: return 'Critical';
            case CriticalityLevel.HIGH: return 'High';
            case CriticalityLevel.MEDIUM: return 'Medium';
            case CriticalityLevel.LOW: return 'Low';
            default: return String(criticality);
        }
    };

    const getCriticalityColor = (criticality: CriticalityLevel | undefined | null) => {
        if (!criticality || criticality === CriticalityLevel.UNKNOWN) return 'default';

        switch(criticality) {
            case CriticalityLevel.CRITICAL: return 'error';
            case CriticalityLevel.HIGH: return 'error';
            case CriticalityLevel.MEDIUM: return 'warning';
            case CriticalityLevel.LOW: return 'info';
            default: return 'default';
        }
    };

    const handleDeleteReport = async (reportId: number) => {
        if (!window.confirm('Are you sure you want to delete this report? This action cannot be undone.')) return;

        try {
            await api.deleteReport(reportId);
            fetchReports();
        } catch (err: any) {
            setError(err.message || 'Failed to delete report');
        }
    };

    const handleViewDetails = (reportId: number) => {
        navigate(`/reports/${reportId}`);
    };

    const handleEditClick = (report: Report) => {
        setEditingReport(report);
        setEditData({
            status: report.status,
            criticality: report.criticality || CriticalityLevel.UNKNOWN,
            comments: report.comments || '',
        });
        setOpenEditDialog(true);
    };

    const handleUpdateReport = async () => {
        if (!editingReport) return;

        try {
            await api.updateReportDashboard(editingReport.id, {
                status: editData.status,
                criticality: editData.criticality,
                comments: editData.comments,
            });
            setOpenEditDialog(false);
            setEditingReport(null);
            fetchReports();
        } catch (err: any) {
            setError(err.message || 'Failed to update report');
        }
    };

    const handleFilterChange = () => {
        fetchReports();
    };

    const handleResetFilters = () => {
        setFilterStatus('ALL');
        setFilterCriticality('ALL');
        fetchReports();
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

    return (
        <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <IconButton onClick={() => navigate('/dashboard')} sx={{ mr: 2 }}>
                    <ArrowBack />
                </IconButton>
                <Typography variant="h4">
                    Project Reports
                </Typography>
            </Box>

            {error && (
                <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
                    {error}
                </Alert>
            )}

            {/* Фильтры */}
            <ReportsFilter
                filterStatus={filterStatus}
                filterCriticality={filterCriticality}
                onStatusChange={setFilterStatus}
                onCriticalityChange={setFilterCriticality}
                onApplyFilters={handleFilterChange}
                onResetFilters={handleResetFilters}
            />

            {/* Статистика */}
            <ReportsStats reports={reports} />

            {reports.length === 0 ? (
                <Card>
                    <CardContent sx={{ textAlign: 'center', py: 4 }}>
                        <BugReportIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
                        <Typography variant="h6" color="text.secondary">
                            No reports found for this project
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                            Try changing your filters
                        </Typography>
                    </CardContent>
                </Card>
            ) : (
                <ReportsTable
                    reports={reports}
                    canEditReport={canEditReport()}
                    onViewDetails={handleViewDetails}
                    onEditClick={handleEditClick}
                    onDeleteReport={handleDeleteReport}
                    getStatusIcon={getStatusIcon}
                    getStatusColor={getStatusColor}
                    getCriticalityLabel={getCriticalityLabel}
                    getCriticalityColor={getCriticalityColor}
                />
            )}

            {/* Диалог редактирования репорта - только для админа */}
            {canEditReport() && (
                <EditReportDialog
                    open={openEditDialog}
                    editingReport={editingReport}
                    editData={editData}
                    onClose={() => setOpenEditDialog(false)}
                    onUpdate={handleUpdateReport}
                    onEditDataChange={handleEditDataChange}
                />
            )}
        </Box>
    );
};

export default ProjectReportsPage;