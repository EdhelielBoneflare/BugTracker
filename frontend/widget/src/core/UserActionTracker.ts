// TypeScript
// File: src/core/UserActionTracker.ts
import { getElementXPath, getElementText, isInteractiveElement, findInteractiveElement } from '../utils/dom';
import { throttle } from '../utils/timing';

export type UserActionHandler = (action: {
    type: 'click' | 'submit' | 'change' | 'keydown' | 'navigation' | string;
    element: Element;
    target: Element;
    value?: string;
    event: Event;
    metadata: {
        xPath: string;
        text: string;
        tagName: string;
        id?: string;
        className?: string;
        href?: string;
        value?: string;
        coordinates?: { x: number; y: number };
        url: string;
        // Additional optional fields used across the module:
        navigationType?: string;
        referrer?: string;
        key?: string;
        code?: string;
        interactionId?: number;
        [key: string]: any;
    };
}) => void;

export class UserActionTracker {
    private onAction: UserActionHandler;
    private isTracking: boolean = false;
    private ignoredActions: string[] = [];
    private trackedElements: Set<string> = new Set();
    private throttleDelay: number = 100;

    constructor(onAction: UserActionHandler) {
        this.onAction = onAction;
    }

    public start(): void {
        if (this.isTracking) return;

        const throttledClick = throttle(this.handleClick.bind(this), this.throttleDelay);
        const throttledSubmit = throttle(this.handleSubmit.bind(this), this.throttleDelay);
        const throttledChange = throttle(this.handleChange.bind(this), this.throttleDelay);
        const throttledKeydown = throttle(this.handleKeydown.bind(this), this.throttleDelay);

        document.addEventListener('click', throttledClick, { capture: true, passive: true });
        document.addEventListener('submit', throttledSubmit, { capture: true });
        document.addEventListener('change', throttledChange, { capture: true });
        document.addEventListener('keydown', throttledKeydown, { capture: true });

        this.trackNavigation();

        this.isTracking = true;
    }

    public stop(): void {
        if (!this.isTracking) return;

        document.removeEventListener('click', this.handleClick);
        document.removeEventListener('submit', this.handleSubmit);
        document.removeEventListener('change', this.handleChange);
        document.removeEventListener('keydown', this.handleKeydown);

        this.isTracking = false;
    }

    public configure(options: { ignoredActions?: string[]; throttleDelay?: number; }): void {
        if (options.ignoredActions) this.ignoredActions = options.ignoredActions;
        if (options.throttleDelay !== undefined) this.throttleDelay = options.throttleDelay;
    }

    private shouldIgnoreAction(element: Element, type: string): boolean {
        if (this.ignoredActions.includes(type)) return true;
        if (element.hasAttribute('data-bt-ignore')) return true;
        const tagName = element.tagName.toLowerCase();
        if (['script', 'style', 'link', 'meta'].includes(tagName)) return true;
        if (tagName === 'html' || tagName === 'body') return true;
        return false;
    }

    private handleClick(event: MouseEvent): void {
        const target = event.target as Element;
        if (!target) return;
        const interactiveElement = findInteractiveElement(target) || target;
        if (this.shouldIgnoreAction(interactiveElement, 'click')) return;

        const elementKey = `${interactiveElement.tagName}-${interactiveElement.id || interactiveElement.className}`;
        if (this.trackedElements.has(elementKey)) return;
        this.trackedElements.add(elementKey);
        setTimeout(() => this.trackedElements.delete(elementKey), 1000);

        this.onAction({
            type: 'click',
            element: interactiveElement,
            target: target,
            event,
            metadata: {
                xPath: getElementXPath(interactiveElement),
                text: getElementText(interactiveElement, 100),
                tagName: interactiveElement.tagName,
                id: interactiveElement.id || undefined,
                className: interactiveElement.className || undefined,
                href: (interactiveElement as HTMLAnchorElement).href || undefined,
                coordinates: { x: event.clientX, y: event.clientY },
                url: window.location.href
            }
        });
    }

