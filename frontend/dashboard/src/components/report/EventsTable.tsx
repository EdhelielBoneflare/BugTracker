import React from 'react';
import {
    Card,
    CardContent,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Paper,
    Box,
} from '@mui/material';
import { BugReport } from '@mui/icons-material';
import { Event } from '../../types/types';
import EventDetailsRow from './EventDetailsRow';

interface EventsTableProps {
    events: Event[];
}

const EventsTable: React.FC<EventsTableProps> = ({ events }) => {
    if (events.length === 0) {
        return (
            <Card>
                <CardContent>
                    <Typography variant="body1" color="text.secondary" align="center" component="div">
                        No events found for this session
                    </Typography>
                </CardContent>
            </Card>
        );
    }

    return (
        <Card>
            <CardContent>
                <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1} component="div">
                    <BugReport />
                    Session Events ({events.length})
                </Typography>

                <TableContainer component={Paper} variant="outlined" sx={{ mt: 2 }}>
                    <Table size="small">
                        <TableHead>
                            <TableRow>
                                <TableCell width="40px" />
                                <TableCell>Type</TableCell>
                                <TableCell>Name</TableCell>
                                <TableCell>Timestamp</TableCell>
                                <TableCell>URL</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {events.map((event) => (
                                <EventDetailsRow key={`event-${event.id}`} event={event} />
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </CardContent>
        </Card>
    );
};

export default EventsTable;