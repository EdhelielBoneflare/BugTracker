import { PayloadBuilder } from './payloads';
import { ApiResponse } from '../types/api';
import { InternalEvent } from '../types/events';
import { SessionData } from '../types/session';

export class Client {
    private baseUrl: string;
    private projectId: string;

    constructor(baseUrl: string, projectId: string) {
        this.baseUrl = baseUrl.replace(/\/$/, '');
        this.projectId = projectId;
    }

    // Send events one-by-one to backend's /api/events which accepts single EventRequest
    async sendEvents(
        events: InternalEvent[],
        sessionId: string | number,
        sessionData: SessionData
    ): Promise<ApiResponse> {
        // Try to use numeric sessionId expected by backend
        const numericSessionId = Number(sessionId);
        if (Number.isNaN(numericSessionId)) {
            // If sessionId is not numeric, backend will reject; throw so caller knows
            throw new Error('Session ID must be numeric to send events to backend');
        }

        // Attempt to parse projectId to number (backend expects Long)
        const numericProjectId = Number(this.projectId);
        if (Number.isNaN(numericProjectId)) {
            throw new Error('Project ID must be numeric to send events to backend');
        }

        // Send events one by one and return the last response (or aggregate if desired)
        let lastResponse: Response | null = null;
        for (const ev of events) {
            // Build EventRequest inline to avoid typing mismatch
            const combined = (ev.message || '') + (ev.stackTrace ? (': ' + ev.stackTrace) : '');
            const fallback = combined === '' ? '' : combined;

            // Prefer a client-generated event id to use as the 'log' (correlation id)
            const eventLogId = ev.eventId || (ev.customMetadata && ev.customMetadata.eventLogId) || undefined;

            const payload = {
                sessionId: numericSessionId,
                type: ev.type,
                name: ev.name,
                // Use client-side event id as `log` when available so backend can correlate
                log: eventLogId ?? fallback,
                stackTrace: ev.stackTrace ?? '',
                url: ev.url ?? window.location.href,
                element: ev.tagName ?? (ev.customMetadata && (ev.customMetadata.element || ev.customMetadata.elementId)) ?? '',
                timestamp: new Date(ev.timestamp).toISOString(),
                metadata: {
                    fileName: ev.fileName ?? '',
                    lineNumber: ev.lineNumber ? String(ev.lineNumber) : '',
                    statusCode: ev.statusCode ? String(ev.statusCode) : ''
                }
            };

            const response = await fetch(`${this.baseUrl}/api/events`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload),
                keepalive: true
            });

            lastResponse = response;

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${await response.text()}`);
            }
        }

        // Backend EventController returns created event id in a map; fabricate ApiResponse for compatibility
        return {
            success: true,
            message: 'Events sent',
            data: {
                sessionId: sessionId,
                receivedCount: events.length,
                timestamp: new Date().toISOString()
            }
        } as ApiResponse;
    }

    sendEventsWithBeacon(
        events: InternalEvent[],
        sessionId: string | number,
        sessionData: SessionData
    ): boolean {
        // Use numeric sessionId
        const numericSessionId = Number(sessionId);
        if (Number.isNaN(numericSessionId)) {
            return false;
        }

        const numericProjectId = Number(this.projectId);
        if (Number.isNaN(numericProjectId)) {
            return false;
        }

        try {
            for (const ev of events) {
                const eventLogId = ev.eventId || (ev.customMetadata && ev.customMetadata.eventLogId) || undefined;
                const combined = (ev.message || '') + (ev.stackTrace ? (': ' + ev.stackTrace) : '');
                const fallback = combined === '' ? '' : combined;

                const payload = {
                    sessionId: numericSessionId,
                    type: ev.type,
                    name: ev.name,
                    log: eventLogId ?? fallback,
                    stackTrace: ev.stackTrace ?? '',
                    url: ev.url ?? window.location.href,
                    element: ev.tagName ?? (ev.customMetadata && (ev.customMetadata.element || ev.customMetadata.elementId)) ?? '',
                    timestamp: new Date(ev.timestamp).toISOString(),
                    metadata: {
                        fileName: ev.fileName ?? '',
                        lineNumber: ev.lineNumber ? String(ev.lineNumber) : '',
                        statusCode: ev.statusCode ? String(ev.statusCode) : ''
                    }
                };

                const blob = new Blob([JSON.stringify(payload)], {
                    type: 'application/json'
                });

                // Send each event separately
                navigator.sendBeacon(`${this.baseUrl}/api/events`, blob);
            }
            return true;
        } catch (e) {
            return false;
        }
    }

    async sendBugReport(
        data: {
            screenshot: string;
            comment: string;
            email?: string;
        },
        sessionId: string | number,
        sessionData: SessionData
    ): Promise<ApiResponse> {
        // For backend, POST to /api/reports/widget with ReportCreationRequestWidget shape
        const numericSessionId = Number(sessionId);
        const numericProjectId = Number(this.projectId);

        if (Number.isNaN(numericProjectId)) {
            throw new Error('Project ID must be numeric to send bug reports to backend');
        }

        if (Number.isNaN(numericSessionId)) {
            throw new Error('Session ID must be numeric to send bug reports to backend');
        }

        const payload = {
            projectId: numericProjectId,
            sessionId: numericSessionId,
            title: data.comment ? data.comment.substring(0, 80) : 'User report',
            tags: [],
            reportedAt: new Date().toISOString(),
            comments: data.comment,
            userEmail: data.email,
            screen: data.screenshot,
            currentUrl: window.location.href,
            userProvided: true
        };

        const response = await fetch(`${this.baseUrl}/api/reports/widget`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${await response.text()}`);
        }

        // Backend returns map with message and reportId; normalize to ApiResponse
        const body = await response.json();
        return {
            success: true,
            message: body.message || 'Report created',
            data: {
                sessionId: sessionId,
                receivedCount: 1,
                timestamp: new Date().toISOString()
            }
        } as ApiResponse;
    }

    // Create session on server and return created numeric session id or null
    async sendSessionStart(sessionId: string, sessionData: SessionData): Promise<number | null> {
        const numericProjectId = Number(this.projectId);
        if (Number.isNaN(numericProjectId)) {
            // Cannot create server session without numeric projectId
            return null;
        }

        const payload = {
            projectId: numericProjectId,
            startTime: sessionData.startTime,
            browser: sessionData.browser,
            browserVersion: sessionData.browserVersion,
            os: sessionData.os,
            deviceType: sessionData.deviceType,
            screenResolution: sessionData.screenResolution,
            viewportSize: sessionData.viewportSize,
            language: sessionData.language,
            userAgent: sessionData.userAgent,
            ipAddress: sessionData.ipAddress,
            cookiesHash: sessionData.cookiesHash,
            plugins: sessionData.plugins
        };

        try {
            const response = await fetch(`${this.baseUrl}/api/sessions`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                return null;
            }

            const body = await response.json();
            // body should be SessionCreationResponse { message, sessionId }
            return body.sessionId as number;
        } catch (e) {
            return null;
        }
    }
}