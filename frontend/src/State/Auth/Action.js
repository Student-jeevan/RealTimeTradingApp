import {
  REGISTER_REQUEST, REGISTER_SUCCESS, REGISTER_FAILURE,
  LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE,
  GET_USER_REQUEST, GET_USER_SUCCESS, GET_USER_FAILURE,
  LOGOUT
} from "./ActionTypes";
import axios from "axios";
const baseURL = import.meta.env.VITE_API_BASE_URL;
export const register = (userData) => async (dispatch) => {
  dispatch({ type: REGISTER_REQUEST });
  try {
    const response = await axios.post(`${baseURL}/auth/signup`, userData);
    dispatch({ type: REGISTER_SUCCESS, payload: response.data.jwt });
    localStorage.setItem("jwt", response.data.jwt);
  } catch (error) {
    dispatch({ type: REGISTER_FAILURE, payload: error.message });
  }
};

export const login = (userData, navigate) => async (dispatch) => {
  dispatch({ type: LOGIN_REQUEST });
  try {
    const response = await axios.post(`${baseURL}/auth/signin`, userData);
    dispatch({ type: LOGIN_SUCCESS, payload: response.data.jwt });
    localStorage.setItem("jwt", response.data.jwt);
    await dispatch(getUser(response.data.jwt));
    if (navigate) navigate("/");
  } catch (error) {
    dispatch({
      type: LOGIN_FAILURE,
      payload: error.response?.data?.message || error.message,
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
