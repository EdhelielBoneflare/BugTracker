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
    Stack,
} from '@mui/material';
import { Report, ReportStatus, CriticalityLevel } from '../../types/types';

export interface EditReportData {
    status: ReportStatus;
    level: CriticalityLevel;
    comments: string;
    projectId?: string;
    developerName?: string;
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

                <Stack spacing={2} sx={{ mt: 2 }}>
                    {/* Project ID */}
                    <TextField
                        label="Project ID"
                        fullWidth
                        value={editData.projectId || ''}
                        onChange={(e) => onEditDataChange('projectId', e.target.value)}
                        size="small"
                        placeholder="Enter project ID"
                    />

                    {/* Developer Name */}
                    <TextField
                        label="Developer Name"
                        fullWidth
                        value={editData.developerName || ''}
                        onChange={(e) => onEditDataChange('developerName', e.target.value)}
                        size="small"
                        placeholder="Enter developer name"
                    />

                    {/* Status */}
                    <FormControl fullWidth size="small">
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

                    {/* Criticality */}
                    <FormControl fullWidth size="small">
                        <InputLabel>Criticality</InputLabel>
                        <Select
                            value={editData.level}
                            onChange={(e) => onEditDataChange('level', e.target.value as CriticalityLevel)}
                            label="Criticality"
                        >
                            <MenuItem value={CriticalityLevel.LOW}>Low</MenuItem>
                            <MenuItem value={CriticalityLevel.MEDIUM}>Medium</MenuItem>
                            <MenuItem value={CriticalityLevel.HIGH}>High</MenuItem>
                            <MenuItem value={CriticalityLevel.CRITICAL}>Critical</MenuItem>
                            <MenuItem value={CriticalityLevel.UNKNOWN}>Unknown</MenuItem>
                        </Select>
                    </FormControl>

                    {/* Comments */}
                    <TextField
                        label="Comments"
                        fullWidth
                        multiline
                        rows={4}
                        value={editData.comments}
                        onChange={(e) => onEditDataChange('comments', e.target.value)}
                        size="small"
                    />
                </Stack>

                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 2 }}>
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