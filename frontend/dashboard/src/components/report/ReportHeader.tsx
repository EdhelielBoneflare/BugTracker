import React from 'react';
import {
    Box,
    Stack,
    Button,
    Typography,
} from '@mui/material';
import {
    ArrowBack,
    Edit,
    Save,
    Cancel,
} from '@mui/icons-material';
import { Report } from '../../types/types';

interface ReportHeaderProps {
    report: Report;
    editing: boolean;
    canEditReport: boolean;
    onBack: () => void;
    onEdit: () => void;
    onSave: () => void;
    onCancel: () => void;
}

const ReportHeader: React.FC<ReportHeaderProps> = ({
                                                       report,
                                                       editing,
                                                       canEditReport,
                                                       onBack,
                                                       onEdit,
                                                       onSave,
                                                       onCancel,
                                                   }) => {
    return (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
            <Stack direction="row" spacing={2} alignItems="center">
                <Button
                    startIcon={<ArrowBack />}
                    onClick={onBack}
                >
                    Back
                </Button>
                <Typography variant="h4" component="div">
                    Report #{report.id.toString().substring(0, 8)}...
                </Typography>
            </Stack>

            {canEditReport && (
                <Stack direction="row" spacing={1}>
                    {!editing ? (
                        <Button
                            startIcon={<Edit />}
                            variant="contained"
                            onClick={onEdit}
                        >
                            Edit
                        </Button>
                    ) : (
                        <>
                            <Button
                                startIcon={<Save />}
                                variant="contained"
                                color="success"
                                onClick={onSave}
                            >
                                Save
                            </Button>
                            <Button
                                startIcon={<Cancel />}
                                variant="outlined"
                                onClick={onCancel}
                            >
                                Cancel
                            </Button>
                        </>
                    )}
                </Stack>
            )}
        </Box>
    );
};

export default ReportHeader;