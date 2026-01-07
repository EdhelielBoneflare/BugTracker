import { SessionData } from '../types/session';
import { getBrowserInfo, getOSInfo, getDeviceInfo, getConnectionInfo } from '../utils/browser';
import { Storage } from '../utils/storage';
import type { Client } from '../api/Client';

const SERVER_SESSION_KEY = 'bt_server_session';

export class SessionManager {
    private sessionId: number = 0; // numeric local id (negative until server assigns positive id)
    private serverSessionId: number | null = null; // in-memory + persisted server id
    private sessionData: SessionData | null = null;
    private storage: Storage;
    private sessionTimeout: number;
    private lastActivity: number = Date.now();
    private cleanupTimer: number | null = null;
    private client?: Client;

    constructor(sessionTimeout: number = 30 * 60 * 1000, client?: Client) {
        this.storage = new Storage();
        this.sessionTimeout = sessionTimeout;
        this.client = client;

        // Load server session id from localStorage (persisted across tabs and reloads)
        try {
            const raw = localStorage.getItem(SERVER_SESSION_KEY);
            if (raw) {
                const parsed = Number(raw);
                if (!Number.isNaN(parsed)) this.serverSessionId = parsed;
            }
        } catch (e) {
            // localStorage may be unavailable in some environments
        }
    }

    public initialize(): number {
        const existing = this.getExistingSession();

        if (existing && !this.isSessionExpired(existing)) {
            // Resume existing session
            this.sessionId = existing.id;
            this.sessionData = existing.data;
            this.lastActivity = existing.lastActivity;
            this.updateLastActivity();
            this.startCleanupTimer();

            // If we have a client and no serverSessionId yet, try to create one for this tab (sessionStorage will persist it across reloads)
            if (!this.serverSessionId && this.client) {
                this.createServerSession(this.sessionId, this.sessionData).catch(() => { /* ignore */ });
            }

            return this.sessionId;
        }

        // Create new session
        // createNewSession now starts an async server creation in background if client is available
        return this.createNewSession();
    }

    /**
     * Create new session with all SessionContext data
     * sync; it also attempts to create a server session asynchronously and stores that server id
     * in sessionStorage (tab-only) when available.
     */
    private createNewSession(): number {
        // Use a negative timestamp to denote a local-only id (unique per creation)
        this.sessionId = -Date.now();
        this.lastActivity = Date.now();
        this.sessionData = this.collectSessionData();

        this.sessionData.active = true;

        // Store in localStorage for cross-tab resume
        this.storage.set('bt_session', {
            id: this.sessionId,
            startTime: this.lastActivity,
            lastActivity: this.lastActivity,
            data: this.sessionData
        });

        // Setup cross-tab sync
        this.setupCrossTabSync();

        // Start cleanup timer
        this.startCleanupTimer();

        // Attempt to create server session in background (do not block initialization)
        if (this.client) {
            // Fire-and-forget
            this.createServerSession(this.sessionId, this.sessionData).catch((err) => {
                // Swallow errors; operate in local-only mode
                // Could add retry/backoff here if desired
                // no-op
            });
        }

        return this.sessionId;
    }

    private async createServerSession(localSessionId: number, sessionData: SessionData): Promise<void> {
        try {
            console.debug('[SessionManager] attempting to create server session for local id', localSessionId);
            const created = await this.client!.sendSessionStart(String(localSessionId), sessionData);
            if (created !== null && !Number.isNaN(Number(created))) {
                // Use the public setter so it persists to sessionStorage as intended
                this.setServerSessionId(Number(created));
                console.debug('[SessionManager] server session created:', this.serverSessionId);

                // Update in-memory session data id to numeric server id
                if (this.sessionData) {
                    (this.sessionData as any).id = Number(created);
                }
            } else {
                console.debug('[SessionManager] server session creation returned null (possibly invalid projectId or network error)');
            }
        } catch (e) {
            console.debug('[SessionManager] createServerSession failed:', e);
            // Ignore; remain local-only
            return;
        }
    }

    private collectSessionData(): SessionData {
        const browserInfo = getBrowserInfo();
        const osInfo = getOSInfo();
        const deviceInfo = getDeviceInfo();
        const connectionInfo = getConnectionInfo();

        return {
            id: this.sessionId,
            startTime: new Date().toISOString(),
            lastActivity: new Date().toISOString(),
            active: true,

            // SessionContext fields
            browser: browserInfo.name,
            browserVersion: browserInfo.version,
            os: osInfo.name,
            plugins: browserInfo.plugins,
            screenResolution: `${window.screen.width}x${window.screen.height}`,
            viewportSize: `${window.innerWidth}x${window.innerHeight}`,
            cookiesHash: this.hashCookies(document.cookie),
            timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
            language: navigator.language,
            userAgent: navigator.userAgent,

            // Additional fields
            deviceType: deviceInfo.type as any,
            connectionType: connectionInfo.type,
            url: window.location.href,
            referrer: document.referrer
        };
    }

