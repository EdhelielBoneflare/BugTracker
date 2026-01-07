import React from 'react';
import {
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Typography,
    Chip,
    Stack,
    Button,
    Tooltip,
} from '@mui/material';
import {
    Visibility,
} from '@mui/icons-material';
import { Report, ReportStatus, CriticalityLevel } from '../../types/types';

interface ReportsTableProps {
    reports: Report[];
    onViewDetails: (reportId: number) => void;
    onEditClick: (report: Report) => void;
    onDeleteReport: (reportId: number) => void;
    getStatusIcon: (status: ReportStatus) => React.ReactElement; // Исправляем тип
    getStatusColor: (status: ReportStatus) => string;
    getCriticalityLabel: (level: CriticalityLevel | undefined | null) => string;
    getCriticalityColor: (level: CriticalityLevel | undefined | null) => string;
}

const ReportsTable: React.FC<ReportsTableProps> = ({
                                                       reports,
                                                       onViewDetails,
                                                       onEditClick,
                                                       onDeleteReport,
                                                       getStatusIcon,
                                                       getStatusColor,
                                                       getCriticalityLabel,
                                                       getCriticalityColor,
                                                   }) => {
    return (
        <TableContainer component={Paper}>
            <Table>
                <TableHead>
                    <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>Title</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Criticality</TableCell>
                        <TableCell>Reported</TableCell>
                        <TableCell>Actions</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {reports.map((report) => (
                        <TableRow key={report.id} hover>
                            <TableCell>#{report.id.toString().substring(0, 8)}</TableCell>
                            <TableCell>
                                <Typography variant="body2" fontWeight="medium">
                                    {report.title}
                                </Typography>
                                {report.comments && (
                                    <Typography variant="caption" color="text.secondary" display="block">
                                        {report.comments.substring(0, 50)}
                                    </Typography>
                                )}
                            </TableCell>
                            <TableCell>
                                <Tooltip title={report.status}>
                                    <Chip
                                        icon={getStatusIcon(report.status)}
                                        label={report.status}
                                        size="small"
                                        color={getStatusColor(report.status) as any}
                                        variant="outlined"
                                    />
                                </Tooltip>
                            </TableCell>
                            <TableCell>
                                <Tooltip title={getCriticalityLabel(report.level)}>
                                    <Chip
                                        label={getCriticalityLabel(report.level)}
                                        size="small"
                                        color={getCriticalityColor(report.level) as any}
                                        variant="outlined"
                                    />
                                </Tooltip>
                            </TableCell>
                            <TableCell>
                                <Typography variant="caption" display="block">
                                    {new Date(report.reportedAt).toLocaleDateString()}
                                </Typography>
                                <Typography variant="caption" color="text.secondary" display="block">
                                    {new Date(report.reportedAt).toLocaleTimeString()}
                                </Typography>
                            </TableCell>
                            <TableCell>
                                <Stack direction="row" spacing={1}>
                                    <Button
                                        size="small"
                                        variant="outlined"
                                        startIcon={<Visibility />}
                                        onClick={() => onViewDetails(report.id)}
                                    >
                                        View
                                    </Button>

                                    {(
                                        <>
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                color="primary"
                                                onClick={() => onEditClick(report)}
                                            >
                                                Edit
                                            </Button>
                                            <Button
                                                size="small"
                                                variant="outlined"
                                                color="error"
                                                onClick={() => onDeleteReport(report.id)}
                                            >
                                                Delete
                                            </Button>
                                        </>
                                    )}
                                </Stack>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </TableContainer>
    );
};

export default ReportsTable;