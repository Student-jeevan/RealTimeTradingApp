import React, { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import {
    Form,
    FormField,
    FormItem,
    FormControl,
    FormMessage,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import {
    InputOTP,
    InputOTPGroup,
    InputOTPSlot,
} from "@/components/ui/input-otp"
import { Button } from '@/components/ui/button'
import { useDispatch, useSelector } from 'react-redux'
import { register, verifySignupOtp } from '@/State/Auth/Action'
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';

function SignupForm() {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { auth } = useSelector(store => store);
    const [isOtpSent, setIsOtpSent] = useState(false);
    const [otp, setOtp] = useState("");

    const form = useForm({
        defaultValues: {
            fullName: "",
            email: "",
            password: "",
        }
    });

    useEffect(() => {
        if (auth.error) {
            toast.error(auth.error);
        }
    }, [auth.error]);


    const onSubmit = (data) => {
        dispatch(register(data)).then((success) => {
            if (success) setIsOtpSent(true);
        });
        console.log(data);
    }

    const handleVerifyOtp = () => {
        dispatch(verifySignupOtp({ email: form.getValues("email"), otp })).then((success) => {
            // If verifySignupOtp returns true, navigation is handled there or here. 
            // In Action.js I updated verifySignupOtp to return true on success.
            // Action.js: dispatch(LOGIN_SUCCESS...) -> updates auth.user.
            // App.jsx redirects if auth.user is present.
            // But let's check action.js again: "if (navigate) navigate("/");" logic was in login. in verifySignupOtp I didn't pass navigate, but I can.
            // Let's pass navigate just in case, or rely on App.jsx state change.
            // Actually, I didn't pass navigate in my previous Action.js edit. 
            // Let's rely on the promise return.
            if (success) navigate("/");
        });
    }

    return (
        <div>
            <h1 className='text-xl font-bold text-center pb-3'>
                {isOtpSent ? "Verify OTP" : "Create New Account"}
            </h1>

            {isOtpSent ? (
                <div className="flex flex-col items-center gap-4">
                    <p className="text-sm text-gray-400">Enter the OTP sent to {form.getValues("email")}</p>
                    <InputOTP
                        maxLength={6}
                        value={otp}
                        onChange={(value) => setOtp(value)}
                    >
                        <InputOTPGroup>
                            <InputOTPSlot index={0} />
                            <InputOTPSlot index={1} />
                            <InputOTPSlot index={2} />
                            <InputOTPSlot index={3} />
                            <InputOTPSlot index={4} />
                            <InputOTPSlot index={5} />
                        </InputOTPGroup>
                    </InputOTP>

                    {auth.error && (
                        <div className="text-red-500 text-sm mt-2 text-center">
                            {typeof auth.error === 'string' ? auth.error : "Invalid OTP"}
                        </div>
                    )}

                    <Button onClick={handleVerifyOtp} className='w-full py-5'>
                        Verify
                    </Button>
                    <Button variant="ghost" onClick={() => setIsOtpSent(false)}>
                        Back
                    </Button>
                </div>
            ) : (
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-6'>
                        <FormField
                            control={form.control}
                            name="fullName"
                            render={({ field }) => (
                                <FormItem>
                                    <FormControl>
                                        <Input className='border w-full border-gray-700 p-5' placeholder="Full Name" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="email"
                            render={({ field }) => (
                                <FormItem>
                                    <FormControl>
                                        <Input className='border w-full border-gray-700 p-5' placeholder="Email" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <FormField
                            control={form.control}
                            name="password"
                            render={({ field }) => (
                                <FormItem>
                                    <FormControl>
                                        <Input type="password" className='border w-full border-gray-700 p-5' placeholder="Password" {...field} />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        <Button type='submit' className='w-full py-5'>
                            Register
                        </Button>
                    </form>
                </Form>
            )}
        </div>
    )
}

export default SignupForm
