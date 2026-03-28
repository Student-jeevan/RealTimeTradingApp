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
import { API_BASE_URL as baseURL } from "../../config/api";
import { toast } from 'sonner';

// ========================
// Device ID helper
// ========================
const getOrCreateDeviceId = () => {
  let deviceId = localStorage.getItem("deviceId");
  if (!deviceId) {
    deviceId = crypto.randomUUID();
    localStorage.setItem("deviceId", deviceId);
  }
  return deviceId;
};

/**
 * Extracts a user-friendly error message from an axios error object.
 */
const extractErrorMessage = (error) => {
  if (error.response && error.response.data) {
    if (typeof error.response.data === 'string') return error.response.data;
    if (error.response.data.message) return error.response.data.message;
    if (error.response.data.error) return error.response.data.error;
  }
  return error.message || "An unexpected network error occurred.";
};

// ========================
// SIGNUP
// ========================

export const register = (userData) => async (dispatch) => {
  dispatch({ type: REGISTER_REQUEST });
  try {
    const response = await axios.post(`${baseURL}/auth/signup`, userData);
    dispatch({ type: REGISTER_SUCCESS, payload: null });
    toast.success("Account created! Please check your email for the OTP.");
    return true;
  } catch (error) {
    const errorMsg = extractErrorMessage(error);
    dispatch({ type: REGISTER_FAILURE, payload: errorMsg });
    toast.error(errorMsg);
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
    toast.success("Authentication successful!");
    return true;
  } catch (error) {
    const errorMsg = extractErrorMessage(error);
    dispatch({ type: VERIFY_SIGNUP_OTP_FAILURE, payload: errorMsg });
    toast.error(errorMsg);
    return false;
  }
}

// ========================
// LOGIN (with deviceId)
// ========================

export const login = (userData, navigate) => async (dispatch) => {
  dispatch({ type: LOGIN_REQUEST });
  try {
    const deviceId = getOrCreateDeviceId();
    const response = await axios.post(`${baseURL}/auth/signin`, {
      ...userData,
      deviceId,
    });

    if (response.data.twoFactorAuthEnabled) {
      // New device → OTP required
      dispatch({
        type: LOGIN_TWO_STEP_SUCCESS,
        payload: {
          sessionId: response.data.session,
          sessionEmail: response.data.session, // backend returns email as session
        }
      });
      toast.info("New device detected. OTP sent to your email.");
      return;
    }

    // Trusted device → direct JWT
    dispatch({ type: LOGIN_SUCCESS, payload: response.data.jwt });
    localStorage.setItem("jwt", response.data.jwt);
    await dispatch(getUser(response.data.jwt));
    toast.success("Login successful!");
    if (navigate) navigate("/");
  } catch (error) {
    const errorMsg = extractErrorMessage(error);
    dispatch({ type: LOGIN_FAILURE, payload: errorMsg });
    toast.error(errorMsg);
  }
};

// ========================
// VERIFY LOGIN OTP (new device → trust it)
// ========================

export const verifyLoginOtp = (data, navigate) => async (dispatch) => {
  dispatch({ type: LOGIN_TWO_STEP_REQUEST });
  try {
    const deviceId = getOrCreateDeviceId();
    const response = await axios.post(`${baseURL}/auth/login/verify-otp`, {
      email: data.email,
      otp: data.otp,
      deviceId,
    });

    dispatch({ type: LOGIN_SUCCESS, payload: response.data.jwt });
    localStorage.setItem("jwt", response.data.jwt);
    await dispatch(getUser(response.data.jwt));
    toast.success("Login successful! Device trusted.");
    if (navigate) navigate("/");
  } catch (error) {
    const errorMsg = extractErrorMessage(error);
    dispatch({ type: LOGIN_TWO_STEP_FAILURE, payload: errorMsg });
    toast.error(errorMsg);
  }
};

// ========================
// RESEND OTP
// ========================

export const resendOtp = (email) => async (dispatch) => {
  try {
    await axios.post(`${baseURL}/auth/resend-otp`, { email });
    toast.success("OTP resent to your email.");
  } catch (error) {
    const errorMsg = extractErrorMessage(error);
    toast.error(errorMsg);
  }
};

// ========================
// GET USER / LOGOUT / CLEAR ERROR
// ========================

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
    const errorMsg = extractErrorMessage(error);
    dispatch({ type: GET_USER_FAILURE, payload: errorMsg });
    if (error.response?.status === 401) {
      toast.error("Session expired. Please login again.");
      dispatch(logout());
    }
  }
};

export const logout = (navigate) => (dispatch) => {
  localStorage.removeItem("jwt");
  // Keep deviceId so returned logins remain trusted
  dispatch({ type: LOGOUT });
  toast.success("Logged out successfully");
  if (navigate) navigate("/signin");
};

export const clearAuthError = () => (dispatch) => {
  dispatch({ type: CLEAR_AUTH_ERROR });
};
