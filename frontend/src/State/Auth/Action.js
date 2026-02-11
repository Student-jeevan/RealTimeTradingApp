import {
  REGISTER_REQUEST, REGISTER_SUCCESS, REGISTER_FAILURE,
  LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE,
  GET_USER_REQUEST, GET_USER_SUCCESS, GET_USER_FAILURE,
  LOGOUT,
  CLEAR_AUTH_ERROR,
  VERIFY_SIGNUP_OTP_REQUEST, VERIFY_SIGNUP_OTP_SUCCESS, VERIFY_SIGNUP_OTP_FAILURE,
  LOGIN_TWO_STEP_REQUEST, LOGIN_TWO_STEP_SUCCESS, LOGIN_TWO_STEP_FAILURE
} from "./ActionTypes";
import axios from "axios";
const baseURL = import.meta.env.VITE_API_BASE_URL;
export const register = (userData) => async (dispatch) => {
  dispatch({ type: REGISTER_REQUEST });
  try {
    const response = await axios.post(`${baseURL}/auth/signup`, userData);
    dispatch({ type: REGISTER_SUCCESS, payload: null }); // OTP sent, no JWT yet
    return true;
  } catch (error) {
    dispatch({ type: REGISTER_FAILURE, payload: error.message });
    return false;
  }
};

export const verifySignupOtp = (data) => async (dispatch) => {
  dispatch({ type: VERIFY_SIGNUP_OTP_REQUEST });
  try {
    const response = await axios.post(`${baseURL}/auth/signup/verify`, data);
    dispatch({ type: LOGIN_SUCCESS, payload: response.data.jwt });
    localStorage.setItem("jwt", response.data.jwt);
    dispatch({ type: GET_USER_REQUEST });

    await dispatch(getUser(response.data.jwt));
    // Note: getUser logs internally if fails.
    return true;
  } catch (error) {
    dispatch({ type: VERIFY_SIGNUP_OTP_FAILURE, payload: error.response?.data?.message || error.message });
    return false;
  }
}

export const login = (userData, navigate) => async (dispatch) => {
  dispatch({ type: LOGIN_REQUEST });
  try {
    const response = await axios.post(`${baseURL}/auth/signin`, userData);
    if (response.data.twoFactorAuthEnabled) {
      dispatch({ type: LOGIN_TWO_STEP_SUCCESS, payload: response.data.session });
      return;
    }
    dispatch({ type: LOGIN_SUCCESS, payload: response.data.jwt });
    localStorage.setItem("jwt", response.data.jwt);
    await dispatch(getUser(response.data.jwt));
    if (navigate) navigate("/");
  } catch (error) {
    dispatch({
      type: LOGIN_FAILURE,
      payload: error.response?.data?.message || error.response?.data || error.message,
    });
  }
};

export const verifyLoginOtp = (data, navigate) => async (dispatch) => {
  dispatch({ type: LOGIN_TWO_STEP_REQUEST });
  try {
    const response = await axios.post(`${baseURL}/auth/two-factor/otp/${data.otp}?id=${data.sessionId}`);
    dispatch({ type: LOGIN_SUCCESS, payload: response.data.jwt });
    localStorage.setItem("jwt", response.data.jwt);
    await dispatch(getUser(response.data.jwt));
    if (navigate) navigate("/");
  } catch (error) {
    dispatch({
      type: LOGIN_TWO_STEP_FAILURE,
      payload: error.response?.data?.message || error.response?.data || error.message,
    });
  }
};

export const getUser = (jwt) => async (dispatch) => {
  dispatch({ type: GET_USER_REQUEST });
  const token = jwt || localStorage.getItem("jwt");
  if (!token) return;

  try {
    const response = await axios.get(`${baseURL}/api/users/profile`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    dispatch({ type: GET_USER_SUCCESS, payload: response.data });
  } catch (error) {
    dispatch({ type: GET_USER_FAILURE, payload: error.message });
  }
};

export const logout = (navigate) => (dispatch) => {
  localStorage.clear();
  dispatch({ type: LOGOUT });
  if (navigate) navigate("/signin");
};

export const clearAuthError = () => (dispatch) => {
  dispatch({ type: CLEAR_AUTH_ERROR });
};
