export interface SessionData {
    id: number;
    startTime: string;
    lastActivity: string;
    active: boolean;

    // SessionContext fields
    browser: string;
    browserVersion: string;
    os: string;
    plugins: string[];
    screenResolution: string;
    viewportSize: string;
    cookiesHash?: string;
    timezone: string;
    language: string;
    userAgent: string;
    ipAddress?: string;

    // Additional fields
    deviceType: 'desktop' | 'mobile' | 'tablet';
    connectionType?: string;
    memoryUsage?: string;
    url: string;
    referrer?: string;
}