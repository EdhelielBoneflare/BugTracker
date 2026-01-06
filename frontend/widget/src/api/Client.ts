import {ApiResponse} from '../types/api';
import {EventType, InternalEvent} from '../types/events';
import {SessionData} from '../types/session';

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

        // Send events one by one and return the last response (or aggregate if desired)
        let lastResponse: Response | null = null;
        for (const ev of events) {
            // Build EventRequest inline to avoid typing mismatch
            const combined = (ev.message || '') + (ev.stackTrace ? (': ' + ev.stackTrace) : '');
            const fallback = combined === '' ? '' : combined;

            // Prefer a client-generated event id to use as the 'log' (correlation id)
            const eventLogId = ev.customMetadata?.eventLogId ?? ev.customMetadata?.eventId;

            // Build log according to event type: errors -> message, actions -> action attributes, else id/fallback
            let logValue: string;
            if (ev.type === EventType.ERROR) {
                logValue = ev.message ?? fallback;
            } else if (ev.type === EventType.ACTION) {
                const cm = ev.customMetadata || {};
                if (cm.method || cm.action) {
                    logValue = `form: ${cm.method ?? ''} ${cm.action ?? ''}`.trim();
                } else if (cm.href) {
                    logValue = `link: ${cm.href}`;
                } else {
                    logValue = eventLogId ?? fallback;
                }
            } else {
                logValue = eventLogId ?? fallback;
            }

            const payload = {
                sessionId: numericSessionId,
                type: ev.type,
                name: ev.name,
                // Use client-side event id as `log` when available so backend can correlate
                log: logValue,
                stackTrace: ev.stackTrace ?? '',
                url: ev.url ?? window.location.href,
                // Prefer element id/class or tag name for quick indexing; include full details under metadata
                element: (ev.customMetadata && (ev.customMetadata.elementId || ev.customMetadata.element)) ?? ev.tagName ?? '',
                timestamp: new Date(ev.timestamp).toISOString(),
                metadata: {
                    fileName: ev.fileName ?? '',
                    lineNumber: ev.lineNumber ? String(ev.lineNumber) : '',
                    statusCode: ev.statusCode ? String(ev.statusCode) : '',
                    // Preserve all custom metadata so action attributes are not lost
                    ...(ev.customMetadata || {})
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

        try {
            for (const ev of events) {
                // Prefer client-provided ids from customMetadata
                const eventLogId = ev.customMetadata?.eventLogId ?? ev.customMetadata?.eventId;

                const combined = (ev.message || '') + (ev.stackTrace ? (': ' + ev.stackTrace) : '');
                const fallback = combined === '' ? '' : combined;

                // Build log according to event type
                let logValue: string;
                if (ev.type === EventType.ERROR) {
                    logValue = ev.message ?? fallback;
                } else if (ev.type === EventType.ACTION) {
                    const cm = ev.customMetadata || {};
                    if (cm.method || cm.action) {
                        logValue = `form: ${cm.method ?? ''} ${cm.action ?? ''}`.trim();
                    } else if (cm.href) {
                        logValue = `link: ${cm.href}`;
                    } else {
                        logValue = eventLogId ?? fallback;
                    }
                } else {
                    logValue = eventLogId ?? fallback;
                }

                const payload = {
                    sessionId: numericSessionId,
                    type: ev.type,
                    name: ev.name,
                    log: logValue,
                    stackTrace: ev.stackTrace ?? '',
                    url: ev.url ?? window.location.href,
                    element: (ev.customMetadata && (ev.customMetadata.elementId || ev.customMetadata.element)) ?? ev.tagName ?? '',
                    timestamp: new Date(ev.timestamp).toISOString(),
                    metadata: {
                        fileName: ev.fileName ?? '',
                        lineNumber: ev.lineNumber ? String(ev.lineNumber) : '',
                        statusCode: ev.statusCode ? String(ev.statusCode) : '',
                        ...(ev.customMetadata || {})
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

        if (Number.isNaN(numericSessionId)) {
            throw new Error('Session ID must be numeric to send bug reports to backend');
        }

        const payload = {
            projectId: this.projectId,
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
        const payload = {
            projectId: this.projectId,
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
