/**
 * Catches JavaScript errors as per your design:
 * - window.onerror
 * - unhandled promise rejections
 * - console.error (optional)
 */

import { generateFingerprint } from '../utils/uuid';

export type ErrorHandler = (error: Error, metadata?: {
    type: 'window' | 'promise' | 'console';
    event?: ErrorEvent;
    reason?: any;
}) => void;

export class ErrorCatcher {
    private onError: ErrorHandler;

    private isCapturing: boolean = false;
    private originalConsoleError: typeof console.error | null = null;
    private ignoredErrors: string[] = [];

    constructor(onError: ErrorHandler) {
        this.onError = onError;
    }

    public start(): void {
        if (this.isCapturing) {
            return;
        }

        // 1. Window error events
        window.addEventListener('error', this.handleWindowError.bind(this));

        // 2. Unhandled promise rejections
        window.addEventListener('unhandledrejection', this.handleUnhandledRejection.bind(this));

        // 3. Console errors (optional - can be enabled separately)

        this.isCapturing = true;
    }

    public stop(): void {
        if (!this.isCapturing) {
            return;
        }

        window.removeEventListener('error', this.handleWindowError);
        window.removeEventListener('unhandledrejection', this.handleUnhandledRejection);

        if (this.originalConsoleError) {
            console.error = this.originalConsoleError;
            this.originalConsoleError = null;
        }

        this.isCapturing = false;
    }

    public enableConsoleErrors(): void {
        if (this.originalConsoleError) {
            return; // Already enabled
        }

        this.originalConsoleError = console.error;

        console.error = (...args: any[]) => {
            // Call original console.error
            this.originalConsoleError?.apply(console, args);

            // Create error object from console.error arguments
            let error: Error;

            if (args[0] instanceof Error) {
                error = args[0];
            } else {
                const message = args.map(arg =>
                    typeof arg === 'object' ? JSON.stringify(arg) : String(arg)
                ).join(' ');
                error = new Error(message);
                error.name = 'ConsoleError';
            }

            this.onError(error, {
                type: 'console',
                reason: args
            });
        };
    }

    public disableConsoleErrors(): void {
        if (!this.originalConsoleError) {
            return;
        }

        console.error = this.originalConsoleError;
        this.originalConsoleError = null;
    }

    public ignoreErrors(patterns: string[]): void {
        this.ignoredErrors = [...this.ignoredErrors, ...patterns];
    }

    private shouldIgnoreError(error: Error): boolean {
        if (!this.ignoredErrors.length) {
            return false;
        }

        const errorString = `${error.name}: ${error.message}`;
        return this.ignoredErrors.some(pattern =>
            errorString.includes(pattern) ||
            (error.stack && error.stack.includes(pattern))
        );
    }

    private handleWindowError(event: ErrorEvent): void {
        // Ignore errors from cross-origin scripts (for security)
        if (event.filename && !event.filename.startsWith(window.location.origin)) {
            return;
        }

        let error: Error;

        if (event.error) {
            error = event.error;
        } else {
            error = new Error(event.message);
            error.name = 'WindowError';
            error.stack = `at ${event.filename}:${event.lineno}:${event.colno}`;
        }

        // Add fingerprint for grouping
        (error as any).fingerprint = generateFingerprint(error);

        // Check if error should be ignored
        if (this.shouldIgnoreError(error)) {
            return;
        }

        this.onError(error, {
            type: 'window',
            event
        });
    }

    private handleUnhandledRejection(event: PromiseRejectionEvent): void {
        let error: Error;

        if (event.reason instanceof Error) {
            error = event.reason;
        } else {
            const reason = typeof event.reason === 'string'
                ? event.reason
                : JSON.stringify(event.reason);
            error = new Error(`Unhandled Promise Rejection: ${reason}`);
            error.name = 'UnhandledRejection';
        }

        // Add fingerprint for grouping
        (error as any).fingerprint = generateFingerprint(error);

        // Check if error should be ignored
        if (this.shouldIgnoreError(error)) {
            return;
        }

        this.onError(error, {
            type: 'promise',
            reason: event.reason
        });
    }

    public captureError(error: Error, metadata?: Record<string, any>): void {
        const enhancedError = new Error(error.message);
        enhancedError.name = error.name;
        enhancedError.stack = error.stack;
        (enhancedError as any).fingerprint = generateFingerprint(error);
        (enhancedError as any).customMetadata = metadata;

        this.onError(enhancedError, {
            type: 'window', // Treat as window error for consistency
            event: undefined
        });
    }

    public isActive(): boolean {
        return this.isCapturing;
    }

    public getIgnoredErrors(): string[] {
        return [...this.ignoredErrors];
    }

    public clearIgnoredErrors(): void {
        this.ignoredErrors = [];
    }
}