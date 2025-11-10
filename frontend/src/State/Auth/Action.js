import { REGISTER_REQUEST,REGISTER_SUCCESS,REGISTER_FAILURE,LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE, GET_USER_REQUEST, GET_USER_SUCCESS, GET_USER_FAILURE , LOGOUT} from "./ActionTypes";
import axios from 'axios';
export const register=(userData)=> async(dispatch)=>{
    dispatch({type:REGISTER_REQUEST})
    const baseURL="http://localhost:8080"
    try{
        const response = await axios.post(`${baseURL}/auth/signup`, userData);
        const user = response.data;
        console.log(user);
        dispatch({type:REGISTER_SUCCESS, payload:user.jwt});
        localStorage.setItem("jwt" , user.jwt);
    }
    catch(error){
        dispatch({type:REGISTER_FAILURE, payload:error.message});
        console.log(error);
    }
}

export const login = (userData, navigate) => async (dispatch) => {
    dispatch({ type: LOGIN_REQUEST });
    const baseURL = "http://localhost:8080";

    try {
        
        const response = await axios.post(`${baseURL}/auth/signin`, userData, {
            headers: { "Content-Type": "application/json" },
        });

        const user = response.data;
        console.log(user);

        dispatch({ type: LOGIN_SUCCESS, payload: user.jwt });
        localStorage.setItem("jwt", user.jwt);

        // Fetch user data after successful login
        await dispatch(getUser(user.jwt));
      
        // Navigate to home page after successful login
        if (navigate) navigate("/"); 
    } catch (error) {
        dispatch({ 
            type: LOGIN_FAILURE, 
            payload: error.response?.data?.message || error.message 
        });
        console.log(error.response?.data || error.message);
    }
};

export const getUser=(jwt)=> async(dispatch)=>{
    dispatch({type:GET_USER_REQUEST})
    const baseURL="http://localhost:8080"
    const token = jwt || localStorage.getItem("jwt");
    if(!token) return;
    try{
        const response = await axios.get(`${baseURL}/api/users/profile`,{
            headers:{
                Authorization:`Bearer ${token}`
            }
        });
        const user = response.data;
        console.log(user);
        dispatch({type:GET_USER_SUCCESS, payload:user});
    }
    catch(error){
        dispatch({type:GET_USER_FAILURE, payload:error.message});
        console.log(error);
    }

}
export const logout = (navigate) => (dispatch) => {
    localStorage.clear();
    dispatch({type:LOGOUT});
    if (navigate) navigate("/signin");
}