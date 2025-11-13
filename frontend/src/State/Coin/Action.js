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