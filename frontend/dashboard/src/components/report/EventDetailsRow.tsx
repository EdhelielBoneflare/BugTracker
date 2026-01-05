import React, { useState } from 'react';
import {
    TableRow,
    TableCell,
    Collapse,
    Box,
    Typography,
    Chip,
    Paper,
    IconButton,
} from '@mui/material';
import {
    KeyboardArrowDown,
    KeyboardArrowUp,
    Code,
    Description,
    Link,
    Numbers,
} from '@mui/icons-material';
import { Event } from '../../types/types';

interface EventDetailsRowProps {
    event: Event;
}

const EventDetailsRow: React.FC<EventDetailsRowProps> = ({ event }) => {
    const [open, setOpen] = useState(false);

    console.log('Event data structure:', event);
    console.log('Event log:', event.log);
    console.log('Event stackTrace:', event.stackTrace);
    console.log('Event metadata:', (event as any).metadata);

    const eventData = {
        id: event.id,
        eventId: event.eventId,
        type: event.type,
        name: event.name,
        timestamp: event.timestamp,
        url: event.url,
        log: event.log || '',
        stackTrace: event.stackTrace || '',
        fileName: event.fileName || (event as any).metadata?.fileName || '',
        lineNumber: event.lineNumber || (event as any).metadata?.lineNumber || '',
        statusCode: event.statusCode || (event as any).metadata?.statusCode || '',
        element: event.element || (event as any).metadata?.element || '',
    };

    console.log('Processed eventData:', eventData);

    const hasLogContent = Boolean(eventData.log && eventData.log.trim() !== '');
    const hasStackTraceContent = Boolean(eventData.stackTrace && eventData.stackTrace.trim() !== '');
    const hasFileName = Boolean(eventData.fileName && eventData.fileName.trim() !== '');
    const hasLineNumber = Boolean(eventData.lineNumber && eventData.lineNumber.trim() !== '');
    const hasStatusCode = Boolean(eventData.statusCode && eventData.statusCode.trim() !== '');
    const hasElement = Boolean(eventData.element && eventData.element.trim() !== '');
    const hasUrl = Boolean(eventData.url && eventData.url.trim() !== '');
    const hasMetadata = Boolean((event as any).metadata && Object.keys((event as any).metadata).length > 0);
    const hasEventId = Boolean(eventData.eventId);

    console.log('Has log content:', hasLogContent, eventData.log);
    console.log('Has stack trace:', hasStackTraceContent, eventData.stackTrace);

    const getEventColor = (type?: string) => {
        if (!type) return 'default';
        const typeUpper = type.toUpperCase();
        if (typeUpper.includes('ERROR') || typeUpper.includes('EXCEPTION')) return 'error';
        if (typeUpper.includes('WARNING') || typeUpper.includes('PERFORMANCE')) return 'warning';
        if (typeUpper.includes('INFO') || typeUpper.includes('ACTION') || typeUpper.includes('NETWORK')) return 'info';
        return 'default';
    };

    const getStatusCodeColor = (statusCode?: string): 'error' | 'warning' | 'success' | 'default' => {
        if (!statusCode) return 'default';
        const code = parseInt(statusCode);
        if (isNaN(code)) return 'default';
        if (code >= 400) return 'error';
        if (code >= 300) return 'warning';
        return 'success';
    };

    const hasAdditionalInfo =
        hasLogContent ||
        hasStackTraceContent ||
        hasFileName ||
        hasLineNumber ||
        hasStatusCode ||
        hasElement ||
        hasUrl ||
        hasMetadata ||
        hasEventId;

    const renderContent = (content: string, isStackTrace: boolean = false) => {
        if (!content || content.trim() === '') return null;

        return (
            <pre style={{
                margin: 0,
                fontSize: isStackTrace ? '11px' : '12px',
                overflow: 'auto',
                whiteSpace: 'pre-wrap',
                fontFamily: 'monospace',
                color: isStackTrace ? '#d32f2f' : 'inherit',
                maxHeight: isStackTrace ? '300px' : '200px'
            }}>
                {content}
            </pre>
        );
    };

    return (
        <>
            <TableRow sx={{ '& > *': { borderBottom: 'unset' } }}>
                <TableCell>
                    <IconButton
                        aria-label="expand row"
                        size="small"
                        onClick={() => setOpen(!open)}
                        disabled={!hasAdditionalInfo}
                    >
                        {open ? <KeyboardArrowUp /> : <KeyboardArrowDown />}
                    </IconButton>
                </TableCell>
                <TableCell>
                    <Chip
                        label={eventData.type || 'Unknown'}
                        size="small"
                        color={getEventColor(eventData.type) as any}
                        variant="outlined"
                    />
                </TableCell>
                <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                        {eventData.name || 'Unnamed Event'}
                    </Typography>
                </TableCell>
                <TableCell>
                    {new Date(eventData.timestamp).toLocaleString()}
                </TableCell>
                <TableCell>
                    {eventData.url ? (
                        <Typography variant="caption" noWrap sx={{ maxWidth: '200px', display: 'block' }}>
                            {eventData.url}
                        </Typography>
                    ) : 'N/A'}
                </TableCell>
            </TableRow>
            <TableRow>
                <TableCell style={{ paddingBottom: 0, paddingTop: 0 }} colSpan={5}>
                    <Collapse in={open} timeout="auto" unmountOnExit>
                        <Box sx={{ margin: 2 }}>
                            {hasAdditionalInfo ? (
                                <>
                                    <Typography variant="subtitle2" gutterBottom>
                                        Detailed Event Information
                                    </Typography>

                                    {(hasElement || hasFileName || hasLineNumber || hasStatusCode || hasUrl || hasEventId) && (
                                        <Box sx={{
                                            display: 'grid',
                                            gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: '1fr 1fr 1fr' },
                                            gap: 2,
                                            mb: 3
                                        }}>
                                            {hasEventId && (
                                                <Box>
                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                        Event ID
                                                    </Typography>
                                                    <Typography variant="body2" sx={{ fontFamily: 'monospace', fontWeight: 'bold' }}>
                                                        #{eventData.eventId}
                                                    </Typography>
                                                </Box>
                                            )}

                                            {hasElement && (
                                                <Box>
                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                        <Code fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
                                                        Element
                                                    </Typography>
                                                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                                                        {eventData.element}
                                                    </Typography>
                                                </Box>
                                            )}

                                            {hasFileName && (
                                                <Box>
                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                        <Description fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
                                                        File Name
                                                    </Typography>
                                                    <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                                                        {eventData.fileName}
                                                    </Typography>
                                                </Box>
                                            )}

                                            {hasLineNumber && (
                                                <Box>
                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                        <Numbers fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
                                                        Line Number
                                                    </Typography>
                                                    <Typography variant="body2">
                                                        {eventData.lineNumber}
                                                    </Typography>
                                                </Box>
                                            )}

                                            {hasStatusCode && (
                                                <Box>
                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                        Status Code
                                                    </Typography>
                                                    <Chip
                                                        label={eventData.statusCode}
                                                        size="small"
                                                        color={getStatusCodeColor(eventData.statusCode)}
                                                    />
                                                </Box>
                                            )}

                                            {hasUrl && (
                                                <Box sx={{ gridColumn: { xs: 'span 1', sm: 'span 2' } }}>
                                                    <Typography variant="caption" color="text.secondary" display="block">
                                                        <Link fontSize="small" sx={{ mr: 0.5, verticalAlign: 'middle' }} />
                                                        URL
                                                    </Typography>
                                                    <Typography variant="body2" sx={{
                                                        wordBreak: 'break-all',
                                                        fontFamily: 'monospace',
                                                        fontSize: '0.8rem'
                                                    }}>
                                                        {eventData.url}
                                                    </Typography>
                                                </Box>
                                            )}
                                        </Box>
                                    )}

                                    {/* Log Content */}
                                    {hasLogContent && (
                                        <>
                                            <Typography variant="subtitle2" gutterBottom>
                                                Log Message
                                            </Typography>
                                            <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'grey.50', mb: 2 }}>
                                                {renderContent(eventData.log, false)}
                                            </Paper>
                                        </>
                                    )}

                                    {/* Stack Trace Content */}
                                    {hasStackTraceContent && (
                                        <>
                                            <Typography variant="subtitle2" gutterBottom color="error">
                                                Stack Trace
                                            </Typography>
                                            <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'grey.50', borderColor: 'error.light' }}>
                                                {renderContent(eventData.stackTrace, true)}
                                            </Paper>
                                        </>
                                    )}

                                    {hasMetadata && (
                                        <>
                                            <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>
                                                Raw Metadata
                                            </Typography>
                                            <Paper variant="outlined" sx={{ p: 1.5, bgcolor: 'grey.50' }}>
                                                <pre style={{
                                                    margin: 0,
                                                    fontSize: '11px',
                                                    overflow: 'auto',
                                                    whiteSpace: 'pre-wrap',
                                                    fontFamily: 'monospace',
                                                    maxHeight: '200px'
                                                }}>
                                                    {JSON.stringify((event as any).metadata, null, 2)}
                                                </pre>
                                            </Paper>
                                        </>
                                    )}
                                </>
                            ) : (
                                <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', bgcolor: 'grey.50' }}>
                                    <Typography variant="body2" color="text.secondary">
                                        No additional details available for this event
                                    </Typography>
                                </Paper>
                            )}

                            {eventData.id && (
                                <Box sx={{ mt: 2, pt: 1, borderTop: '1px solid', borderColor: 'divider' }}>
                                    <Typography variant="caption" color="text.secondary">
                                        Event ID: {eventData.id}
                                    </Typography>
                                </Box>
                            )}
                        </Box>
                    </Collapse>
                </TableCell>
            </TableRow>
        </>
    );
};

export default EventDetailsRow;