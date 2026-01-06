import { ApiResponse, Event, Project, Report, Session, User } from '../types/types';

class ApiClient {
    private readonly baseUrl: string;
    private token: string | null;

    constructor() {
        this.baseUrl = 'http://localhost:8080';
        this.token = localStorage.getItem('token');
    }

    private async request<T>(
        endpoint: string,
        options: RequestInit = {}
    ): Promise<T> {
        const headers = this.prepareHeaders(options.headers);

        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }

        try {
            const response = await fetch(`${this.baseUrl}${endpoint}`, {
                ...options,
                headers,
                // КРИТИЧЕСКО ВАЖНО: добавьте credentials для работы с CORS
                credentials: 'include',  // <-- ДОБАВЬТЕ ЭТУ СТРОКУ
            });

            this.logRequest(options.method || 'GET', endpoint, response.status);

            if (response.status === 401) {
                this.handleUnauthorized();
                throw new Error('Session expired');
            }

            if (!response.ok) {
                await this.handleErrorResponse(response);
            }

            // Обновляем токен из заголовка ответа, если он есть
            const newToken = response.headers.get('Authorization');
            if (newToken && newToken.startsWith('Bearer ')) {
                const tokenValue = newToken.replace('Bearer ', '');
                this.setToken(tokenValue);
            }

            if (response.status === 204 || response.headers.get('content-length') === '0') {
                return null as T;
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else {
                return {} as T;
            }
        } catch (error) {
            console.error(`API request error (${endpoint}):`, error);
            if (error instanceof TypeError && error.message.includes('Failed to fetch')) {
                console.error('CORS or network error detected. Check backend CORS configuration.');
            }
            throw error;
        }
    }

    private prepareHeaders(inputHeaders?: HeadersInit): Record<string, string> {
        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
        };

        if (!inputHeaders) return headers;

        if (inputHeaders instanceof Headers) {
            inputHeaders.forEach((value, key) => {
                headers[key] = value;
            });
        } else if (Array.isArray(inputHeaders)) {
            inputHeaders.forEach(([key, value]) => {
                headers[key] = value;
            });
        } else {
            Object.entries(inputHeaders).forEach(([key, value]) => {
                headers[key] = value as string;
            });
        }

