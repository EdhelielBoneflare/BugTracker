/**
 * Tracks performance metrics:
 * - Page load times
 * - Core Web Vitals
 * - Resource timing
 * - Long tasks
 */

export type PerformanceHandler = (metric: {
    name: string;
    value: number;
    unit: 'ms' | 's' | 'score';
    metadata: {
        url: string;
        timestamp: string;
        [key: string]: any;
    };
}) => void;

export class PerformanceMonitor {
    private onMetric: PerformanceHandler;
    private isMonitoring: boolean = false;
    private observedEntries: PerformanceObserver[] = [];

    // Configuration
    private trackCoreWebVitals: boolean = true;
    private trackResourceTiming: boolean = false;
    private trackLongTasks: boolean = false;
    private threshold: number = 1000; // 1 second threshold

    constructor(onMetric: PerformanceHandler) {
        this.onMetric = onMetric;
    }

    public start(): void {
        if (this.isMonitoring) {
            return;
        }

        // Track traditional performance timing
        this.trackNavigationTiming();

        // Track Core Web Vitals if supported
        if (this.trackCoreWebVitals && 'PerformanceObserver' in window) {
            this.trackCoreWebVitalsMetrics();
        }

        // Track resource timing if enabled
        if (this.trackResourceTiming) {
            this.trackResourceTimingMetrics();
        }

        // Track long tasks if enabled
        if (this.trackLongTasks) {
            this.trackLongTasksMetrics();
        }

        this.isMonitoring = true;
    }

    public stop(): void {
        if (!this.isMonitoring) {
            return;
        }

        // Disconnect all observers
        this.observedEntries.forEach(observer => observer.disconnect());
        this.observedEntries = [];

        this.isMonitoring = false;
    }

    public configure(options: {
        trackCoreWebVitals?: boolean;
        trackResourceTiming?: boolean;
        trackLongTasks?: boolean;
        threshold?: number;
    }): void {
        if (options.trackCoreWebVitals !== undefined) {
            this.trackCoreWebVitals = options.trackCoreWebVitals;
        }

        if (options.trackResourceTiming !== undefined) {
            this.trackResourceTiming = options.trackResourceTiming;
        }

        if (options.trackLongTasks !== undefined) {
            this.trackLongTasks = options.trackLongTasks;
        }

        if (options.threshold !== undefined) {
            this.threshold = options.threshold;
        }
    }

    private trackNavigationTiming(): void {
        if (!performance || !performance.timing) {
            return;
        }

        // Wait for page to fully load
        if (document.readyState === 'complete') {
            this.captureNavigationTiming();
        } else {
            window.addEventListener('load', () => {
                setTimeout(() => this.captureNavigationTiming(), 0);
            });
        }
    }

    private captureNavigationTiming(): void {
        const timing = performance.timing;

        // Calculate key metrics
        const metrics = {
            // Page load metrics
            pageLoadTime: timing.loadEventEnd - timing.navigationStart,
            domReadyTime: timing.domContentLoadedEventEnd - timing.navigationStart,
            domInteractiveTime: timing.domInteractive - timing.navigationStart,

            // Network metrics
            redirectTime: timing.redirectEnd - timing.redirectStart,
            dnsTime: timing.domainLookupEnd - timing.domainLookupStart,
            tcpTime: timing.connectEnd - timing.connectStart,
            requestTime: timing.responseStart - timing.requestStart,
            responseTime: timing.responseEnd - timing.responseStart,

            // Processing metrics
            domProcessingTime: timing.domComplete - timing.domLoading,
            onLoadTime: timing.loadEventEnd - timing.loadEventStart
        };

        // Report metrics that exceed threshold
        Object.entries(metrics).forEach(([name, value]) => {
            if (value > 0 && value < Number.MAX_SAFE_INTEGER) {
                this.reportMetric(name, value);
            }
        });
    }

