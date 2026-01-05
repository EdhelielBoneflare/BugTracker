/**
 * Monitors network errors via monkey-patching:
 * - Failed XHR/fetch requests
 * - Network timeouts
 * - Aborted requests
 */

export type NetworkErrorHandler = (details: {
    type: 'fetch' | 'xhr';
    url: string;
    method: string;
    status: number;
    statusText: string;
    duration: number;
    error?: Error;
    requestBody?: any;
    responseBody?: any;
}) => void;

export class NetworkMonitor {
    private onError: NetworkErrorHandler;
    private isMonitoring: boolean = false;

    // Original implementations
    private originalFetch: typeof fetch;
    private originalXMLHttpRequest: typeof XMLHttpRequest;
    private originalXHRPrototype: any;

    // Configuration
    private trackSuccessfulRequests: boolean = false;
    private trackTimeout: number = 10000; // 10 seconds
    private ignoredUrls: RegExp[] = [];

    constructor(onError: NetworkErrorHandler) {
        this.onError = onError;
        this.originalFetch = window.fetch;
        this.originalXMLHttpRequest = window.XMLHttpRequest;
        this.originalXHRPrototype = XMLHttpRequest.prototype;
    }

    public start(): void {
        if (this.isMonitoring) {
            return;
        }

        // Patch fetch API
        this.patchFetch();

        // Patch XMLHttpRequest
        this.patchXMLHttpRequest();

        this.isMonitoring = true;
    }

    public stop(): void {
        if (!this.isMonitoring) {
            return;
        }

        // Restore original fetch
        window.fetch = this.originalFetch;

        // Restore original XMLHttpRequest
        window.XMLHttpRequest = this.originalXMLHttpRequest;

        this.isMonitoring = false;
    }

    /**
     * Configure monitoring options
     */
    public configure(options: {
        trackSuccessfulRequests?: boolean;
        trackTimeout?: number;
        ignoredUrls?: RegExp[];
    }): void {
        if (options.trackSuccessfulRequests !== undefined) {
            this.trackSuccessfulRequests = options.trackSuccessfulRequests;
        }

        if (options.trackTimeout !== undefined) {
            this.trackTimeout = options.trackTimeout;
        }

        if (options.ignoredUrls !== undefined) {
            this.ignoredUrls = options.ignoredUrls;
        }
    }

    private shouldIgnoreUrl(url: string): boolean {
        return this.ignoredUrls.some(regex => regex.test(url));
    }

    private patchFetch(): void {
        const self = this;

        window.fetch = async function(...args) {
            const startTime = performance.now();

            // Extract request details
            const requestInfo = args[0];
            const requestInit = args[1] || {};

            let url: string;
            let method: string = 'GET';
            let requestBody: any = null;

            // Parse request information
            if (typeof requestInfo === 'string') {
                url = requestInfo;
            } else if (requestInfo instanceof Request) {
                url = requestInfo.url;
                method = requestInfo.method;
            } else {
                url = String(requestInfo);
            }

            // Check if URL should be ignored
            if (self.shouldIgnoreUrl(url)) {
                return self.originalFetch.apply(window, args);
            }

            // Get request body
            if (requestInit.body) {
                requestBody = requestInit.body;
            }

            method = requestInit.method || method;

            try {
                // Make the request
                const response = await self.originalFetch.apply(window, args);
                const duration = performance.now() - startTime;

                // Check for errors
                if (!response.ok) {
                    let responseBody: any = null;

                    try {
                        // Try to read response body for error details
                        const clone = response.clone();
                        const contentType = response.headers.get('content-type');

                        if (contentType && contentType.includes('application/json')) {
                            responseBody = await clone.json();
                        } else {
                            responseBody = await clone.text();
                        }
                    } catch {
                        // Ignore errors reading response body
                    }

                    self.onError({
                        type: 'fetch',
                        url,
                        method,
                        status: response.status,
                        statusText: response.statusText,
                        duration,
                        responseBody
                    });
                }

                // Track successful requests if enabled
                if (self.trackSuccessfulRequests && duration > self.trackTimeout) {
                    self.onError({
                        type: 'fetch',
                        url,
                        method,
                        status: response.status,
                        statusText: 'Timeout',
                        duration,
                        error: new Error(`Request took ${duration.toFixed(0)}ms`)
                    });
                }

                return response;

            } catch (error) {
                const duration = performance.now() - startTime;

                self.onError({
                    type: 'fetch',
                    url,
                    method,
                    status: 0,
                    statusText: 'Network Error',
                    duration,
                    error: error as Error,
                    requestBody
                });

                throw error;
            }
        };
    }

