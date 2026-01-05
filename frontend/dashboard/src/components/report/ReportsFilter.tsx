import React from 'react';
import {
    Paper,
    Box,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Button,
} from '@mui/material';
import { FilterList } from '@mui/icons-material';
import { ReportStatus, CriticalityLevel } from '../../types/types';

interface ReportsFilterProps {
    filterStatus: string;
    filterCriticality: string;
    onStatusChange: (status: string) => void;
    onCriticalityChange: (criticality: string) => void;
    onApplyFilters: () => void;
    onResetFilters: () => void;
}

const ReportsFilter: React.FC<ReportsFilterProps> = ({
                                                         filterStatus,
                                                         filterCriticality,
                                                         onStatusChange,
                                                         onCriticalityChange,
                                                         onApplyFilters,
                                                         onResetFilters,
                                                     }) => {
    return (
        <Paper sx={{ p: 2, mb: 3 }}>
            <Box sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 1fr 1fr' },
                gap: 2,
                alignItems: 'center'
            }}>
                <FormControl fullWidth size="small">
                    <InputLabel>Status</InputLabel>
                    <Select
                        value={filterStatus}
                        onChange={(e) => onStatusChange(e.target.value)}
                        label="Status"
                    >
                        <MenuItem value="ALL">All Statuses</MenuItem>
                        <MenuItem value={ReportStatus.NEW}>New</MenuItem>
                        <MenuItem value={ReportStatus.IN_PROGRESS}>In Progress</MenuItem>
                        <MenuItem value={ReportStatus.DONE}>Done</MenuItem>
                    </Select>
                </FormControl>
                <FormControl fullWidth size="small">
                    <InputLabel>Criticality</InputLabel>
                    <Select
                        value={filterCriticality}
                        onChange={(e) => onCriticalityChange(e.target.value)}
                        label="Criticality"
                    >
                        <MenuItem value="ALL">All Criticalities</MenuItem>
                        <MenuItem value={CriticalityLevel.LOW}>Low</MenuItem>
                        <MenuItem value={CriticalityLevel.MEDIUM}>Medium</MenuItem>
                        <MenuItem value={CriticalityLevel.HIGH}>High</MenuItem>
                        <MenuItem value={CriticalityLevel.CRITICAL}>Critical</MenuItem>
                        <MenuItem value={CriticalityLevel.UNKNOWN}>Unknown</MenuItem>
                    </Select>
                </FormControl>
                <Button
                    variant="contained"
                    onClick={onApplyFilters}
                    startIcon={<FilterList />}
                    fullWidth
                >
                    Apply Filters
                </Button>
                <Button
                    variant="outlined"
                    onClick={onResetFilters}
                    fullWidth
                >
                    Reset Filters
                </Button>
            </Box>
        </Paper>
    );
};

export default ReportsFilter;