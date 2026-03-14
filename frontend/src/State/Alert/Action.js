import api from '@/config/api';
import * as types from './ActionTypes';

const authHeaders = (jwt) => ({ headers: { Authorization: `Bearer ${jwt}` } });

export const createAlert = ({ jwt, coinId, targetPrice, condition }) => async (dispatch) => {
    dispatch({ type: types.CREATE_ALERT_REQUEST });
    try {
        const res = await api.post('/api/alerts', { coinId, targetPrice, condition }, authHeaders(jwt));
        dispatch({ type: types.CREATE_ALERT_SUCCESS, payload: res.data });
        return res.data;
    } catch (err) {
        dispatch({ type: types.CREATE_ALERT_FAILURE, error: err.message });
        throw err;
    }
};

export const getActiveAlerts = (jwt) => async (dispatch) => {
    dispatch({ type: types.GET_ACTIVE_ALERTS_REQUEST });
    try {
        const res = await api.get('/api/alerts/active', authHeaders(jwt));
        dispatch({ type: types.GET_ACTIVE_ALERTS_SUCCESS, payload: res.data });
    } catch (err) {
        dispatch({ type: types.GET_ACTIVE_ALERTS_FAILURE, error: err.message });
    }
};

export const getTriggeredAlerts = (jwt) => async (dispatch) => {
    dispatch({ type: types.GET_TRIGGERED_ALERTS_REQUEST });
    try {
        const res = await api.get('/api/alerts/triggered', authHeaders(jwt));
        dispatch({ type: types.GET_TRIGGERED_ALERTS_SUCCESS, payload: res.data });
    } catch (err) {
        dispatch({ type: types.GET_TRIGGERED_ALERTS_FAILURE, error: err.message });
    }
};

export const deleteAlert = ({ jwt, id }) => async (dispatch) => {
    dispatch({ type: types.DELETE_ALERT_REQUEST });
    try {
        await api.delete(`/api/alerts/${id}`, authHeaders(jwt));
        dispatch({ type: types.DELETE_ALERT_SUCCESS, payload: id });
    } catch (err) {
        dispatch({ type: types.DELETE_ALERT_FAILURE, error: err.message });
        throw err;
    }
};
