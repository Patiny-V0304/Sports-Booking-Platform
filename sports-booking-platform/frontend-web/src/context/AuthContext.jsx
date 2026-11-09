import { createContext, useContext, useState, useEffect } from 'react';
import axiosClient from '../api/axiosClient';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(() => {
        const saved = localStorage.getItem('userInfo');
        return saved ? JSON.parse(saved) : null;
    });

    const login = async (phoneOrEmail, password) => {
        const res = await axiosClient.post('/auth/login', { phoneOrEmail, password });
        const authData = res.data;
        localStorage.setItem('accessToken', authData.accessToken);
        localStorage.setItem('userInfo', JSON.stringify(authData));
        setUser(authData);
        return authData;
    };

    const register = async (payload) => {
        const res = await axiosClient.post('/auth/register', payload);
        const authData = res.data;
        localStorage.setItem('accessToken', authData.accessToken);
        localStorage.setItem('userInfo', JSON.stringify(authData));
        setUser(authData);
        return authData;
    };

    const logout = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('userInfo');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);