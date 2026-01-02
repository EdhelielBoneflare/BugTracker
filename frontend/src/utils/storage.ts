export class Storage {
    private prefix = 'bt_';

    set<T>(key: string, value: T): void {
        try {
            const prefixedKey = this.prefix + key;
            const serialized = JSON.stringify(value);
            localStorage.setItem(prefixedKey, serialized);
        } catch (error) {
            console.warn('Failed to save to localStorage:', error);
            // localStorage may be full or disabled
        }
    }

    get<T>(key: string): T | null {
        try {
            const prefixedKey = this.prefix + key;
            const item = localStorage.getItem(prefixedKey);
            return item ? JSON.parse(item) : null;
        } catch (error) {
            console.warn('Failed to read from localStorage:', error);
            return null;
        }
    }

    remove(key: string): void {
        try {
            const prefixedKey = this.prefix + key;
            localStorage.removeItem(prefixedKey);
        } catch (error) {
            console.warn('Failed to remove from localStorage:', error);
        }
    }

    clear(): void {
        try {
            const keysToRemove: string[] = [];

            // Find all BugTracker keys
            for (let i = 0; i < localStorage.length; i++) {
                const key = localStorage.key(i);
                if (key && key.startsWith(this.prefix)) {
                    keysToRemove.push(key);
                }
            }

            // Remove all found keys
            keysToRemove.forEach(key => localStorage.removeItem(key));
        } catch (error) {
            console.warn('Failed to clear localStorage:', error);
        }
    }

    setSession(session: any): void {
        this.set('session', session);
    }

    getSession(): any {
        return this.get('session');
    }

    updateLastActivity(): void {
        const session = this.getSession();
        if (session) {
            session.lastActivity = Date.now();
            this.setSession(session);
        }
    }

    static isAvailable(): boolean {
        try {
            const testKey = '__bt_test__';
            localStorage.setItem(testKey, testKey);
            localStorage.removeItem(testKey);
            return true;
        } catch {
            return false;
        }
    }
}