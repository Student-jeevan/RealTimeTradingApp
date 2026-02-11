
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
import {
    InputOTP,
    InputOTPGroup,
    InputOTPSlot,
} from "@/components/ui/input-otp"
import { Button } from '@/components/ui/button'
import { DialogClose } from '@/components/ui/dialog'
import { useDispatch } from 'react-redux'
import { login, clearAuthError, verifyLoginOtp } from '@/State/Auth/Action'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner';
import { useSelector } from 'react-redux';
import { useEffect } from 'react';

function SigninForm() {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { auth } = useSelector(store => store);
    const [otp, setOtp] = useState("");

    const form = useForm({
        resolver: "",
        defaultValues: {
            email: "",
            password: "",
        }
    })

    const onSubmit = (data) => {
        dispatch(login(data, navigate));
        console.log(data);
    }

    const handleVerifyOtp = () => {
        dispatch(verifyLoginOtp({ otp, sessionId: auth.sessionId }, navigate));
    }

    return (
        <div>
            <h1 className='text-xl font-bold text-center pb-3'>
                {auth.twoFactorAuthEnabled ? "Verify OTP" : "Login"}
            </h1>

            {auth.twoFactorAuthEnabled ? (
                <div className="flex flex-col items-center gap-4">
                    <p className="text-sm text-gray-400">Enter the OTP sent to your email</p>
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
                                        <Input className='border w-full border-gray-700 p-5' placeholder="user@gmail.com" {...field}
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
                                        <Input type="password" className='border w-full border-gray-700 p-5' placeholder="Your Password" {...field}
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
                            <div className="text-red-500 text-sm mt-2 text-center">
                                {auth.error}
                            </div>
                        )}
                        <Button type='submit' className='w-full py-5'>
                            Submit
                        </Button>
                    </form>
                </Form>
            )}
        </div>
    )
}

export default SigninForm
