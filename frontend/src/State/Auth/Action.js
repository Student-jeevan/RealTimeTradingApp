import { REGISTER_REQUEST,REGISTER_SUCCESS,REGISTER_FAILURE,LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE, GET_USER_REQUEST, GET_USER_FAILURE } from "./ActionTypes";
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

export const login=(userData)=> async(dispatch)=>{
    dispatch({type:LOGIN_REQUEST})
    const baseURL="http://localhost:8080"
    try{
        const response = await axios.post(`${baseURL}/auth/signin`, userData);
        const user = response.data;
        console.log(user);
        dispatch({type:LOGIN_SUCCESS, payload:user.jwt});
        localStorage.setItem("jwt" , user.jwt);
    }
    catch(error){
        dispatch({type:LOGIN_FAILURE, payload:error.message});
        console.log(error);
    }
} 
export const getUser=(jwt)=> async(dispatch)=>{
    dispatch({type:GET_USER_REQUEST})
    const baseURL="http://localhost:8080"
    try{
        const response = await axios.get(`${baseURL}/api/users/profile`,{
            headers:{
                Authorization:`Bearer ${jwt}`
            }
        });
        const user = response.data;
        console.log(user);
        dispatch({type:GET_USER_REQUEST, payload:user});
    }
    catch(error){
        dispatch({type:GET_USER_FAILURE, payload:error.message});
        console.log(error);
    }
}