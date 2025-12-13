
import React, { useState } from 'react'
import { useForm } from 'react-hook-form'
import {
    Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormDescription,
  FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { DialogClose } from '@/components/ui/dialog'
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSeparator,
  InputOTPSlot,
} from "@/components/ui/input-otp"
import api from '@/config/api'
import { useNavigate } from 'react-router-dom'

function ForgotPasswordForm() {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [otpSent, setOtpSent] = useState(false);
    const [sessionToken, setSessionToken] = useState('');
    const [otp, setOtp] = useState('');
    const [email, setEmail] = useState('');

    const form = useForm({
        resolver:"",
        defaultValues:{
            email:""
        }
    })

    const passwordForm = useForm({
        resolver:"",
        defaultValues:{
            password:"",
            confirmPassword:""
        }
    })

    const onSubmit=async (data)=>{
        try {
            setIsLoading(true);
            setError('');
            setMessage('');
            console.log('Sending forgot password request:', data);
            
            const requestBody = {
                sendTo: data.email,
                verificationType: 'EMAIL'
            };

            const response = await api.post('/auth/users/reset-password/send-otp', requestBody);
            console.log('Response received:', response.data);
            setMessage(response.data.message || 'Password reset OTP sent successfully');
            setSessionToken(response.data.session || '');
            setEmail(data.email);
            setOtpSent(true);
        } catch (error) {
            console.error('Error sending forgot password request:', error);
            setError(error.response?.data?.message || error.message || 'Failed to send reset password OTP');
        } finally {
            setIsLoading(false);
        }
    }

    const onOtpSubmit=async (data)=>{
        try {
            setIsLoading(true);
            setError('');
            setMessage('');
            
            if(data.password !== data.confirmPassword) {
                setError('Passwords do not match');
                setIsLoading(false);
                return;
            }

            if(!otp || otp.length !== 6) {
                setError('Please enter a valid 6-digit OTP');
                setIsLoading(false);
                return;
            }

            console.log('Verifying OTP and resetting password');

            const requestBody = {
                otp: otp,
                password: data.password
            };

            const response = await api.patch(`/auth/users/reset-password/verify-otp?id=${sessionToken}`, requestBody);
            
            console.log('Password reset successful:', response.data);
            setMessage('Password reset successfully! Redirecting to login...');
            setTimeout(() => {
                navigate('/signin');
            }, 2000);
        } catch (error) {
            console.error('Error resetting password:', error);
            setError(error.response?.data?.message || error.message || 'Failed to reset password');
        } finally {
            setIsLoading(false);
        }
    }

    if(otpSent) {
        return (
            <div>
                <h1 className='text-xl font-bold text-center pb-3'>Verify OTP & Reset Password</h1>
                <p className='text-sm text-center text-gray-400 mb-4'>OTP sent to {email}</p>
                <Form {...passwordForm}>
                    <form onSubmit={passwordForm.handleSubmit(onOtpSubmit)} className='space-y-6'>
                        <div className='space-y-4'>
                            <div>
                                <label className='text-sm font-medium mb-2 block'>Enter OTP</label>
                                <div className='flex justify-center'>
                                    <InputOTP value={otp} onChange={(value)=>setOtp(value)} maxLength={6}>
                                        <InputOTPGroup>
                                            <InputOTPSlot index={0} />
                                            <InputOTPSlot index={1} />
                                            <InputOTPSlot index={2} />
                                        </InputOTPGroup>
                                        <InputOTPSeparator />
                                        <InputOTPGroup>
                                            <InputOTPSlot index={3} />
                                            <InputOTPSlot index={4} />
                                            <InputOTPSlot index={5} />
                                        </InputOTPGroup>
                                    </InputOTP>
                                </div>
                            </div>
                            <FormField
                                control={passwordForm.control}
                                name="password"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormControl>
                                            <Input type="password" className='border w-full border-gray-700 p-5' placeholder="Enter new password" {...field} />
                                        </FormControl>
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={passwordForm.control}
                                name="confirmPassword"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormControl>
                                            <Input type="password" className='border w-full border-gray-700 p-5' placeholder="Confirm new password" {...field} />
                                        </FormControl>
                                    </FormItem>
                                )}
                            />
                        </div>
                        {error && <div className='text-red-500 text-sm text-center'>{error}</div>}
                        {message && <div className='text-green-500 text-sm text-center'>{message}</div>}
                        <Button type='submit' className='w-full py-5' disabled={isLoading}>
                            {isLoading ? 'Resetting...' : 'Reset Password'}
                        </Button>
                        <Button 
                            type='button' 
                            variant="ghost" 
                            className='w-full' 
                            onClick={() => {
                                setOtpSent(false);
                                setOtp('');
                                setSessionToken('');
                                setMessage('');
                                setError('');
                            }}
                        >
                            Back to Email
                        </Button>
                    </form>
                </Form>
            </div>
        )
    }

    return (
        <div>
             <h1 className='text-xl font-bold text-center pb-3'>Forgot password</h1>
            <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-6'>
                    <FormField
                       control={form.control}
                        name="email"
                        render={({ field }) => (
                            <FormItem>
                            <FormControl>
                                <Input  className='border w-full border-gray-700 p-5'  placeholder="enter your email" {...field} />
                            </FormControl>
                            </FormItem>
                       )}
                   />
                   {error && <div className='text-red-500 text-sm text-center'>{error}</div>}
                   {message && <div className='text-green-500 text-sm text-center'>{message}</div>}
                     <Button type='submit' className='w-full py-5' disabled={isLoading}>
                     {isLoading ? 'Sending...' : 'Submit'}
                   </Button>
                 
                </form>
            </Form>
        </div>
    )
}

export default ForgotPasswordForm
