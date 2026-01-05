
export function measureExecution<T>(fn: () => T, name: string): { result: T; duration: number } {
    const start = performance.now();
    const result = fn();
    const duration = performance.now() - start;

    return { result, duration };
}

export function getPerformanceTiming(): Record<string, number> {
    if (!performance || !performance.timing) {
        return {};
    }

    const timing = performance.timing;

    return {
        // Navigation
        navigationStart: timing.navigationStart,
        unloadEventStart: timing.unloadEventStart,
        unloadEventEnd: timing.unloadEventEnd,
        redirectStart: timing.redirectStart,
        redirectEnd: timing.redirectEnd,
        fetchStart: timing.fetchStart,

        // DNS
        domainLookupStart: timing.domainLookupStart,
        domainLookupEnd: timing.domainLookupEnd,

        // Connection
        connectStart: timing.connectStart,
        connectEnd: timing.connectEnd,
        secureConnectionStart: timing.secureConnectionStart,

        // Request/Response
        requestStart: timing.requestStart,
        responseStart: timing.responseStart,
        responseEnd: timing.responseEnd,

        // DOM
        domLoading: timing.domLoading,
        domInteractive: timing.domInteractive,
        domContentLoadedEventStart: timing.domContentLoadedEventStart,
        domContentLoadedEventEnd: timing.domContentLoadedEventEnd,
        domComplete: timing.domComplete,

        // Load
        loadEventStart: timing.loadEventStart,
        loadEventEnd: timing.loadEventEnd
    };
}

/**
 * Calculate performance metrics from timing data
 */
export function calculatePerformanceMetrics(): Record<string, number> {
    const timing = getPerformanceTiming();

    if (Object.keys(timing).length === 0) {
        return {};
    }

    const t = timing;

    return {
        // Traditional metrics
        pageLoadTime: t.loadEventEnd - t.navigationStart,
        domReadyTime: t.domContentLoadedEventEnd - t.navigationStart,
        domInteractiveTime: t.domInteractive - t.navigationStart,

        // Network metrics
        redirectTime: t.redirectEnd - t.redirectStart,
        appCacheTime: t.domainLookupStart - t.fetchStart,
        dnsTime: t.domainLookupEnd - t.domainLookupStart,
        tcpTime: t.connectEnd - t.connectStart,
        sslTime: t.connectEnd - t.secureConnectionStart,
        requestTime: t.responseStart - t.requestStart,
        responseTime: t.responseEnd - t.responseStart,

        // Processing metrics
        domProcessingTime: t.domComplete - t.domLoading,
        domContentLoadTime: t.domContentLoadedEventEnd - t.domContentLoadedEventStart,
        onLoadTime: t.loadEventEnd - t.loadEventStart
    };
}

export function throttle<T extends (...args: any[]) => any>(
    fn: T,
    delay: number
): (...args: Parameters<T>) => void {
    let lastCall = 0;
    let timeout: number | null = null;

    return function(this: any, ...args: Parameters<T>) {
        const now = Date.now();
        const remaining = delay - (now - lastCall);

        if (remaining <= 0) {
            lastCall = now;
            fn.apply(this, args);
        } else if (!timeout) {
            timeout = window.setTimeout(() => {
                lastCall = Date.now();
                fn.apply(this, args);
                timeout = null;
            }, remaining);
        }
    };
}

export function debounce<T extends (...args: any[]) => any>(
    fn: T,
    delay: number
): (...args: Parameters<T>) => void {
    let timeout: number | null = null;

    return function(this: any, ...args: Parameters<T>) {
        if (timeout) {
            clearTimeout(timeout);
        }

        timeout = window.setTimeout(() => {
            fn.apply(this, args);
            timeout = null;
        }, delay);
    };
}
