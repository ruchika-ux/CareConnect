import axios from 'axios';

export const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
const KEY = 'careconnect-session';

export const loadSession = () => { try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; } };
export const saveSession = s => localStorage.setItem(KEY, JSON.stringify(s));
export const clearSession = () => localStorage.removeItem(KEY);

const http = axios.create({ baseURL: API });

http.interceptors.request.use(cfg => {
  const s = loadSession();
  if (s?.token) cfg.headers.Authorization = `Bearer ${s.token}`;
  return cfg;
});

// An expired/invalid token sends the user back to sign-in.
http.interceptors.response.use(r => r, err => {
  if (err.response?.status === 401 && loadSession()) {
    clearSession();
    window.location.assign('/login');
  }
  return Promise.reject(err);
});

export const errorMessage = err =>
  err.response?.data?.message ||
  (err.response ? 'Something went wrong. Please try again.' : 'Cannot reach the server. Is the backend running?');

export const api = {
  get: (url, params) => http.get(url, { params }).then(r => r.data),
  post: (url, body, params) => http.post(url, body, { params }).then(r => r.data),
  put: (url, body, params) => http.put(url, body, { params }).then(r => r.data),
};
