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
import { login, clearAuthError, verifyLoginOtp, resendOtp } from '@/State/Auth/Action'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner';

function SigninForm() {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { auth } = useSelector(store => store);
    const [otp, setOtp] = useState("");
    const [resendCooldown, setResendCooldown] = useState(0);

    const form = useForm({
        defaultValues: {
            email: "",
            password: "",
        }
    });

    // Start 30-second resend cooldown when OTP screen is shown
    useEffect(() => {
        if (auth.twoFactorAuthEnabled) {
            setResendCooldown(30);
        }
    }, [auth.twoFactorAuthEnabled]);

    useEffect(() => {
        if (resendCooldown <= 0) return;
        const timer = setTimeout(() => setResendCooldown(c => c - 1), 1000);
        return () => clearTimeout(timer);
    }, [resendCooldown]);

    const onSubmit = (data) => {
        dispatch(login(data, navigate));
    }

    const handleVerifyOtp = () => {
        if (otp.length !== 6) {
            toast.error("Please enter the 6-digit OTP");
            return;
        }
        dispatch(verifyLoginOtp({
            email: auth.sessionEmail,
            otp,
        }, navigate));
    }

    const handleResendOtp = () => {
        if (resendCooldown > 0) return;
        dispatch(resendOtp(auth.sessionEmail));
        setResendCooldown(30);
    }

    return (
        <div>
            <h1 className='text-xl font-bold text-center pb-3'>
                {auth.twoFactorAuthEnabled ? "Verify OTP" : "Login"}
            </h1>

            {auth.twoFactorAuthEnabled ? (
                <div className="flex flex-col items-center gap-4">
                    <p className="text-sm text-gray-400 text-center">
                        New device detected. Enter the 6-digit OTP sent to{" "}
                        <strong>{auth.sessionEmail}</strong>
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
                        {auth.loading ? "Verifying..." : "Verify & Trust Device"}
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
                </div>
            ) : (
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-6'>
                        <FormField
                            control={form.control}
                            name="email"
                            render={({ field }) => (
                                <FormItem>
                                    <FormControl>
                                        <Input
                                            className='border w-full border-gray-700 p-5'
                                            placeholder="user@gmail.com"
                                            {...field}
                                            onChange={(e) => {
                                                field.onChange(e);
                                                if (auth.error) dispatch(clearAuthError());
                                            }}
                                        />
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
                                        <Input
                                            type="password"
                                            className='border w-full border-gray-700 p-5'
                                            placeholder="Your Password"
                                            {...field}
                                            onChange={(e) => {
                                                field.onChange(e);
                                                if (auth.error) dispatch(clearAuthError());
                                            }}
                                        />
                                    </FormControl>
                                    <FormMessage />
                                </FormItem>
                            )}
                        />
                        {auth.error && (
                            <p className="text-sm text-red-500 text-center">{auth.error}</p>
                        )}
                        <Button type='submit' className='w-full py-5' disabled={auth.loading}>
                            {auth.loading ? "Logging in..." : "Login"}
                        </Button>
                    </form>
                </Form>
            )}
        </div>
    )
}

export default SigninForm
