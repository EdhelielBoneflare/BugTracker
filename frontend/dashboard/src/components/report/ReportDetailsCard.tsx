import React from 'react';
import {
    Card,
    CardContent,
    Box,
    Typography,
    Chip,
    Stack,
    Paper,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
} from '@mui/material';
import {
    Person,
    Email,
    Web,
    Schedule,
} from '@mui/icons-material';
import { Report, ReportStatus, CriticalityLevel } from '../../types/types';

export interface ReportEditData {
    status: ReportStatus;
    criticality: CriticalityLevel;
    comments: string;
    projectId?: string; // Добавляем
    developerName?: string; // Добавляем
}

interface ReportDetailsCardProps {
    report: Report;
    editing: boolean;
    editData: ReportEditData;
    onEditDataChange: (field: keyof ReportEditData, value: any) => void;
    getCriticalityLabel: (criticality: CriticalityLevel) => string;
    getCriticalityColor: (criticality: CriticalityLevel) => string;
    getStatusColor: (status: ReportStatus) => string;
}

const ReportDetailsCard: React.FC<ReportDetailsCardProps> = ({
                                                                 report,
                                                                 editing,
                                                                 editData,
                                                                 onEditDataChange,
                                                                 getCriticalityLabel,
                                                                 getCriticalityColor,
                                                                 getStatusColor,
                                                             }) => {
    return (
        <Card>
            <CardContent>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 3 }}>
                    <Typography variant="h5" component="div">
                        {report.title}
                    </Typography>
                    <Stack direction="row" spacing={1}>
                        {editing ? (
                            <>
                                <FormControl size="small" sx={{ minWidth: 120 }}>
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
                                <FormControl size="small" sx={{ minWidth: 120 }}>
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
                            </>
                        ) : (
                            <>
                                <Chip
                                    label={getCriticalityLabel(report.criticality)}
                                    color={getCriticalityColor(report.criticality) as any}
                                    size="medium"
                                    variant="outlined"
                                />
                                <Chip
                                    label={report.status}
                                    color={getStatusColor(report.status) as any}
                                    size="medium"
                                    variant="outlined"
                                />
                            </>
                        )}
                    </Stack>
                </Box>

                {editing ? (
                    <>
                        <Stack spacing={2} sx={{ mb: 2 }}>
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
                    </>
                ) : report.comments ? (
                    <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: 'grey.50' }}>
                        <Typography variant="subtitle2" color="text.secondary" gutterBottom component="div">
                            Comments:
                        </Typography>
                        <Typography variant="body1" component="div">
                            {report.comments}
                        </Typography>
                    </Paper>
                ) : null}

                <Box sx={{
                    display: 'grid',
                    gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                    gap: 2,
                    mb: 2
                }}>
                    {report.projectId && (
                        <Box>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <strong>Project ID:</strong> {report.projectId}
                            </Typography>
                        </Box>
                    )}

                    {report.developerName && (
                        <Box>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <Person fontSize="small" />
                                <strong>Developer name:</strong> {report.developerName}
                            </Typography>
                        </Box>
                    )}

                    {report.tags && report.tags.length > 0 && (
                        <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <strong>Tags:</strong>
                                <Stack direction="row" spacing={0.5} sx={{ ml: 1, flexWrap: 'wrap', gap: 0.5 }}>
                                    {report.tags.map((tag, index) => (
                                        <Chip
                                            key={index}
                                            label={tag}
                                            size="small"
                                            variant="outlined"
                                            color="primary"
                                        />
                                    ))}
                                </Stack>
                            </Typography>
                        </Box>
                    )}

                    {report.sessionId && (
                        <Box>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <strong>Session ID:</strong> {report.sessionId}
                            </Typography>
                        </Box>
                    )}

                    {report.userEmail && (
                        <Box>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <Email fontSize="small" />
                                <strong>User Email:</strong> {report.userEmail}
                            </Typography>
                        </Box>
                    )}

                    <Box>
                        <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                            <strong>User Provided:</strong>
                            <Chip
                                label={report.userProvided ? "Yes" : "No"}
                                size="small"
                                color={report.userProvided ? "primary" : "default"}
                                sx={{ ml: 1 }}
                            />
                        </Typography>
                    </Box>

                    {report.currentUrl && (
                        <Box>
                            <Typography variant="body2" display="flex" alignItems="flex-start" gap={1} component="div">
                                <Web fontSize="small" sx={{ mt: 0.5 }} />
                                <Box>
                                    <Typography variant="body2" fontWeight="medium">URL:</Typography>
                                    <Typography
                                        variant="body2"
                                        sx={{
                                            wordBreak: 'break-all',
                                            overflowWrap: 'break-word',
                                            whiteSpace: 'pre-wrap'
                                        }}
                                    >
                                        {report.currentUrl}
                                    </Typography>
                                </Box>
                            </Typography>
                        </Box>
                    )}

                    {report.screen && (
                        <Box sx={{ gridColumn: 'span 2' }}>
                            <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                                <strong>Screen Info:</strong> {report.screen}
                            </Typography>
                        </Box>
                    )}

                    <Box>
                        <Typography variant="body2" display="flex" alignItems="center" gap={1} component="div">
                            <Schedule fontSize="small" />
                            <strong>Reported:</strong> {new Date(report.reportedAt).toLocaleString()}
                        </Typography>
                    </Box>
                </Box>
            </CardContent>
        </Card>
    );
};

export default ReportDetailsCard;