    private patchXMLHttpRequest(): void {
        const self = this;
        const OriginalXHR = this.originalXMLHttpRequest;

        class PatchedXHR extends OriginalXHR {
            private _url: string = '';
            private _method: string = 'GET';
            private _startTime: number = 0;
            private _requestBody: any = null;
            private _responseBody: any = null;

            open(method: string, url: string | URL, ...args: any[]): void {
                this._method = method;
                this._url = url.toString();
                this._startTime = performance.now();

                if (!self.shouldIgnoreUrl(this._url)) {
                    this.addEventListener('error', this.handleError.bind(this));
                    this.addEventListener('load', this.handleLoad.bind(this));
                    this.addEventListener('timeout', this.handleTimeout.bind(this));
                    this.addEventListener('abort', this.handleAbort.bind(this));
                }

                // Use apply to satisfy TS about spread argument types
                (super.open as any).apply(this, [method, url, ...args]);
            }

            send(body?: any): void {
                this._requestBody = body;
                super.send(body);
            }

            private handleError(): void {
                const duration = performance.now() - this._startTime;

                self.onError({
                    type: 'xhr',
                    url: this._url,
                    method: this._method,
                    status: this.status,
                    statusText: this.statusText,
                    duration,
                    error: new Error('XHR Network Error'),
                    requestBody: this._requestBody,
                    responseBody: this._responseBody
                });
            }

            private handleLoad(): void {
                const duration = performance.now() - this._startTime;

                // Try to capture response body for errors
                if (this.status >= 400) {
                    try {
                        if (this.responseType === '' || this.responseType === 'text') {
                            this._responseBody = this.responseText;
                        } else if (this.responseType === 'json') {
                            this._responseBody = this.response;
                        }
                    } catch {
                        // Ignore errors reading response
                    }

                    self.onError({
                        type: 'xhr',
                        url: this._url,
                        method: this._method,
                        status: this.status,
                        statusText: this.statusText,
                        duration,
                        requestBody: this._requestBody,
                        responseBody: this._responseBody
                    });
                }

                // Track slow requests
                if (self.trackSuccessfulRequests && duration > self.trackTimeout) {
                    self.onError({
                        type: 'xhr',
                        url: this._url,
                        method: this._method,
                        status: this.status,
                        statusText: 'Timeout',
                        duration,
                        error: new Error(`Request took ${duration.toFixed(0)}ms`)
                    });
                }
            }

            private handleTimeout(): void {
                const duration = performance.now() - this._startTime;

                self.onError({
                    type: 'xhr',
                    url: this._url,
                    method: this._method,
                    status: 408,
                    statusText: 'Request Timeout',
                    duration,
                    error: new Error('XHR Request Timeout'),
                    requestBody: this._requestBody
                });
            }

            private handleAbort(): void {
                const duration = performance.now() - this._startTime;

                self.onError({
                    type: 'xhr',
                    url: this._url,
                    method: this._method,
                    status: 0,
                    statusText: 'Request Aborted',
                    duration,
                    error: new Error('XHR Request Aborted'),
                    requestBody: this._requestBody
                });
            }
        }

        window.XMLHttpRequest = PatchedXHR as any;
    }

    public isActive(): boolean {
        return this.isMonitoring;
    }

    public getConfig(): {
        trackSuccessfulRequests: boolean;
        trackTimeout: number;
        ignoredUrls: RegExp[];
    } {
        return {
            trackSuccessfulRequests: this.trackSuccessfulRequests,
            trackTimeout: this.trackTimeout,
            ignoredUrls: this.ignoredUrls
        };
    }
}