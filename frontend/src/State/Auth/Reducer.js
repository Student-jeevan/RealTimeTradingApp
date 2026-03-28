import {
    REGISTER_REQUEST, REGISTER_SUCCESS, REGISTER_FAILURE,
    LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE,
    LOGIN_TWO_STEP_REQUEST, LOGIN_TWO_STEP_SUCCESS, LOGIN_TWO_STEP_FAILURE,
    GET_USER_REQUEST, GET_USER_SUCCESS, GET_USER_FAILURE,
    LOGOUT, CLEAR_AUTH_ERROR,
    VERIFY_SIGNUP_OTP_REQUEST, VERIFY_SIGNUP_OTP_FAILURE
} from "./ActionTypes";

const initialState = {
    user: null,
    loading: false,
    error: null,
    jwt: null,
    twoFactorAuthEnabled: false,
    sessionId: null,
    sessionEmail: null,
}

const authReducer = (state = initialState, action) => {
    switch (action.type) {
        case REGISTER_REQUEST:
        case LOGIN_REQUEST:
        case LOGIN_TWO_STEP_REQUEST:
        case GET_USER_REQUEST:
        case VERIFY_SIGNUP_OTP_REQUEST:
            return { ...state, loading: true, error: null };

        case LOGIN_SUCCESS:
            return {
                ...state, loading: false, error: null,
                jwt: action.payload,
                twoFactorAuthEnabled: false,
                sessionId: null,
                sessionEmail: null,
            };

        case REGISTER_SUCCESS:
            return { ...state, loading: false, error: null };

        case LOGIN_TWO_STEP_SUCCESS:
            return {
                ...state, loading: false, error: null,
                twoFactorAuthEnabled: true,
                sessionId: action.payload.sessionId,
                sessionEmail: action.payload.sessionEmail,
            };

        case GET_USER_SUCCESS:
            return { ...state, user: action.payload, loading: false, error: null };

        case REGISTER_FAILURE:
        case LOGIN_FAILURE:
        case LOGIN_TWO_STEP_FAILURE:
        case GET_USER_FAILURE:
        case VERIFY_SIGNUP_OTP_FAILURE:
            return { ...state, loading: false, error: action.payload };

        case LOGOUT:
            return { ...initialState };

        case CLEAR_AUTH_ERROR:
            return { ...state, error: null };

        default:
            return state;
    }
}

export default authReducer;