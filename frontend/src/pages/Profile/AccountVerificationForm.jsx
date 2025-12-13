import React, { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose
} from "@/components/ui/dialog"
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSeparator,
  InputOTPSlot,
} from "@/components/ui/input-otp"
import { Button } from "@/components/ui/button"
import { useSelector, useDispatch } from 'react-redux'
import api from '@/config/api'
import { getUser } from '@/State/Auth/Action'

function AccountVerificationForm({ handleSumbit, handleSubmit }) {
    // Support both prop names (handleSumbit is a typo but matches Profile.jsx)
    const onEnableTwoFactorCallback = handleSumbit || handleSubmit;
    const auth = useSelector(state => state.auth);
    const dispatch = useDispatch();
    const [otp, setOtp] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [isSendingOtp, setIsSendingOtp] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [otpSent, setOtpSent] = useState(false);
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const handleSendOtp = async () => {
        try {
            setIsSendingOtp(true);
            setError('');
            setMessage('');
            const jwt = localStorage.getItem("jwt");
            
            if (!jwt) {
                setError('Please log in to send OTP');
                return;
            }

            const response = await api.post('/api/users/verification/EMAIL/send-otp', {}, {
                headers: {
                    'Authorization': `Bearer ${jwt}`
                }
            });
            
            console.log('OTP sent:', response.data);
            setMessage('OTP sent successfully to your email');
            setOtpSent(true);
            setIsDialogOpen(true);
        } catch (error) {
            console.error('Error sending OTP:', error);
            setError(error.response?.data?.message || error.message || 'Failed to send OTP');
        } finally {
            setIsSendingOtp(false);
        }
    }
    
    const handleVerifyOtp = async () => {
        try {
            if (!otp || otp.length !== 6) {
                setError('Please enter a valid 6-digit OTP');
                return;
            }

            setIsLoading(true);
            setError('');
            setMessage('');
            const jwt = localStorage.getItem("jwt");
            
            if (!jwt) {
                setError('Please log in to verify OTP');
                return;
            }

            const response = await api.patch(`/api/users/enable-two-factor/verify-otp/${otp}`, {}, {
                headers: {
                    'Authorization': `Bearer ${jwt}`
                }
            });
            
            console.log('Two-factor authentication enabled:', response.data);
            setMessage('Two-factor authentication enabled successfully!');
            
            // Update user in Redux store
            await dispatch(getUser(jwt));
            
            // Call parent callback if provided
            if (onEnableTwoFactorCallback) {
                onEnableTwoFactorCallback();
            }
            
            // Close dialog after successful verification
            setTimeout(() => {
                setIsDialogOpen(false);
                setOtp('');
                setOtpSent(false);
                setMessage('');
            }, 2000);
        } catch (error) {
            console.error('Error verifying OTP:', error);
            setError(error.response?.data?.message || error.message || 'Failed to verify OTP');
        } finally {
            setIsLoading(false);
        }
    } 

    return (
        <div className='flex justify-center'>
            <div className='space-y-5 mt-10 w-full'>
                <div className='flex justify-between items-center'>
                    <p>Email: </p>
                    <p className='text-gray-500'>{auth.user?.email || 'Loading...'}</p>
                    <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                        <DialogTrigger asChild>
                            <Button onClick={handleSendOtp} disabled={isSendingOtp}>
                                {isSendingOtp ? 'Sending...' : otpSent ? 'Resend OTP' : 'Send OTP'}
                            </Button>
                        </DialogTrigger>
                        <DialogContent>  
                            <DialogHeader>
                                <DialogTitle>Enter OTP</DialogTitle>
                                <DialogDescription>
                                    Enter the 6-digit OTP sent to {auth.user?.email}
                                </DialogDescription>
                            </DialogHeader>
                            <div className='py-5 space-y-4'>
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
                                {error && <div className='text-red-500 text-sm text-center'>{error}</div>}
                                {message && <div className='text-green-500 text-sm text-center'>{message}</div>}
                                <div className='flex justify-center gap-2'>
                                    <Button 
                                        onClick={handleVerifyOtp} 
                                        disabled={isLoading || !otp || otp.length !== 6}
                                        className="w-[10rem]"
                                    >
                                        {isLoading ? 'Verifying...' : 'Verify & Enable'}
                                    </Button>
                                    <DialogClose asChild>
                                        <Button variant="outline">Cancel</Button>
                                    </DialogClose>
                                </div>
                            </div>
                        </DialogContent>
                    </Dialog>
                </div>
            </div>
        </div>
    )
}

export default AccountVerificationForm