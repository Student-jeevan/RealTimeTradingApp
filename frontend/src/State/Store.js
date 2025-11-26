import { legacy_createStore, combineReducers, applyMiddleware } from 'redux';
import { thunk } from 'redux-thunk';
import authReducer from './Auth/Reducer';
import coinReducer from './Coin/Reducer';
import walletReducer from './Wallet/Reducer';
import withdrawalReducer from './Withdrawal/Reducer';
import { withdrawalRequest } from './Withdrawal/Action';
import orderReducer from './Order/Reducer';
import assetReducer from './Asset/Reducer';

const rootReducer = combineReducers({
  auth: authReducer,
  coin: coinReducer,
  wallet: walletReducer,
  withdrawal: withdrawalReducer,
  order: orderReducer,
  asset: assetReducer
});

export const store = legacy_createStore(rootReducer, applyMiddleware(thunk));
