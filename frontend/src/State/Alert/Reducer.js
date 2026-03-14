import * as types from './ActionTypes';

const initialState = {
    activeAlerts: [],
    triggeredAlerts: [],
    loading: false,
    error: null,
};

const alertReducer = (state = initialState, action) => {
    switch (action.type) {
        case types.CREATE_ALERT_REQUEST:
        case types.GET_ACTIVE_ALERTS_REQUEST:
        case types.GET_TRIGGERED_ALERTS_REQUEST:
        case types.DELETE_ALERT_REQUEST:
            return { ...state, loading: true, error: null };

        case types.CREATE_ALERT_SUCCESS:
            return {
                ...state,
                loading: false,
                activeAlerts: [action.payload, ...state.activeAlerts],
            };

        case types.GET_ACTIVE_ALERTS_SUCCESS:
            return { ...state, loading: false, activeAlerts: action.payload };

        case types.GET_TRIGGERED_ALERTS_SUCCESS:
            return { ...state, loading: false, triggeredAlerts: action.payload };

        case types.DELETE_ALERT_SUCCESS:
            return {
                ...state,
                loading: false,
                activeAlerts: state.activeAlerts.filter((a) => a.id !== action.payload),
            };

        case types.CREATE_ALERT_FAILURE:
        case types.GET_ACTIVE_ALERTS_FAILURE:
        case types.GET_TRIGGERED_ALERTS_FAILURE:
        case types.DELETE_ALERT_FAILURE:
            return { ...state, loading: false, error: action.error };

        default:
            return state;
    }
};

export default alertReducer;
