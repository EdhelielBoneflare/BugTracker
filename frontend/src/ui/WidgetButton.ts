// TypeScript
// File: src/ui/WidgetButton.ts
export class WidgetButton {
    private button: HTMLButtonElement | null = null;
    private position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
    private text: string;
    private onClick: () => void;
    private isVisible: boolean = true;

    // Styles
    private styles: {
        base: Partial<CSSStyleDeclaration>;
        hover: Partial<CSSStyleDeclaration>;
        active: Partial<CSSStyleDeclaration>;
    };

    constructor(
        position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' = 'bottom-right',
        onClick: () => void,
        text: string = 'Report Bug'
    ) {
        this.position = position;
        this.onClick = onClick;
        this.text = text;

        // Define button styles
        this.styles = {
            base: {
                position: 'fixed',
                zIndex: '2147483647',
                padding: '12px 24px',
                backgroundColor: '#e74c3c',
                color: 'white',
                border: 'none',
                borderRadius: '25px',
                cursor: 'pointer',
                fontSize: '14px',
                fontWeight: '600',
                fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                outline: 'none',
                userSelect: 'none',
                touchAction: 'manipulation',
                backdropFilter: 'blur(10px)',
                borderWidth: '1px',
                borderStyle: 'solid',
                borderColor: 'rgba(255, 255, 255, 0.2)'
            },
            hover: {
                backgroundColor: '#c0392b',
                transform: 'translateY(-2px)',
                boxShadow: '0 6px 20px rgba(0, 0, 0, 0.2)'
            },
            active: {
                backgroundColor: '#a93226',
                transform: 'translateY(0)',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)'
            }
        };
    }

    public create(): void {
        if (this.button) {
            return;
        }

        this.button = document.createElement('button');
        this.button.id = 'bugtracker-widget-button';
        this.button.innerHTML = this.text;
        this.button.title = 'Report a bug or issue';
        this.button.setAttribute('aria-label', 'Report a bug');
        this.button.setAttribute('role', 'button');

        this.applyStyles(this.styles.base);

        // Use the internal applyPosition method (renamed) to avoid recursion
        this.applyPosition();

        this.addEventListeners();

        // If body is not yet available (script ran in <head>), wait for DOMContentLoaded
        if (!document.body) {
            const onReady = () => {
                try {
                    document.body.appendChild(this.button!);
                    this.addGlobalStyles();
                } catch (err) {
                    // If append still fails, clean up to avoid dangling state
                    console.error('WidgetButton: failed to append button after DOMContentLoaded', err);
                    this.button = null;
                }
            };

            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', onReady, { once: true });
            } else {
                // document is already interactive/complete but body was null (very rare)
                setTimeout(onReady, 0);
            }

            return;
        }

        // Normal path: append immediately
        document.body.appendChild(this.button);
        this.addGlobalStyles();
    }

    private applyStyles(styles: Partial<CSSStyleDeclaration>): void {
        if (!this.button) return;
        Object.assign(this.button.style, styles);
    }

    // Renamed from setPosition to applyPosition to avoid name collision with public setter
    private applyPosition(): void {
        if (!this.button) return;

        const positions = {
            'top-left': { top: '20px', left: '20px' },
            'top-right': { top: '20px', right: '20px' },
            'bottom-left': { bottom: '20px', left: '20px' },
            'bottom-right': { bottom: '20px', right: '20px' }
        } as Record<string, Partial<CSSStyleDeclaration>>;

        Object.assign(this.button.style, positions[this.position]);
    }

    private addEventListeners(): void {
        if (!this.button) return;

        this.button.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            this.onClick();
        });

        this.button.addEventListener('mouseenter', () => {
            this.applyStyles(this.styles.hover);
        });

        this.button.addEventListener('mouseleave', () => {
            this.applyStyles(this.styles.base);
        });

        this.button.addEventListener('mousedown', () => {
            this.applyStyles(this.styles.active);
        });

        this.button.addEventListener('mouseup', () => {
            this.applyStyles(this.styles.hover);
        });

        this.button.addEventListener('touchstart', () => {
            this.applyStyles(this.styles.active);
        }, { passive: true });

        this.button.addEventListener('touchend', () => {
            this.applyStyles(this.styles.base);
        }, { passive: true });

        this.button.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.onClick();
            }
        });
    }

    private addGlobalStyles(): void {
        const styleId = 'bugtracker-widget-styles';
        if (document.getElementById(styleId)) {
            return;
        }

        const style = document.createElement('style');
        style.id = styleId;
        style.textContent = `
            #bugtracker-widget-button {
                animation: bugtracker-button-appear 0.3s ease;
            }
            @keyframes bugtracker-button-appear {
                from { opacity: 0; transform: translateY(20px) scale(0.9); }
                to { opacity: 1; transform: translateY(0) scale(1); }
            }
            @media (prefers-reduced-motion: reduce) {
                #bugtracker-widget-button { animation: none; transition: none; }
            }
        `;

        // document.head may be null in very early parsing stages; fall back to document.documentElement
        const target = document.head || document.documentElement || document.body;
        try {
            target.appendChild(style);
        } catch (err) {
            // last resort: try to append later
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => {
                    (document.head || document.documentElement || document.body)!.appendChild(style);
                }, { once: true });
            } else {
                console.error('WidgetButton: failed to append global styles', err);
            }
        }
    }

    public show(): void {
        if (!this.button) return;
        this.button.style.display = 'block';
        this.button.style.visibility = 'visible';
        this.button.style.opacity = '1';
        this.isVisible = true;
    }

    public hide(): void {
        if (!this.button) return;
        this.button.style.display = 'none';
        this.isVisible = false;
    }

    public toggle(): void {
        if (this.isVisible) {
            this.hide();
        } else {
            this.show();
        }
    }

    public setText(text: string): void {
        this.text = text;
        if (this.button) {
            this.button.innerHTML = text;
        }
    }

    // Public setter keeps the name \`setPosition\` but uses the internal \`applyPosition\`
    public setPosition(position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right'): void {
        this.position = position;
        if (this.button) {
            this.applyPosition();
        }
    }

    public updateStyles(styles: {
        base?: Partial<CSSStyleDeclaration>;
        hover?: Partial<CSSStyleDeclaration>;
        active?: Partial<CSSStyleDeclaration>;
    }): void {
        if (styles.base) this.styles.base = { ...this.styles.base, ...styles.base };
        if (styles.hover) this.styles.hover = { ...this.styles.hover, ...styles.hover };
        if (styles.active) this.styles.active = { ...this.styles.active, ...styles.active };
        if (this.button) this.applyStyles(this.styles.base);
    }

    public destroy(): void {
        if (!this.button) return;
        if (this.button.parentNode) this.button.parentNode.removeChild(this.button);
        const styleElement = document.getElementById('bugtracker-widget-styles');
        if (styleElement && styleElement.parentNode) styleElement.parentNode.removeChild(styleElement);
        this.button = null;
    }

    public exists(): boolean {
        return this.button !== null;
    }

    public isButtonVisible(): boolean {
        return this.isVisible;
    }

    public getButtonElement(): HTMLButtonElement | null {
        return this.button;
    }
}
