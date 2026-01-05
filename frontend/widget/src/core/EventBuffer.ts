import { InternalEvent } from '../types/events';

export class EventBuffer {
    private buffer: InternalEvent[] = [];
    private maxSize: number;
    private onFlush: (events: InternalEvent[]) => Promise<void>;

    constructor(
        maxSize: number,
        onFlush: (events: InternalEvent[]) => Promise<void>
    ) {
        this.maxSize = maxSize;
        this.onFlush = onFlush;
    }

    public add(event: InternalEvent): void {
        this.buffer.push(event);

        // Only flush immediately for user actions or errors so backend isn't polled periodically
        const urgent = event.type === 'ACTION' || event.type === 'ERROR';
        if (urgent) {
            // Fire-and-forget but handle errors inside flush
            void this.flush().catch(() => { /* swallow - buffer restored on failure */ });
        }

        // If buffer grows very large, trigger a flush to avoid unbounded memory usage
        if (this.buffer.length >= this.maxSize) {
            void this.flush().catch(() => { /* swallow */ });
        }
    }

    public async flush(): Promise<void> {
        if (this.buffer.length === 0) return;

        const eventsToSend = [...this.buffer];
        this.buffer = [];

        try {
            await this.onFlush(eventsToSend);
        } catch (error) {
            // Restore events on failure
            this.buffer = [...eventsToSend, ...this.buffer];
            throw error;
        }
    }

    public getEvents(): InternalEvent[] {
        return [...this.buffer];
    }

    public clear(): void {
        this.buffer = [];
    }

    public get size(): number {
        return this.buffer.length;
    }

    public isEmpty(): boolean {
        return this.buffer.length === 0;
    }

    public isFull(): boolean {
        return this.buffer.length >= this.maxSize;
    }

    // Helper used by BugTracker to decide whether to flush on visibilitychange / beforeunload
    public hasUrgentEvents(): boolean {
        return this.buffer.some(e => e.type === 'ACTION' || e.type === 'ERROR');
    }
}