declare global {
    interface Window {
        html2canvas?: Html2Canvas;
    }
}

const HTML2CANVAS_CDN = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';

export type Html2Canvas = (element: HTMLElement, options?: any) => Promise<HTMLCanvasElement>;

export class ScreenshotCapturer {
    private html2canvas: Html2Canvas | null = null;
    private defaultOptions: any = {};
    private cdnUrl: string = HTML2CANVAS_CDN;
    private loadingPromise: Promise<void> | null = null;

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
        if (typeof window !== 'undefined' && window.html2canvas) {
            this.html2canvas = window.html2canvas;
        }
    }

    private async ensureHtml2Canvas(): Promise<void> {
        if (this.html2canvas) return;

        this.checkAvailability();
        if (this.html2canvas) return;

        // Prevent duplicate loads
        if (this.loadingPromise) {
            return this.loadingPromise;
        }

        this.loadingPromise = new Promise<void>((resolve, reject) => {
            // Check if script already exists
            const existing = document.querySelector(`script[src="${this.cdnUrl}"]`) as HTMLScriptElement | null;
            if (existing) {
                const checkAndResolve = () => {
                    this.checkAvailability();
                    if (this.html2canvas) {
                        resolve();
                    } else {
                        reject(new Error('html2canvas not available after load'));
                    }
                };

                if (window.html2canvas) {
                    this.html2canvas = window.html2canvas;
                    resolve();
                    return;
                }

                existing.addEventListener('load', checkAndResolve, { once: true });
                existing.addEventListener('error', () => reject(new Error('Failed to load html2canvas')), { once: true });
                return;
            }

            const script = document.createElement('script');
            script.src = this.cdnUrl;
            script.async = true;
            script.onload = () => {
                this.checkAvailability();
                if (this.html2canvas) {
                    resolve();
                } else {
                    reject(new Error('html2canvas did not expose expected API'));
                }
            };
            script.onerror = () => reject(new Error('Failed to load html2canvas script'));
            document.head.appendChild(script);
        });

        return this.loadingPromise;
    }

    public isHtml2CanvasAvailable(): boolean {
        return !!this.html2canvas || !!(typeof window !== 'undefined' && window.html2canvas);
    }

    public setOptions(opts: Partial<any>) {
        this.defaultOptions = { ...this.defaultOptions, ...opts };
    }

    public getOptions() {
        return { ...this.defaultOptions };
    }

    private async capture(element: HTMLElement, options?: any): Promise<string> {
        await this.ensureHtml2Canvas();
        if (!this.html2canvas && !window.html2canvas) {
            throw new Error('html2canvas is not available');
        }
        const impl = this.html2canvas || window.html2canvas!;
        const opts = { ...this.defaultOptions, ...options };
        const canvas = await impl(element, opts);
        return canvas.toDataURL('image/jpeg', 0.8);
    }

    public async capturePreview(targetElement?: HTMLElement): Promise<string> {
        const target = targetElement || document.body;
        const opts = { ...this.defaultOptions, scale: 0.5, quality: 0.6 };
        return await this.capture(target, opts);
    }


    public async captureFinal(target?: HTMLElement): Promise<string> {
        const el = target || document.body;
        const opts = { ...this.defaultOptions, scale: 0.8, quality: 0.8, backgroundColor: '#ffffff' };
        try {
            return await this.capture(el, opts);
        } catch (e) {
            const fallbackOpts = {
                ...this.defaultOptions,
                width: window.innerWidth,
                height: window.innerHeight,
                x: window.pageXOffset,
                y: window.pageYOffset
            };
            await this.ensureHtml2Canvas();
            const impl = this.html2canvas || window.html2canvas!;
            const canvas = await impl(document.body, fallbackOpts);
            return canvas.toDataURL('image/jpeg', 0.8);
        }
    }

    public async captureViewport(): Promise<string> {
        const opts = {
            ...this.defaultOptions,
            width: window.innerWidth,
            height: window.innerHeight,
            x: window.pageXOffset,
            y: window.pageYOffset
        };
        await this.ensureHtml2Canvas();
        const impl = this.html2canvas || window.html2canvas!;
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