    /**
     * Hash cookies for privacy
     */
    private hashCookies(cookies: string): string {
        // Simple hash - in production use SHA-256
        let hash = 0;
        for (let i = 0; i < cookies.length; i++) {
            const char = cookies.charCodeAt(i);
            hash = ((hash << 5) - hash) + char;
            hash = hash & hash;
        }
        return hash.toString(36);
    }

    private getExistingSession(): { id: number; lastActivity: number; data: SessionData } | null {
        const session = this.storage.get('bt_session') as { id: number; lastActivity: number; data: SessionData } | null;
        return session;
    }

    /**
     * Check if session expired (N minutes of inactivity)
     */
    private isSessionExpired(session: { lastActivity: number }): boolean {
        return Date.now() - session.lastActivity > this.sessionTimeout;
    }

    public updateLastActivity(): void {
        this.lastActivity = Date.now();

        const session = this.storage.get('bt_session') as { id: number; lastActivity: number; data: SessionData } | null;
        if (session) {
            session.lastActivity = this.lastActivity;
            this.storage.set('bt_session', session);
        }

        this.resetCleanupTimer();
    }

    /**
     * Start cleanup timer (checks every minute)
     */
    private startCleanupTimer(): void {
        if (this.cleanupTimer) clearInterval(this.cleanupTimer);

        this.cleanupTimer = window.setInterval(() => {
            const session = this.getExistingSession();
            if (session && this.isSessionExpired(session)) {
                this.endSession();
                if (this.cleanupTimer) {
                    clearInterval(this.cleanupTimer);
                    this.cleanupTimer = null;
                }
            }
        }, 60000);
    }

    /**
     * Reset cleanup timer
     */
    private resetCleanupTimer(): void {
        this.startCleanupTimer();
    }

    public endSession(): void {
        // Remove persisted session entirely so initialize() creates a fresh one instead of resuming
        try {
            this.storage.remove('bt_session');
        } catch (e) {
            // best-effort; ignore
        }

        // Reset in-memory session state
        this.sessionId = 0;
        this.sessionData = null;

        // Clear in-memory and persisted server id as session ended
        this.serverSessionId = null;
        try {
            localStorage.removeItem(SERVER_SESSION_KEY);
        } catch (e) {
            // ignore
        }

        // Stop cleanup timer if present
        if (this.cleanupTimer) {
            clearInterval(this.cleanupTimer);
            this.cleanupTimer = null;
        }
    }

    /**
     * Setup cross-tab sync
     */
    private setupCrossTabSync(): void {
        window.addEventListener('storage', (event) => {
            if (event.key === 'bt_session' && event.newValue) {
                try {
                    const newSession = JSON.parse(event.newValue);
                    if (newSession.startTime > this.lastActivity) {
                        this.sessionId = newSession.id;
                        this.sessionData = newSession.data;
                        this.lastActivity = newSession.lastActivity;
                    }
                } catch (e) {
                    // Ignore
                }
            }
        });
    }

    /**
     * Get session ID
     */
    public getSessionId(): number {
        // Prefer server session id if present (stored in sessionStorage), fall back to local numeric id
        return this.serverSessionId ?? this.sessionId;
    }

    /**
     * Set server-side session id in-memory + sessionStorage (tab-only)
     */
    public setServerSessionId(id: number): void {
        this.serverSessionId = Number(id);
        try {
            // Persist to localStorage so other pages/tabs from same origin can reuse the server session id
            localStorage.setItem(SERVER_SESSION_KEY, String(this.serverSessionId));
        } catch (e) {
            // ignore
        }
        if (this.sessionData) {
            (this.sessionData as any).id = Number(id);
        }
    }

    /**
     * Whether the current session is backed by server-created id
     */
    public isServerBacked(): boolean {
        return !!this.serverSessionId;
    }

    /**
     * Get session data
     */
    public getSessionData(): SessionData {
        if (!this.sessionData) {
            // This should never happen if initialize() was called
            // But if it does, create session data now
            console.warn('Session data accessed before initialization, creating now');
            this.initialize(); // This will create session data locally and kick off server creation in background
        }
        return this.sessionData!;
    }

    /**
     * Check if session is active
     */
    public isActive(): boolean {
        return !!this.getSessionId() && !this.isSessionExpired({ lastActivity: this.lastActivity });
    }
}