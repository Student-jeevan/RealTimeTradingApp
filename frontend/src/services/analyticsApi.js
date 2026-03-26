import api from '@/config/api';

/**
 * Analytics API service.
 * All endpoints require JWT in the Authorization header.
 */

const getAuthHeader = () => {
  const jwt = localStorage.getItem('jwt');
  return { headers: { Authorization: `Bearer ${jwt}` } };
};

export const fetchPortfolioMetrics = async () => {
  const { data } = await api.get('/api/analytics/portfolio', getAuthHeader());
  return data;
};

export const fetchTradePerformance = async () => {
  const { data } = await api.get('/api/analytics/performance', getAuthHeader());
  return data;
};

export const fetchRiskMetrics = async () => {
  const { data } = await api.get('/api/analytics/risk', getAuthHeader());
  return data;
};

export const fetchAssetAllocation = async () => {
  const { data } = await api.get('/api/analytics/allocation', getAuthHeader());
  return data;
};
