import axios from 'axios';

// TEMP: hardcode backend URL for production EC2
export const API_BASE_URL = 'http://13.49.225.93:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export default api;