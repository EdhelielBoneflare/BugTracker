// Usage on client site (once for whole site):
// <script src="https://cdn.example.com/bugtracker.bundle.js"></script>
// <script>
//   BugTracker.initialize(12345, { baseUrl: 'https://api.example.com' });
// </script>

import { Client } from './api/Client';
import { BugTracker as BugTrackerClass } from './core/BugTracker';
import type { SessionData } from './types/session';
import type { InternalEvent } from './types/events';

type InitOptions = {
    baseUrl?: string;
    debug?: boolean;
};

let clientInstance: Client | null = null;
let trackerInstance: BugTrackerClass | null = null;
let trackerInitPromise: Promise<any> | null = null;

const ensureInitialized = () => {
    if (!clientInstance && !trackerInstance) {
        throw new Error('BugTracker not initialized. Call BugTracker.initialize(projectId, opts) first.');
    }
};

const api = {
    async initialize(projectId: string | number, opts?: InitOptions) {
        // If an initialization is already in progress or done, return the same promise (idempotent)
        if (trackerInitPromise) return trackerInitPromise;
        if (trackerInstance) return Promise.resolve(api);

        const baseUrl = opts?.baseUrl ?? window.location.origin;

        // Create a client for low-level APIs (kept for backward compatibility)
        clientInstance = new Client(baseUrl, String(projectId));

        // Create the full BugTracker instance (synchronous construction)
        try {
            trackerInstance = new BugTrackerClass({ projectId: String(projectId), apiUrl: baseUrl, debug: !!opts?.debug });
        } catch (e) {
            // eslint-disable-next-line no-console
            console.error('[BugTracker] Failed to construct tracker:', e);
            throw e;
        }

        // Start initialization and expose a promise to callers
        trackerInitPromise = trackerInstance.initialize()
            .then(() => api)
            .catch((e) => {
                // Surface initialization errors to console and clear the promise so callers can retry
                // eslint-disable-next-line no-console
                console.error('[BugTracker] initialization failed:', e);
                trackerInitPromise = null;
                // Keep trackerInstance so integrator can inspect if needed, but allow retry by clearing the promise
                throw e;
            });

        return trackerInitPromise;
    },

    async sendEvents(events: InternalEvent[], sessionId: string | number, sessionData: SessionData) {
        ensureInitialized();
        if (clientInstance) return clientInstance.sendEvents(events, sessionId, sessionData);
        throw new Error('Client not available');
    },

    sendEventsWithBeacon(events: InternalEvent[], sessionId: string | number, sessionData: SessionData) {
        ensureInitialized();
        if (clientInstance) return clientInstance.sendEventsWithBeacon(events, sessionId, sessionData);
        return false;
    },

    async sendBugReport(payload: { screenshot: string; comment: string; email?: string }, sessionId: string | number, sessionData: SessionData) {
        ensureInitialized();
        if (clientInstance) return clientInstance.sendBugReport(payload, sessionId, sessionData);
        throw new Error('Client not available');
    },

    isInitialized() {
        return clientInstance !== null || trackerInstance !== null;
    }
};

// Expose globally when running in browser so CDN/script tag consumers can call BugTracker.initialize immediately
if (typeof window !== 'undefined') {
    try {
        (window as any).BugTracker = api;
    } catch (e) {
        // In very locked-down environments assignment may fail; we silently continue
        // bundlers will still get named/default exports
        // eslint-disable-next-line no-console
        console.debug('[BugTracker] Could not attach global BugTracker:', e);
    }
}

// Named exports so UMD global (which maps module exports) always contains `initialize` and helpers
export const initialize = api.initialize;
export const sendEvents = api.sendEvents;
export const sendEventsWithBeacon = api.sendEventsWithBeacon;
export const sendBugReport = api.sendBugReport;
export const isInitialized = api.isInitialized;

// Also export the full API object as a named export `BugTracker` to make sure consumers referencing
// `window.BugTracker.initialize` (UMD global) find the initialize function even if bundler maps named
// exports to the global object.
export const BugTracker = api;

export default api;
