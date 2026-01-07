export enum ReportStatus {
    NEW = 'NEW',
    IN_PROGRESS = 'IN_PROGRESS',
    DONE = 'DONE'
}

export enum CriticalityLevel {
    LOW = 'LOW',
    MEDIUM = 'MEDIUM',
    HIGH = 'HIGH',
    CRITICAL = 'CRITICAL',
    UNKNOWN = 'UNKNOWN'
}

export enum UserRole {
    ADMIN = 'ADMIN',
    PM = 'PM',
    DEVELOPER = 'DEVELOPER'
}

export interface User {
    id: string;
    username: string;
    password?: string;
    role: UserRole;
    projectIds?: string[];
    projectId?: string;
}

export interface Project {
    id: string;
    name: string;
    projectName?: string;
    createdAt?: string;
}

export interface Report {
    id: number;
    title: string;
    comments?: string;
    level: CriticalityLevel;
    currentUrl?: string;
    reportedAt: string;
    screenUrl: string | null;
    status: ReportStatus;
    userEmail?: string;
    userProvided?: boolean;
    developerName?: string;
    projectId: string;
    sessionId?: number;
    createdAt: string;
    updatedAt?: string;
    tags?: string[];
}

export interface Event {
    id: number;
    eventId?: number;
    sessionId?: number;
    type: string;
    name?: string;
    log?: string;
    stackTrace?: string;
    url?: string;
    element?: string;
    timestamp: string;
    fileName?: string;
    lineNumber?: string;
    statusCode?: string;
    metadata?: {
        fileName?: string;
        lineNumber?: string;
        statusCode?: string;
        [key: string]: any;
    };
}

export interface Session {
    id: number;
    projectId?: string;
    userId?: string;
    browser?: string;
    browserVersion?: string;
    cookiesHash?: string;
    deviceType?: string;
    endTime?: string;
    ipAddress?: string;
    isActive?: boolean;
    language?: string;
    os?: string;
    screenResolution?: string;
    startTime?: string;
    userAgent?: string;
    viewportSize?: string;
    createdAt: string;
    plugins?: string[];
}

export interface ApiResponse<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
}