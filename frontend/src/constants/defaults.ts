export const EVENT_TYPES = {
    ERROR: 'ERROR',
    ACTION: 'ACTION',
    NETWORK: 'NETWORK',
    PERFORMANCE: 'PERFORMANCE',
    CUSTOM: 'CUSTOM',
    SESSION_START: 'SESSION_START',
    SESSION_END: 'SESSION_END'
} as const;

export const ERROR_LEVELS = {
    FATAL: 'fatal',
    ERROR: 'error',
    WARNING: 'warning',
    INFO: 'info',
    DEBUG: 'debug'
} as const;

export const DEFAULT_MAX_BUFFER_SIZE = 50;
export const DEFAULT_FLUSH_INTERVAL = 5000; // 0 = disabled (only flush on ACTION/ERROR or manual flush)

export const DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000; // 30 minutes

export const BUTTON_POSITIONS = ['top-left', 'top-right', 'bottom-left', 'bottom-right'] as const;

export const PERFORMANCE_METRICS = {
    LOAD_TIME: 'load_time',
    DOM_READY_TIME: 'dom_ready_time',
    FIRST_CONTENTFUL_PAINT: 'first_contentful_paint',
    LARGEST_CONTENTFUL_PAINT: 'largest_contentful_paint',
    FIRST_INPUT_DELAY: 'first_input_delay',
    CUMULATIVE_LAYOUT_SHIFT: 'cumulative_layout_shift'
} as const;

export const TRACKED_STATUS_CODES = [400, 401, 403, 404, 408, 409, 429, 500, 502, 503, 504];

export const DEFAULT_TRACKED_ACTIONS = ['click', 'submit', 'change', 'keydown'];

export const STORAGE_KEYS = {
    SESSION: 'bt_session',
    SESSION_ID: 'bt_session_id',
    SESSION_START: 'bt_session_start',
    LAST_ACTIVITY: 'bt_last_activity'
} as const;