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
import { register, verifySignupOtp, resendOtp } from '@/State/Auth/Action'
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';

function SignupForm() {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { auth } = useSelector(store => store);
    const [isOtpSent, setIsOtpSent] = useState(false);
    const [otp, setOtp] = useState("");
    const [resendCooldown, setResendCooldown] = useState(0);

    const form = useForm({
        defaultValues: {
            fullName: "",
            email: "",
            password: "",
        }
    });

    // Trigger 30s cooldown when OTP screen appears
    useEffect(() => {
        if (isOtpSent) {
            setResendCooldown(30);
        }
    }, [isOtpSent]);

    useEffect(() => {
        if (resendCooldown <= 0) return;
        const timer = setTimeout(() => setResendCooldown(c => c - 1), 1000);
        return () => clearTimeout(timer);
    }, [resendCooldown]);

    const onSubmit = (data) => {
        dispatch(register(data)).then((success) => {
            if (success) setIsOtpSent(true);
        });
    }

    const handleVerifyOtp = () => {
        if (otp.length !== 6) {
            toast.error("Please enter the 6-digit OTP");
            return;
        }
        dispatch(verifySignupOtp({ email: form.getValues("email"), otp })).then((success) => {
            if (success) navigate("/");
        });
    }

    const handleResendOtp = () => {
        if (resendCooldown > 0) return;
        dispatch(resendOtp(form.getValues("email")));
        setResendCooldown(30);
    }

    return (
        <div>
            <h1 className='text-xl font-bold text-center pb-3'>
                {isOtpSent ? "Verify OTP" : "Create New Account"}
            </h1>

            {isOtpSent ? (
                <div className="flex flex-col items-center gap-4">
                    <p className="text-sm text-gray-400 text-center">
                        Enter the OTP sent to <strong>{form.getValues("email")}</strong>
                    </p>

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

                    <Button
                        onClick={handleVerifyOtp}
                        className='w-full py-5'
                        disabled={auth.loading || otp.length < 6}
                    >
                        {auth.loading ? "Verifying..." : "Verify"}
                    </Button>

                    <Button
                        variant="ghost"
                        onClick={handleResendOtp}
                        disabled={resendCooldown > 0}
                        className="text-sm"
                    >
                        {resendCooldown > 0
                            ? `Resend OTP (${resendCooldown}s)`
                            : "Resend OTP"}
                    </Button>

                    <Button variant="ghost" onClick={() => setIsOtpSent(false)}>
                        ← Back
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
                        {auth.error && (
                            <p className="text-sm text-red-500 text-center">{auth.error}</p>
                        )}
                        <Button type='submit' className='w-full py-5' disabled={auth.loading}>
                            {auth.loading ? "Sending OTP..." : "Register"}
                        </Button>
                    </form>
                </Form>
            )}
        </div>
    )
}

export default SignupForm
