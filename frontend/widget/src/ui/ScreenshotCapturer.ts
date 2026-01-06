export type Html2Canvas = (element: HTMLElement, options?: any) => Promise<HTMLCanvasElement>;

export class ScreenshotCapturer {
    private html2canvas: Html2Canvas | null = null;
    private defaultOptions: any = {};
    private cdnUrl: string = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';

    constructor() {
        this.setDefaultOptions();
        this.checkAvailability();
    }

    private setDefaultOptions() {
        this.defaultOptions = {
            scale: 0.8,
            useCORS: true,
            allowTaint: false,
            backgroundColor: '#ffffff',
            logging: false,
            imageTimeout: 15000,
            removeContainer: true,
            onclone: null
        };
    }

    private checkAvailability() {
        if (typeof (window as any) !== 'undefined' && (window as any).html2canvas) {
            this.html2canvas = (window as any).html2canvas as Html2Canvas;
        }
    }

    // Loads html2canvas dynamically if not present
    private async ensureHtml2Canvas(): Promise<void> {
        if (this.html2canvas) return;
        // If global exists, use it
        this.checkAvailability();
        if (this.html2canvas) return;

        // First try a dynamic import so bundlers/CSP-friendly deployments that include html2canvas
        // as an npm dependency will provide it as same-origin code. This avoids tracking-prevention
        // blocking the CDN-hosted script.
        try {
            // eslint-disable-next-line @typescript-eslint/no-var-requires
            const mod = await import('html2canvas');
            // Module default is the html2canvas function
            const impl = (mod && (mod.default || mod)) as any;
            if (typeof impl === 'function') {
                this.html2canvas = impl as Html2Canvas;
                return;
            }
        } catch (e) {
            // dynamic import failed (module not installed or blocked). Fall through to CDN loader.
            // console.debug('[BugTracker] dynamic import of html2canvas failed, falling back to CDN:', e);
        }

        // Otherwise, load from CDN as a last resort
        await new Promise<void>((resolve, reject) => {
            const script = document.createElement('script');
            script.src = this.cdnUrl;
            script.async = true;
            script.onload = () => {
                this.checkAvailability();
                if (this.html2canvas) resolve();
                else reject(new Error('html2canvas did not expose expected API'));
            };
            script.onerror = (err) => reject(new Error('Failed to load html2canvas script'));
            document.head.appendChild(script);
        });
    }

    public isHtml2CanvasAvailable(): boolean {
        return !!this.html2canvas || !!(window as any).html2canvas;
    }

    public setOptions(opts: Partial<any>) {
        this.defaultOptions = { ...this.defaultOptions, ...opts };
    }

    public getOptions() {
        return { ...this.defaultOptions };
    }

    private async capture(element: HTMLElement, options?: any): Promise<string> {
        await this.ensureHtml2Canvas();
        if (!this.html2canvas && !(window as any).html2canvas) {
            throw new Error('html2canvas is not available');
        }
        const impl = this.html2canvas || (window as any).html2canvas;
        const opts = { ...this.defaultOptions, ...options };
        const canvas = await impl(element, opts);
        return canvas.toDataURL('image/jpeg', 0.8);
    }

    public async capturePreview(target?: HTMLElement): Promise<string> {
        const el = target || document.body;
        // small preview
        const opts = { ...this.defaultOptions, scale: 0.3, quality: 0.5, backgroundColor: null } as any;
        return this.capture(el, opts);
    }

    public async captureFinal(target?: HTMLElement): Promise<string> {
        const el = target || document.body;
        const opts = { ...this.defaultOptions, scale: 0.8, quality: 0.8, backgroundColor: '#ffffff' } as any;
        try {
            return await this.capture(el, opts);
        } catch (e) {
            // fallback to viewport capture
            const fallbackOpts = { ...this.defaultOptions, width: window.innerWidth, height: window.innerHeight, x: window.pageXOffset, y: window.pageYOffset } as any;
            await this.ensureHtml2Canvas();
            const impl = this.html2canvas || (window as any).html2canvas;
            const canvas = await impl(document.body, fallbackOpts);
            return canvas.toDataURL('image/jpeg', 0.8);
        }
    }

    public async captureViewport(): Promise<string> {
        const opts = { ...this.defaultOptions, width: window.innerWidth, height: window.innerHeight, x: window.pageXOffset, y: window.pageYOffset } as any;
        await this.ensureHtml2Canvas();
        const impl = this.html2canvas || (window as any).html2canvas;
        const canvas = await impl(document.body, opts);
        return canvas.toDataURL('image/jpeg', 0.8);
    }

    public async dataUrlToBlob(dataUrl: string): Promise<Blob | null> {
        return new Promise((resolve) => {
            try {
                const parts = dataUrl.split(',');
                const mime = parts[0].match(/:(.*?);/)?.[1] || 'image/jpeg';
                const bstr = atob(parts[1]);
                let n = bstr.length;
                const u8arr = new Uint8Array(n);
                while (n--) u8arr[n] = bstr.charCodeAt(n);
                resolve(new Blob([u8arr], { type: mime }));
            } catch (e) {
                console.error('Failed to convert data URL to Blob:', e);
                resolve(null);
            }
        });
    }
}