    private handleSubmit(event: Event): void {
        const form = event.target as HTMLFormElement;
        if (!form) return;
        if (this.shouldIgnoreAction(form, 'submit')) return;

        this.onAction({
            type: 'submit',
            element: form,
            target: form,
            event,
            metadata: {
                xPath: getElementXPath(form),
                text: getElementText(form, 100),
                tagName: form.tagName,
                id: form.id || undefined,
                className: form.className || undefined,
                action: form.action,
                method: form.method,
                url: window.location.href
            }
        });
    }

    private handleChange(event: Event): void {
        const target = event.target as HTMLInputElement;
        if (!target) return;
        const inputType = target.type;
        const trackableTypes = ['text', 'email', 'password', 'search', 'tel', 'url', 'number'];
        if (!trackableTypes.includes(inputType)) return;
        if (this.shouldIgnoreAction(target, 'change')) return;

        this.onAction({
            type: 'change',
            element: target,
            target: target,
            value: target.value,
            event,
            metadata: {
                xPath: getElementXPath(target),
                text: getElementText(target, 50),
                tagName: target.tagName,
                id: target.id || undefined,
                className: target.className || undefined,
                type: inputType,
                name: target.name || undefined,
                value: target.value.substring(0, 100),
                url: window.location.href
            }
        });
    }

    private handleKeydown(event: KeyboardEvent): void {
        const importantKeys = ['Enter', 'Escape', 'Tab'];
        if (!importantKeys.includes(event.key)) return;
        const target = event.target as Element;
        if (!target) return;
        if (this.shouldIgnoreAction(target, 'keydown')) return;

        this.onAction({
            type: 'keydown',
            element: target,
            target: target,
            event,
            metadata: {
                xPath: getElementXPath(target),
                text: getElementText(target, 50),
                tagName: target.tagName,
                id: target.id || undefined,
                className: target.className || undefined,
                key: event.key,
                code: event.code,
                url: window.location.href
            }
        });
    }

    private trackNavigation(): void {
        const originalPushState = history.pushState;
        const originalReplaceState = history.replaceState;

        const trackNavigationEvent = (type: 'pushstate' | 'replacestate') => {
            this.onAction({
                type: 'navigation',
                element: document.body,
                target: document.body,
                event: new Event(type),
                metadata: {
                    xPath: '/html/body',
                    text: '',
                    tagName: 'BODY',
                    url: window.location.href,
                    navigationType: type,
                    referrer: document.referrer
                }
            });
        };

        history.pushState = function(...args) {
            originalPushState.apply(this, args);
            trackNavigationEvent('pushstate');
        };

        history.replaceState = function(...args) {
            originalReplaceState.apply(this, args);
            trackNavigationEvent('replacestate');
        };

        window.addEventListener('popstate', () => {
            this.onAction({
                type: 'navigation',
                element: document.body,
                target: document.body,
                event: new Event('popstate'),
                metadata: {
                    xPath: '/html/body',
                    text: '',
                    tagName: 'BODY',
                    url: window.location.href,
                    navigationType: 'popstate',
                    referrer: document.referrer
                }
            });
        });
    }

    public trackManualAction(type: string, element: Element, metadata?: Record<string, any>): void {
        this.onAction({
            type: type as any,
            element,
            target: element,
            event: new Event('manual'),
            metadata: {
                xPath: getElementXPath(element),
                text: getElementText(element, 100),
                tagName: element.tagName,
                id: element.id || undefined,
                className: element.className || undefined,
                url: window.location.href,
                ...metadata
            }
        });
    }

    public isActive(): boolean {
        return this.isTracking;
    }

    public getConfig(): { ignoredActions: string[]; throttleDelay: number; } {
        return { ignoredActions: this.ignoredActions, throttleDelay: this.throttleDelay };
    }
}