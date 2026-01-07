import { InternalEvent } from '../types/events';
import { SessionData } from '../types/session';
import { EventBatchPayload, EventPayload, BugReportPayload } from '../types/api';

export class PayloadBuilder {

    static buildEventBatchPayload(
        projectId: string,
        sessionId: string,
        events: InternalEvent[],
        sessionData: SessionData
    ): EventBatchPayload {
        return {
            projectId,
            sessionId,
            events: events.map(event => this.buildEventPayload(event)),
            sessionContext: this.buildSessionContext(sessionData)
        };
    }

    static buildEventPayload(event: InternalEvent): EventPayload {
        return {
            type: event.type as 'ERROR' | 'ACTION' | 'NETWORK' | 'PERFORMANCE' | 'CUSTOM',
            name: event.name,
            message: event.message,
            stackTrace: event.stackTrace,
            timestamp: event.timestamp,
            url: event.url,
            metadata: {
                lineNumber: event.lineNumber,
                columnNumber: event.columnNumber,
                fileName: event.fileName,
                tagName: event.tagName,
                xPath: event.xPath,
                networkUrl: event.networkUrl,
                statusCode: event.statusCode,
                duration: event.duration,
                ...event.customMetadata
            }
        };
    }

    static buildSessionContext(sessionData: SessionData) {
        return {
            browser: sessionData.browser,
            browserVersion: sessionData.browserVersion,
            os: sessionData.os,
            plugins: sessionData.plugins,
            screenResolution: sessionData.screenResolution,
            viewportSize: sessionData.viewportSize,
            cookiesHash: sessionData.cookiesHash,
            timezone: sessionData.timezone,
            language: sessionData.language,
            userAgent: sessionData.userAgent,
            deviceType: sessionData.deviceType,
            connectionType: sessionData.connectionType
        };
    }

    static buildBugReportPayload(
        projectId: string,
        sessionId: string,
        data: {
            screenshot: string;
            comment: string;
            email?: string;
            currentUrl: string;
        },
        sessionData: SessionData
    ): BugReportPayload {
        return {
            projectId,
            sessionId,
            screenshot: data.screenshot,
            comment: data.comment,
            userEmail: data.email,
            currentUrl: data.currentUrl,
            reportedAt: new Date().toISOString(),
            additionalInfo: {
                deviceType: sessionData.deviceType,
                connectionType: sessionData.connectionType,
                memoryUsage: sessionData.memoryUsage,
                browser: sessionData.browser,
                browserVersion: sessionData.browserVersion,
                os: sessionData.os
            }
        };
    }

    // New: build EventRequest DTO shape expected by backend (uses numeric projectId/sessionId)
    static buildEventRequest(
        projectId: String,
        sessionId: number,
        event: InternalEvent,
        sessionData: SessionData
    ) {
        return {
            sessionId: sessionId,
            type: event.type,
            name: event.name,
            log: event.message ?? '',
            stackTrace: event.stackTrace ?? '',
            url: event.url ?? window.location.href,
            element: event.tagName ?? (event.customMetadata && (event.customMetadata.element || event.customMetadata.elementId)) ?? '',
            timestamp: new Date(event.timestamp).toISOString(),
            metadata: {
                fileName: event.fileName ?? '',
                lineNumber: event.lineNumber ? String(event.lineNumber) : '',
                statusCode: event.statusCode ? String(event.statusCode) : ''
            }
        };
    }

    // New: build ReportCreationRequestWidget
    static buildReportWidgetRequest(
        projectId: String,
        sessionId: number,
        data: {
            screenshot: string;
            comment: string;
            email?: string;
            currentUrl: string;
        },
        sessionData: SessionData
    ) {
        return {
            projectId: projectId,
            sessionId: sessionId,
            title: data.comment ? data.comment.substring(0, 80) : 'User report',
            tags: [],
            reportedAt: new Date().toISOString(),
            comments: data.comment,
            userEmail: data.email,
            screen: data.screenshot,
            currentUrl: data.currentUrl,
            userProvided: true
        };
    }

    // New: build SessionRequest for backend
    static buildSessionRequest(projectId: String, sessionData: SessionData) {
        return {
            projectId: projectId,
            startTime: new Date(sessionData.startTime ?? Date.now()).toISOString(),
            browser: sessionData.browser,
            browserVersion: sessionData.browserVersion,
            os: sessionData.os,
            deviceType: sessionData.deviceType,
            screenResolution: sessionData.screenResolution,
            viewportSize: sessionData.viewportSize,
            language: sessionData.language,
            userAgent: sessionData.userAgent,
            ipAddress: undefined,
            cookiesHash: sessionData.cookiesHash,
            plugins: sessionData.plugins
        };
    }
}