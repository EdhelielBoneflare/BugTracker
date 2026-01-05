export function getElementXPath(element: Element): string {
    if (!element || !element.ownerDocument) {
        return '';
    }

    // If element has ID, use it for simple XPath
    if (element.id) {
        return `//*[@id="${element.id}"]`;
    }

    const parts: string[] = [];
    let current: Element | null = element;

    while (current && current.nodeType === Node.ELEMENT_NODE) {
        let index = 0;
        const siblings = current.parentNode?.children || [];

        // Find position among siblings with same tag name
        for (let i = 0; i < siblings.length; i++) {
            const sibling = siblings[i] as Element;
            if (sibling === current) {
                const tagName = current.tagName.toLowerCase();
                parts.unshift(`${tagName}[${index + 1}]`);
                break;
            }

            if (sibling.tagName === current.tagName) {
                index++;
            }
        }

        current = current.parentElement;
    }

    return parts.length > 0 ? `/${parts.join('/')}` : '';
}

export function getElementSelector(element: Element): string {
    if (!element || !element.ownerDocument) {
        return '';
    }

    // Try ID first
    if (element.id) {
        return `#${CSS.escape(element.id)}`;
    }

    // Try class names
    if (element.classList.length > 0) {
        const classSelector = `.${Array.from(element.classList)
            .map(cls => CSS.escape(cls))
            .join('.')}`;

        // Check if selector is unique
        if (element.ownerDocument.querySelectorAll(classSelector).length === 1) {
            return classSelector;
        }
    }

    // Try tag name with attributes
    const tagName = element.tagName.toLowerCase();
    const attributes = Array.from(element.attributes)
        .filter(attr => attr.name !== 'class' && attr.name !== 'id')
        .map(attr => `[${CSS.escape(attr.name)}="${CSS.escape(attr.value)}"]`)
        .join('');

    if (attributes) {
        const selector = `${tagName}${attributes}`;
        if (element.ownerDocument.querySelectorAll(selector).length === 1) {
            return selector;
        }
    }

    // Fallback to XPath
    return getElementXPath(element);
}

export function isElementVisible(element: Element): boolean {
    if (!element) return false;

    const style = window.getComputedStyle(element);

    // Check basic visibility
    if (style.display === 'none' ||
        style.visibility === 'hidden' ||
        style.opacity === '0') {
        return false;
    }

    // Check if element has dimensions
    const rect = element.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) {
        return false;
    }

    // Check if element is within viewport
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight;
    const viewportWidth = window.innerWidth || document.documentElement.clientWidth;

    const verticalInView = rect.top <= viewportHeight && rect.bottom >= 0;
    const horizontalInView = rect.left <= viewportWidth && rect.right >= 0;

    return verticalInView && horizontalInView;
}

export function getElementText(element: Element, maxLength: number = 200): string {
    if (!element) return '';

    const text = element.textContent?.trim() || '';

    if (text.length <= maxLength) {
        return text;
    }

    return text.substring(0, maxLength) + '...';
}

export function findInteractiveElement(element: Element): Element | null {
    let current: Element | null = element;

    while (current) {
        // Check if element is interactive
        if (isInteractiveElement(current)) {
            return current;
        }

        // Move to parent
        current = current.parentElement;
    }

    return null;
}

export function isInteractiveElement(element: Element): boolean {
    const interactiveTags = ['A', 'BUTTON', 'INPUT', 'SELECT', 'TEXTAREA', 'LABEL'];

    // Check tag name
    if (interactiveTags.includes(element.tagName)) {
        return true;
    }

    // Check ARIA role
    const role = element.getAttribute('role');
    if (role && ['button', 'link', 'menuitem', 'tab', 'checkbox', 'radio'].includes(role)) {
        return true;
    }

    // Check for event handlers
    if (element.hasAttribute('onclick') ||
        element.hasAttribute('onchange') ||
        element.hasAttribute('onsubmit')) {
        return true;
    }

    // Check for data-track attribute
    if (element.hasAttribute('data-track') ||
        element.hasAttribute('data-testid') ||
        element.hasAttribute('data-qa')) {
        return true;
    }

    return false;
}