    private trackCoreWebVitalsMetrics(): void {
        // Largest Contentful Paint (LCP)
        try {
            const lcpObserver = new PerformanceObserver((entryList) => {
                const entries = entryList.getEntries();
                const lastEntry = entries[entries.length - 1];

                this.reportMetric('largest_contentful_paint', lastEntry.startTime, {
                    element: (lastEntry as any).element?.tagName,
                    url: (lastEntry as any).element?.src || (lastEntry as any).element?.href,
                    size: (lastEntry as any).size
                });
            });

            lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true });
            this.observedEntries.push(lcpObserver);
        } catch (e) {
            console.warn('LCP observation not supported:', e);
        }

        // First Input Delay (FID)
        try {
            const fidObserver = new PerformanceObserver((entryList) => {
                const entries = entryList.getEntries();
                entries.forEach(entry => {
                    // Use any to access interactionId which may not be present in lib defs
                    const fidEntry: any = entry;
                    const value = (fidEntry.processingStart - fidEntry.startTime);
                    this.reportMetric('first_input_delay', value, {
                        target: (fidEntry.target as Element)?.tagName,
                        interactionId: fidEntry.interactionId
                    });
                });
            });

            fidObserver.observe({ type: 'first-input', buffered: true });
            this.observedEntries.push(fidObserver);
        } catch (e) {
            console.warn('FID observation not supported:', e);
        }

        // Cumulative Layout Shift (CLS)
        try {
            let clsValue = 0;
            let clsEntries: any[] = [];

            const clsObserver = new PerformanceObserver((entryList) => {
                for (const entry of entryList.getEntries()) {
                    // Only count layout shifts without recent user input
                    if (!(entry as any).hadRecentInput) {
                        clsEntries.push(entry);
                        clsValue += (entry as any).value;
                    }
                }

                // Report CLS periodically
                if (clsValue > 0) {
                    this.reportMetric('cumulative_layout_shift', clsValue, {
                        entries: clsEntries.length,
                        sessionValue: clsValue
                    });
                }
            });

            clsObserver.observe({ type: 'layout-shift', buffered: true });
            this.observedEntries.push(clsObserver);
        } catch (e) {
            console.warn('CLS observation not supported:', e);
        }

        // First Contentful Paint (FCP)
        try {
            const fcpObserver = new PerformanceObserver((entryList) => {
                const entries = entryList.getEntries();
                const firstPaint = entries[0];

                this.reportMetric('first_contentful_paint', firstPaint.startTime);
            });

            fcpObserver.observe({ type: 'paint', buffered: true });
            this.observedEntries.push(fcpObserver);
        } catch (e) {
            console.warn('FCP observation not supported:', e);
        }
    }

    private trackResourceTimingMetrics(): void {
        if (!performance.getEntriesByType) {
            return;
        }

        // Monitor slow resources
        const resourceObserver = new PerformanceObserver((entryList) => {
            const entries = entryList.getEntries();

            entries.forEach(entry => {
                const resourceEntry = entry as PerformanceResourceTiming;

                // Check if resource load time exceeds threshold
                const loadTime = resourceEntry.responseEnd - resourceEntry.startTime;

                if (loadTime > this.threshold) {
                    this.reportMetric('slow_resource', loadTime, {
                        name: resourceEntry.name,
                        initiatorType: resourceEntry.initiatorType,
                        transferSize: resourceEntry.transferSize,
                        encodedBodySize: resourceEntry.encodedBodySize,
                        decodedBodySize: resourceEntry.decodedBodySize
                    });
                }
            });
        });

        try {
            resourceObserver.observe({ type: 'resource', buffered: true });
            this.observedEntries.push(resourceObserver);
        } catch (e) {
            console.warn('Resource timing observation not supported:', e);
        }
    }

    private trackLongTasksMetrics(): void {
        try {
            const longTaskObserver = new PerformanceObserver((entryList) => {
                const entries = entryList.getEntries();

                entries.forEach(entry => {
                    // Treat entry as PerformanceEntry with known duration and optional attribution
                    const longTaskEntry = entry as PerformanceEntry & { attribution?: any[]; duration: number };

                    this.reportMetric('long_task', longTaskEntry.duration, {
                        attribution: longTaskEntry.attribution || []
                    });
                });
            });

            longTaskObserver.observe({ type: 'longtask', buffered: true });
            this.observedEntries.push(longTaskObserver);
        } catch (e) {
            console.warn('Long task observation not supported:', e);
        }
    }

    private reportMetric(name: string, value: number, additionalMetadata: Record<string, any> = {}): void {
        this.onMetric({
            name,
            value,
            unit: 'ms',
            metadata: {
                url: window.location.href,
                timestamp: new Date().toISOString(),
                threshold: this.threshold,
                ...additionalMetadata
            }
        });
    }

    public captureMetric(name: string, value: number, metadata?: Record<string, any>): void {
        this.reportMetric(name, value, metadata || {});
    }

    public getCurrentMetrics(): Record<string, number> {
        const metrics: Record<string, number> = {};

        // Navigation timing
        if (performance?.timing) {
            const timing = performance.timing;
            metrics.pageLoadTime = timing.loadEventEnd - timing.navigationStart;
            metrics.domReadyTime = timing.domContentLoadedEventEnd - timing.navigationStart;
        }

        // Memory info if available
        if ((performance as any).memory) {
            const memory = (performance as any).memory;
            metrics.usedJSHeapSize = memory.usedJSHeapSize;
            metrics.totalJSHeapSize = memory.totalJSHeapSize;
            metrics.jsHeapSizeLimit = memory.jsHeapSizeLimit;
        }

        return metrics;
    }

    public isActive(): boolean {
        return this.isMonitoring;
    }

    public getConfig(): {
        trackCoreWebVitals: boolean;
        trackResourceTiming: boolean;
        trackLongTasks: boolean;
        threshold: number;
    } {
        return {
            trackCoreWebVitals: this.trackCoreWebVitals,
            trackResourceTiming: this.trackResourceTiming,
            trackLongTasks: this.trackLongTasks,
            threshold: this.threshold
        };
    }
}