        return headers;
    }

    private logRequest(method: string, endpoint: string, status: number): void {
        console.log(`🌐 ${method} ${endpoint}`, status);
    }

    private handleUnauthorized(): void {
        this.logout();
        window.location.href = '/login';
    }

    private async handleErrorResponse(response: Response): Promise<never> {
        let errorData = {};

        try {
            errorData = await response.json();
        } catch {
            // Ignore JSON parse errors
        }

        const errorMessage =
            (errorData as any).message ||
            (errorData as any).error ||
            Object.values(errorData).join(', ') ||
            `HTTP error ${response.status}`;

        throw new Error(errorMessage);
    }

    setToken(token: string): void {
        this.token = token;
        localStorage.setItem('token', token);
    }

    logout(): void {
        this.token = null;
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    }

    // ============ АВТОРИЗАЦИЯ ============
    async login(username: string, password: string) {
        const result = await this.request<{
            token: string;
            username: string;
            role: string;
            userId?: string;
        }>('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password }),
        });

        // Сохраняем токен, если он пришел в ответе
        if (result.token) {
            this.setToken(result.token);
        }

        return result;
    }

    async register(username: string, password: string) {
        return this.request<{ message: string; userId?: string }>('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, password }),
        });
    }

    // ============ ПРОЕКТЫ ============
    async getMyProjects(): Promise<Project[]> {
        const data = await this.request<Project[]>('/api/my/projects');
        return Array.isArray(data) ? data : [];
    }

    async getAllProjects(): Promise<Project[]> {
        const data = await this.request<Project[]>('/api/projects');
        return Array.isArray(data) ? data : [];
    }

    async createProject(data: { projectName: string }): Promise<{ message: string; id: string }> {
        return this.request('/api/projects', {
            method: 'POST',
            body: JSON.stringify(data),
        });
    }

    async updateProject(projectId: string, data: { projectName: string }): Promise<{ message: string; id: string }> {
        return this.request(`/api/projects/${projectId}`, {
            method: 'PATCH',
            body: JSON.stringify(data),
        });
    }

    async deleteProject(projectId: string): Promise<{ message: string }> {
        return this.request(`/api/projects/${projectId}`, {
            method: 'DELETE',
        });
    }

    // ============ ОТЧЕТЫ (REPORTS) ============
    async getProjectReports(projectId: string, page: number = 0, size: number = 30): Promise<ApiResponse<Report>> {
        return this.request(`/api/reports/byProject/${projectId}?page=${page}&size=${size}`);
    }

    async getReport(reportId: number): Promise<Report> {
        return this.request(`/api/reports/${reportId}`);
    }

    async deleteReport(reportId: number): Promise<void> {
        return this.request(`/api/reports/${reportId}`, {
            method: 'DELETE',
        });
    }

    async updateReportDashboard(reportId: number, data: {
        status?: string;
        criticality?: string;
        comments?: string;
        projectId?: string;
        developerName?: string;
    }): Promise<Report> {
        return this.request(`/api/reports/${reportId}/dashboard`, {
            method: 'PATCH',
            body: JSON.stringify(data),
        });
    }

    // ============ СЕССИИ ============
    async getSession(sessionId: string): Promise<Session> {
        try {
            return await this.request<Session>(`/api/sessions/${sessionId}`);
        } catch (err) {
            console.error(`Failed to load session ${sessionId}:`, err);
            throw err;
        }
    }

    // ============ СОБЫТИЯ (EVENTS) ============
    async getSessionEvents(sessionId: string): Promise<Event[]> {
        return this.request(`/api/events/session/${sessionId}`);
    }

    // ============ АДМИН - ПОЛЬЗОВАТЕЛИ ============
    async getAllUsers(page: number = 0, size: number = 30): Promise<{
        pageNumber: number;
        size: number;
        last: boolean;
        totalPages: number;
        pageSize: number;
        page: number;
        content: User[];
        first: boolean;
        totalElements: number;
    }> {
        const response = await this.request<{
            content: User[];
            pageable: {
                pageNumber: number;
                pageSize: number;
                sort: any;
            };
            totalPages: number;
            totalElements: number;
            last: boolean;
            first: boolean;
            number: number;
            size: number;
            sort: any;
            numberOfElements: number;
            empty: boolean;
        }>(`/api/admin/users?page=${page}&size=${size}`);

        return {
            page: 0,
            size: 0,
            content: response.content,
            totalPages: response.totalPages,
            totalElements: response.totalElements,
            pageNumber: response.number,
            pageSize: response.size,
            last: response.last,
            first: response.first
        };
    }

    async updateUserRole(userId: string, role: string): Promise<User> {
        const validRoles = ['ADMIN', 'PM', 'DEVELOPER'];
        const roleUpper = role.toUpperCase();

        if (!validRoles.includes(roleUpper)) {
            throw new Error(`Invalid role. Must be one of: ${validRoles.join(', ')}`);
        }

        return this.request(`/api/admin/users/${userId}/role?role=${roleUpper}`, {
            method: 'PATCH',
        });
    }

    // ============ НАЗНАЧЕНИЕ ПРОЕКТОВ ============
    async assignUserToProject(userId: string, projectId: string): Promise<User> {
        return this.request(`/api/users/${userId}/projects/assign/${projectId}`, {
            method: 'PATCH',
        });
    }

    async removeUserFromProject(userId: string, projectId: string): Promise<User> {
        return this.request(`/api/users/${userId}/projects/remove/${projectId}`, {
            method: 'PATCH',
        });
    }

    // ============ МЕТОД ДЛЯ ОТЛАДКИ CORS ============
    async testCors(): Promise<string> {
        return this.request('/auth/test-cors', {
            method: 'GET',
        });
    }
}

export const api = new ApiClient();