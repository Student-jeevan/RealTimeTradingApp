import api from '@/config/api';
import * as types from './ActionTypes';

export const payOrder = ({ jwt, orderData, amount }) => async (dispatch) => {
    dispatch({ type: types.PAY_ORDER_REQUEST });

    try {
        const response = await api.post('/api/orders/pay', orderData, {
            headers: {
                Authorization: `Bearer ${jwt}`,
            },
        });

        dispatch({
            type: types.PAY_ORDER_SUCCESS,
            payload: response.data,
            amount,
        });

        console.log("order success", response.data);
        return response.data; // Return data for component handling
    } catch (error) {
        console.log("error", error);
        dispatch({
            type: types.PAY_ORDER_FAILURE,
            error: error.message,
        });
        throw error; // Throw error for component handling
    }
};

// -------------------- FIXED BELOW --------------------

export const getAllOrdersForUser =
    ({ jwt, orderType, assetSymbol }) =>
        async (dispatch) => {

            dispatch({ type: types.GET_ALL_ORDER_REQUEST });

            try {
                const response = await api.get('/api/orders', {
                    headers: {
                        Authorization: `Bearer ${jwt}`,
                    },
                    params: {
                        order_type: orderType,
                        asset_symbol: assetSymbol,
                    },
                });

                dispatch({
                    type: types.GET_ALL_ORDER_SUCCESS,
                    payload: response.data,
                });

                console.log("order fetched", response.data);
            } catch (error) {
                console.log("error", error);
                dispatch({
                    type: types.GET_ALL_ORDER_FAILURE,
                    error: error.message,
                });
            }
        };
