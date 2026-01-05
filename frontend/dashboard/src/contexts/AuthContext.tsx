import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, UserRole } from '../types/types';
import { api } from '../api/Api';

interface AuthContextType {
    user: User | null;
    loading: boolean;
    login: (username: string, password: string) => Promise<boolean>;
    register: (username: string, password: string) => Promise<boolean>;
    logout: () => void;
    isAdmin: () => boolean;
    isPM: () => boolean;
    isDeveloper: () => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

interface AuthProviderProps {
    children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const userData = localStorage.getItem('user');
        const token = localStorage.getItem('token');

        console.log('Auth init:', { userData, token });

        if (userData && token) {
            try {
                const parsedUser = JSON.parse(userData);
                console.log('Parsed user:', parsedUser);

                if (parsedUser.role) {
                    parsedUser.role = parsedUser.role.toUpperCase();
                }
                setUser(parsedUser);
            } catch (e) {
                console.error('Error parsing user data:', e);
                localStorage.removeItem('user');
                localStorage.removeItem('token');
            }
        }
        setLoading(false);
    }, []);

    const login = async (username: string, password: string): Promise<boolean> => {
        try {
            console.log('Login attempt:', username);
            const response = await api.login(username, password);
            console.log('Login response:', response);
            console.log('Response role:', response.role, 'Type:', typeof response.role);

            api.setToken(response.token);

            const role = response.role.toUpperCase() as UserRole;
            console.log('Normalized role:', role);

            if (!Object.values(UserRole).includes(role)) {
                console.error('Invalid role received:', role);
            }

            const userId = String(response.userId);
            console.log('User ID:', userId, 'Type:', typeof userId);

            const userData: User = {
                id: userId,
                username: response.username,
                role: role,
            };

            console.log('Saving user:', userData);
            setUser(userData);
            localStorage.setItem('user', JSON.stringify(userData));
            return true;
        } catch (error) {
            console.error('Login error:', error);
            return false;
        }
    };

    const register = async (username: string, password: string): Promise<boolean> => {
        try {
            await api.register(username, password);
            return true;
        } catch (error) {
            console.error('Register error:', error);
            return false;
        }
    };

    const logout = () => {
        console.log('Logging out');
        api.logout();
        setUser(null);
        localStorage.removeItem('user');
    };

    const isAdmin = () => {
        const result = user?.role === UserRole.ADMIN;
        console.log('isAdmin check:', { userRole: user?.role, result });
        return result;
    };

    const isPM = () => {
        const result = user?.role === UserRole.PM;
        console.log('isPM check:', { userRole: user?.role, result });
        return result;
    };

    const isDeveloper = () => {
        const result = user?.role === UserRole.DEVELOPER;
        console.log('isDeveloper check:', { userRole: user?.role, result });
        return result;
    };

    return (
        <AuthContext.Provider value={{
            user,
            loading,
            login,
            register,
            logout,
            isAdmin,
            isPM,
            isDeveloper,
        }}>
            {children}
        </AuthContext.Provider>
    );
};