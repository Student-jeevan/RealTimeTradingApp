import {
  FETCH_COIN_LIST_REQUEST,
  FETCH_COIN_LIST_SUCCESS,
  FETCH_COIN_LIST_FAILURE,

  FETCH_TOP_50_COIN_REQUEST,
  FETCH_TOP_50_COIN_SUCCESS,
  FETCH_TOP_50_COIN_FAILURE,

  FETCH_MARKET_CHART_REQUEST,
  FETCH_MARKET_CHART_SUCCESS,
  FETCH_MARKET_CHART_FAILURE,

  FETCH_COIN_BY_ID_REQUEST,
  FETCH_COIN_BY_ID_SUCCESS,
  FETCH_COIN_BY_ID_FAILURE,

  FETCH_COIN_DETAILS_REQUEST,
  FETCH_COIN_DETAILS_SUCCESS,
  FETCH_COIN_DETAILS_FAILURE,

  SEARCH_COIN_REQUEST,
  SEARCH_COIN_SUCCESS,
  SEARCH_COIN_FAILURE
} from "./ActionType";
const initalState = {
    coinList:[],
    top50: [],
    searchCoinList:[],
    marketChart: {data: [] , loading:false},
    coinById:null,
    coinDetails:null,
    loading:false,
    error:null,
};

const coinReducer  = (state = initalState, action)=>{
    switch(action.type){
        case FETCH_COIN_LIST_REQUEST:
        case FETCH_COIN_BY_ID_REQUEST:
        case FETCH_COIN_DETAILS_REQUEST:
        case SEARCH_COIN_REQUEST:
        case FETCH_TOP_50_COIN_REQUEST:
            return  {...state , loading: true , error:null};
        case FETCH_MARKET_CHART_REQUEST:
            return {
                ...state,
                marketChart: {loading: true , data:[]},
                error:null,
            };
        case FETCH_COIN_LIST_SUCCESS:
            return {
                ...state,
                coinList: action.payload,
                loading: false,
                error:null,
            };
        case FETCH_TOP_50_COIN_SUCCESS:
            return{
                ...state,
                top50:action.payload,
                loading: false,
                error:null,
            };
        case FETCH_MARKET_CHART_SUCCESS:
            return {
                ...state,
                marketChart: {data:action.payload.prices || [] , loading:false},
                error: null,
            };
        case FETCH_MARKET_CHART_FAILURE:
            return {
                ...state,
                marketChart: {data: [], loading: false},
                error: action.payload
            };
        case SEARCH_COIN_SUCCESS:
            return {
                ...state,
                searchCoinList: action.payload.coins,
                loading: false,
                error:null,
            };
        case FETCH_COIN_DETAILS_SUCCESS:
            return {
                ...state,
                coinDetails: action.payload,
                marketChart: {loading: false, data: []},
                loading: false,
                error: null,
            }
        case FETCH_COIN_LIST_FAILURE:
        case SEARCH_COIN_FAILURE:
        case FETCH_COIN_BY_ID_FAILURE:
        case FETCH_COIN_DETAILS_FAILURE:
        case FETCH_TOP_50_COIN_FAILURE:
            return {
                ...state,
                loading: false,
                error: action.payload
            };
        default:
            return state;

        
    }
};
export default coinReducer;
