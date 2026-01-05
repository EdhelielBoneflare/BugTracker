import React from 'react';
import {
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Typography,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    TextField,
    Button,
} from '@mui/material';
import { Report, ReportStatus, CriticalityLevel } from '../../types/types';

export interface EditReportData {
    status: ReportStatus;
    criticality: CriticalityLevel;
    comments: string;
}

interface EditReportDialogProps {
    open: boolean;
    editingReport: Report | null;
    editData: EditReportData;
    onClose: () => void;
    onUpdate: () => void;
    onEditDataChange: (field: keyof EditReportData, value: any) => void;
}

const EditReportDialog: React.FC<EditReportDialogProps> = ({
                                                               open,
                                                               editingReport,
                                                               editData,
                                                               onClose,
                                                               onUpdate,
                                                               onEditDataChange,
                                                           }) => {
    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>
                Edit Report #{editingReport?.id.toString().substring(0, 8)}...
            </DialogTitle>
            <DialogContent>
                <Typography variant="subtitle1" gutterBottom>
                    {editingReport?.title}
                </Typography>

                <FormControl fullWidth sx={{ mb: 2, mt: 2 }}>
                    <InputLabel>Status</InputLabel>
                    <Select
                        value={editData.status}
                        onChange={(e) => onEditDataChange('status', e.target.value as ReportStatus)}
                        label="Status"
                    >
                        <MenuItem value={ReportStatus.NEW}>New</MenuItem>
                        <MenuItem value={ReportStatus.IN_PROGRESS}>In Progress</MenuItem>
                        <MenuItem value={ReportStatus.DONE}>Done</MenuItem>
                    </Select>
                </FormControl>

                <FormControl fullWidth sx={{ mb: 2 }}>
                    <InputLabel>Criticality</InputLabel>
                    <Select
                        value={editData.criticality}
                        onChange={(e) => onEditDataChange('criticality', e.target.value as CriticalityLevel)}
                        label="Criticality"
                    >
                        <MenuItem value={CriticalityLevel.LOW}>Low</MenuItem>
                        <MenuItem value={CriticalityLevel.MEDIUM}>Medium</MenuItem>
                        <MenuItem value={CriticalityLevel.HIGH}>High</MenuItem>
                        <MenuItem value={CriticalityLevel.CRITICAL}>Critical</MenuItem>
                        <MenuItem value={CriticalityLevel.UNKNOWN}>Unknown</MenuItem>
                    </Select>
                </FormControl>

                <TextField
                    label="Comments"
                    fullWidth
                    multiline
                    rows={4}
                    value={editData.comments}
                    onChange={(e) => onEditDataChange('comments', e.target.value)}
                    sx={{ mb: 2 }}
                />

                <Typography variant="caption" color="text.secondary" display="block">
                    Reported: {editingReport && new Date(editingReport.reportedAt).toLocaleString()}
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancel</Button>
                <Button
                    onClick={onUpdate}
                    variant="contained"
                >
                    Update Report
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default EditReportDialog;