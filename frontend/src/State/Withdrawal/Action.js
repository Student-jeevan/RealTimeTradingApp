import api from "@/config/api";
import {
  WITHDRAWAL_REQUEST,
  WITHDRAWAL_SUCCESS,
  WITHDRAWAL_FAILURE,
  WITHDRAWAL_PROCEED_REQUEST,
  WITHDRAWAL_PROCEED_SUCCESS,
  WITHDRAWAL_PROCEED_FAILURE,
  GET_WITHDRAWAL_HISTORY_REQUEST,
  GET_WITHDRAWAL_HISTORY_SUCCESS,
  GET_WITHDRAWAL_HISTORY_FAILURE,
  GET_WITHDRAWAL_REQUEST_REQUEST,
  GET_WITHDRAWAL_REQUEST_SUCCESS,
  GET_WITHDRAWAL_REQUEST_FAILURE,
  ADD_PAYMENT_DETAILS_REQUEST,
  ADD_PAYMENT_DETAILS_SUCCESS,
  ADD_PAYMENT_DETAILS_FAILURE,
  GET_PAYMENT_DETAILS_REQUEST,
  GET_PAYMENT_DETAILS_SUCCESS,
  GET_PAYMENT_DETAILS_FAILURE
} from "./ActionTypes";

/* USER WITHDRAW */
export const withdrawalRequest = ({ amount, jwt }) => async dispatch => {
  dispatch({ type: WITHDRAWAL_REQUEST });
  try {
    const res = await api.post(
      `/api/withdrawal/${amount}`,
      null,
      { headers: { Authorization: `Bearer ${jwt}` } }
    );

    dispatch({ type: WITHDRAWAL_SUCCESS, payload: res.data });
  } catch (err) {
    dispatch({ type: WITHDRAWAL_FAILURE, payload: err.message });
  }
};

/* ADMIN APPROVE */
export const approveWithdrawal = ({ id, jwt }) => async dispatch => {
  dispatch({ type: WITHDRAWAL_PROCEED_REQUEST });
  try {
    const response = await api.post(
      `/admin/withdrawals/${id}/approve`,
      null,
      { headers: { Authorization: `Bearer ${jwt}` } }
    );
    dispatch({
      type: WITHDRAWAL_PROCEED_SUCCESS,
      payload: { ...response.data.withdrawal, status: 'SUCCESS' }
    });
  } catch (error) {
    dispatch({
      type: WITHDRAWAL_PROCEED_FAILURE,
      payload: error.response?.data?.message || error.message
    });
  }
};

/* ADMIN REJECT */
export const rejectWithdrawal = ({ id, jwt }) => async dispatch => {
  dispatch({ type: WITHDRAWAL_PROCEED_REQUEST });
  try {
    const response = await api.post(
      `/admin/withdrawals/${id}/reject`,
      null,
      { headers: { Authorization: `Bearer ${jwt}` } }
    );
    dispatch({
      type: WITHDRAWAL_PROCEED_SUCCESS,
      payload: { ...response.data.withdrawal, status: 'DECLINE' }
    });
  } catch (error) {
    dispatch({
      type: WITHDRAWAL_PROCEED_FAILURE,
      payload: error.response?.data?.message || error.message
    });
  }
};

/* LEGACY: admin approve/reject via old endpoint */
export const proceedWithdrawal = ({ id, jwt, accept }) => async dispatch => {
  if (accept) return dispatch(approveWithdrawal({ id, jwt }));
  return dispatch(rejectWithdrawal({ id, jwt }));
};


/* USER HISTORY */
export const getWithdrawalHistory = jwt => async dispatch => {
  dispatch({ type: GET_WITHDRAWAL_HISTORY_REQUEST });
  try {
    const res = await api.get("/api/withdrawal", {
      headers: { Authorization: `Bearer ${jwt}` }
    });

    dispatch({
      type: GET_WITHDRAWAL_HISTORY_SUCCESS,
      payload: res.data
    });
  } catch (err) {
    dispatch({
      type: GET_WITHDRAWAL_HISTORY_FAILURE,
      payload: err.message
    });
  }
};

/* ADMIN ALL REQUESTS */
export const getAllWithdrawalRequest = jwt => async dispatch => {
  dispatch({ type: GET_WITHDRAWAL_REQUEST_REQUEST });
  try {
    const res = await api.get("/admin/withdrawals", {
      headers: { Authorization: `Bearer ${jwt}` }
    });
    dispatch({ type: GET_WITHDRAWAL_REQUEST_SUCCESS, payload: res.data });
  } catch (err) {
    dispatch({ type: GET_WITHDRAWAL_REQUEST_FAILURE, payload: err.message });
  }
};

/* PAYMENT DETAILS (FIXED EXPORT) */
export const addPaymentDetails = ({ paymentDetails, jwt }) => async dispatch => {
  dispatch({ type: ADD_PAYMENT_DETAILS_REQUEST });
  try {
    const res = await api.post(
      "/api/payment-details",
      paymentDetails,
      { headers: { Authorization: `Bearer ${jwt}` } }
    );

    dispatch({
      type: ADD_PAYMENT_DETAILS_SUCCESS,
      payload: res.data
    });
  } catch (err) {
    dispatch({
      type: ADD_PAYMENT_DETAILS_FAILURE,
      payload: err.message
    });
  }
};

export const getPaymentDetails = ({ jwt }) => async dispatch => {
  dispatch({ type: GET_PAYMENT_DETAILS_REQUEST });
  try {
    const res = await api.get("/api/payment-details", {
      headers: { Authorization: `Bearer ${jwt}` }
    });

    dispatch({
      type: GET_PAYMENT_DETAILS_SUCCESS,
      payload: res.data
    });
  } catch (err) {
    dispatch({
      type: GET_PAYMENT_DETAILS_FAILURE,
      payload: err.message
    });
  }
};
