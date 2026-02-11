import { REGISTER_REQUEST, REGISTER_SUCCESS, REGISTER_FAILURE, LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE, LOGIN_TWO_STEP_REQUEST, LOGIN_TWO_STEP_SUCCESS, LOGIN_TWO_STEP_FAILURE, GET_USER_REQUEST, GET_USER_SUCCESS, GET_USER_FAILURE, LOGOUT, CLEAR_AUTH_ERROR } from "./ActionTypes";
const intialState = {
    user: null,
    loading: false,
    error: null,
    jwt: null
}
const authRedcuer = (state = intialState, action) => {
    switch (action.type) {
        case REGISTER_REQUEST:
        case LOGIN_REQUEST:
        case GET_USER_REQUEST:
            return { ...state, loading: true, error: null };
        case REGISTER_SUCCESS:
        case LOGIN_SUCCESS:
            return { ...state, loading: false, error: null, jwt: action.payload };
        case LOGIN_TWO_STEP_SUCCESS:
            return { ...state, loading: false, twoFactorAuthEnabled: true, sessionId: action.payload };
        case GET_USER_SUCCESS:
            return { ...state, user: action.payload, loading: false, error: null };
        case REGISTER_FAILURE:
        case LOGIN_FAILURE:
        case GET_USER_FAILURE:
            return { ...state, loading: false, error: action.payload };
        case LOGOUT:
            return { ...intialState }
        case CLEAR_AUTH_ERROR:
            return { ...state, error: null }
        default:
            return state;
    }
}
export default authRedcuer;