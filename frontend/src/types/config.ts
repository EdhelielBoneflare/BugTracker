export interface BugTrackerConfig {
    // Required fields
    projectId: string;
    apiUrl: string;

    // Buffer configuration
    maxBufferSize?: number;           // Default: 50
    flushInterval?: number;           // Default: 5 seconds

    // Feature toggles
    captureUserActions?: boolean;     // Default: true
    captureNetworkErrors?: boolean;   // Default: true
    capturePerformance?: boolean;     // Default: false
    captureConsoleErrors?: boolean;   // Default: true

    // UI configuration
    buttonPosition?: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
    buttonText?: string;              // Default: "Report Bug"
    disableScreenshots?: boolean;     // Default: false

    // Session configuration
    sessionTimeout?: number;          // Default: 1800000 (30 minutes)
    sessionId?: string;               // Override auto-generated session ID

    // Filtering
    ignoreUrls?: RegExp[];            // URLs to ignore
    sampleRate?: number;              // 0.0 to 1.0, default: 1.0
    ignoredActions?: string[];        // User actions to ignore

    // Callbacks
    beforeSend?: (event: any) => any | null;
    onError?: (error: Error) => void;

    // Advanced
    debug?: boolean;                  // Default: false
    environment?: string;             // e.g., 'production', 'development'
    release?: string;                 // Version/release tag
}

export const DEFAULT_CONFIG: Partial<BugTrackerConfig> = {
    maxBufferSize: 50,
    flushInterval: 0,
    captureUserActions: true,
    captureNetworkErrors: true,
    capturePerformance: false,
    captureConsoleErrors: true,
    buttonPosition: 'bottom-right',
    buttonText: 'Report Bug',
    disableScreenshots: false,
    sessionTimeout: 30 * 60 * 1000, // 30 minutes
    sampleRate: 1.0,
    debug: false,
    environment: 'production'
};