import { legacy_createStore, combineReducers, applyMiddleware } from 'redux';
import { thunk } from 'redux-thunk';
import authReducer from './Auth/Reducer';
import coinReducer from './Coin/Reducer';

const rootReducer = combineReducers({
  auth: authReducer,
  coin: coinReducer
});

export const store = legacy_createStore(rootReducer, applyMiddleware(thunk));
