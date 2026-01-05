// Error names for categorization
export const ERROR_NAMES = {
    // JavaScript errors
    TYPE_ERROR: 'TypeError',
    REFERENCE_ERROR: 'ReferenceError',
    SYNTAX_ERROR: 'SyntaxError',
    RANGE_ERROR: 'RangeError',
    URI_ERROR: 'URIError',
    EVAL_ERROR: 'EvalError',

    // Network errors
    NETWORK_ERROR: 'NetworkError',
    ABORT_ERROR: 'AbortError',
    TIMEOUT_ERROR: 'TimeoutError',

    // Custom errors
    CONSOLE_ERROR: 'ConsoleError',
    UNHANDLED_REJECTION: 'UnhandledRejection',
    WINDOW_ERROR: 'WindowError'
} as const;

// Event metadata keys
export const METADATA_KEYS = {
    // Error
    LINE_NUMBER: 'lineNumber',
    COLUMN_NUMBER: 'columnNumber',
    FILENAME: 'fileName',
    STACK_TRACE: 'stackTrace',

    // Network
    REQUEST_URL: 'requestUrl',
    STATUS_CODE: 'statusCode',
    METHOD: 'method',
    RESPONSE_TIME: 'responseTime',

    // User action
    ELEMENT_TYPE: 'elementType',
    ELEMENT_ID: 'elementId',
    ELEMENT_CLASS: 'elementClass',
    XPATH: 'xPath',
    TEXT_CONTENT: 'textContent',
    COORDINATES: 'coordinates',

    // Performance
    VALUE: 'value',
    LOAD_TIME: 'loadTime',
    DOM_READY_TIME: 'domReadyTime',
    FCP: 'fcp',
    LCP: 'lcp',
    FID: 'fid',
    CLS: 'cls'
} as const;

export const DEFAULT_EVENT_METADATA = {
    userAgent: navigator.userAgent,
    url: window.location.href,
    timestamp: () => new Date().toISOString()
};