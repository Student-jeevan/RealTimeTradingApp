import axios from 'axios';

// Prefer explicit environment configuration, fall back to current origin
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  import.meta.env.REACT_APP_API_URL ||
  window.location.origin;

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export default api;