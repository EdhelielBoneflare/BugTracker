export class ConfigurationError extends Error {
    constructor(message: string) {
        super(`BugTracker Configuration Error: ${message}`);
        this.name = 'ConfigurationError';
    }
}

export class ValidationError extends Error {
    constructor(message: string) {
        super(`BugTracker Validation Error: ${message}`);
        this.name = 'ValidationError';
    }
}

export function validateConfig(config: any): void {
    if (!config) {
        throw new ConfigurationError('Configuration object is required');
    }

    if (!config.projectId || typeof config.projectId !== 'string') {
        throw new ConfigurationError('projectId is required and must be a string');
    }

    if (!config.apiUrl || typeof config.apiUrl !== 'string') {
        throw new ConfigurationError('apiUrl is required and must be a string');
    }

    // Validate URLs
    try {
        new URL(config.apiUrl);
    } catch {
        throw new ConfigurationError('apiUrl must be a valid URL');
    }

    // Validate numeric options
    if (config.maxBufferSize !== undefined &&
        (typeof config.maxBufferSize !== 'number' || config.maxBufferSize <= 0)) {
        throw new ConfigurationError('maxBufferSize must be a positive number');
    }

    // Allow flushInterval === 0 to disable periodic flushing. If provided and non-zero, it must be a positive number.
    if (config.flushInterval !== undefined && config.flushInterval !== 0 &&
        (typeof config.flushInterval !== 'number' || config.flushInterval <= 0)) {
        throw new ConfigurationError('flushInterval must be 0 (disabled) or a positive number');
    }

    if (config.sampleRate !== undefined &&
        (typeof config.sampleRate !== 'number' || config.sampleRate < 0 || config.sampleRate > 1)) {
        throw new ConfigurationError('sampleRate must be between 0 and 1');
    }

    // Validate button position
    const validPositions = ['top-left', 'top-right', 'bottom-left', 'bottom-right'];
    if (config.buttonPosition && !validPositions.includes(config.buttonPosition)) {
        throw new ConfigurationError(`buttonPosition must be one of: ${validPositions.join(', ')}`);
    }
}

export function validateEvent(event: any): boolean {
    if (!event || typeof event !== 'object') {
        return false;
    }

    // Required fields
    if (!event.type || !event.name || !event.timestamp || !event.url) {
        return false;
    }

    // Validate timestamp format
    try {
        const date = new Date(event.timestamp);
        if (isNaN(date.getTime())) {
            return false;
        }
    } catch {
        return false;
    }

    // Validate URL
    try {
        new URL(event.url);
    } catch {
        return false;
    }

    return true;
}

// remove sensitive info
export function sanitizeData(data: any): any {
    if (!data || typeof data !== 'object') {
        return data;
    }

    const sanitized = { ...data };

    const sensitiveFields = [
        'password', 'token', 'secret', 'key', 'credit', 'card', 'ssn',
        'passport', 'auth', 'cookie', 'session', 'jwt'
    ];

    Object.keys(sanitized).forEach(key => {
        const lowerKey = key.toLowerCase();

        // Check if key contains sensitive terms
        if (sensitiveFields.some(term => lowerKey.includes(term))) {
            sanitized[key] = '[REDACTED]';
        }

        // Recursively sanitize nested objects
        if (sanitized[key] && typeof sanitized[key] === 'object') {
            sanitized[key] = sanitizeData(sanitized[key]);
        }
    });

    return sanitized;
}
