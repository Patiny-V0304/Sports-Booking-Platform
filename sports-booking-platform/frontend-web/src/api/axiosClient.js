import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

const axiosClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Tự động đính kèm JWT Token vào Header Authorization trước khi gửi Request
axiosClient.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Tự động bóc tách dữ liệu hoặc thông báo lỗi từ ApiResponse của Backend
axiosClient.interceptors.response.use(
    (response) => response.data,
    (error) => {
        const message =
            error.response?.data?.message ||
            error.response?.data?.error ||
            'Không thể kết nối đến máy chủ API Gateway (Cổng 8080)';
        return Promise.reject(new Error(message));
    }
);

export default axiosClient;