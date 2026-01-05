import React from 'react';
import {
    Box,
    Paper,
    Typography,
} from '@mui/material';
import { Report, ReportStatus } from '../../types/types';

interface ReportsStatsProps {
    reports: Report[];
}

const ReportsStats: React.FC<ReportsStatsProps> = ({ reports }) => {
    return (
        <Box sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr 1fr', sm: '1fr 1fr 1fr 1fr' },
            gap: 2,
            mb: 3
        }}>
            <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6">{reports.length}</Typography>
                <Typography variant="body2" color="text.secondary">Total Reports</Typography>
            </Paper>
            <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color="error">
                    {reports.filter(r => r.status === ReportStatus.NEW).length}
                </Typography>
                <Typography variant="body2" color="text.secondary">New</Typography>
            </Paper>
            <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color="warning">
                    {reports.filter(r => r.status === ReportStatus.IN_PROGRESS).length}
                </Typography>
                <Typography variant="body2" color="text.secondary">In Progress</Typography>
            </Paper>
            <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Typography variant="h6" color="success">
                    {reports.filter(r => r.status === ReportStatus.DONE).length}
                </Typography>
                <Typography variant="body2" color="text.secondary">Done</Typography>
            </Paper>
        </Box>
    );
};

export default ReportsStats;