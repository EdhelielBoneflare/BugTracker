export enum EventType {
    ERROR = 'ERROR',
    ACTION = 'ACTION',
    NETWORK = 'NETWORK',
    PERFORMANCE = 'PERFORMANCE',
    CUSTOM = 'CUSTOM'
}

export interface InternalEvent {
    // Core fields
    type: EventType;
    name: string;
    timestamp: string;
    url: string;

    // Error details
    message?: string;
    stackTrace?: string;

    // Extended metadata (will be mapped to metadata field)
    lineNumber?: number;
    columnNumber?: number;
    fileName?: string;
    tagName?: string;
    xPath?: string;
    networkUrl?: string;
    statusCode?: number;
    duration?: number;

    // Additional custom metadata
    customMetadata?: Record<string, any>;
}