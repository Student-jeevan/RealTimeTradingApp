import React from 'react'
import { useForm } from 'react-hook-form'
import {
    Form,
    FormField,
    FormItem,
    FormControl,
} from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useDispatch } from 'react-redux'
import { register } from '@/State/Auth/Action'

function SignupForm() {
    const dispatch = useDispatch();
    const form = useForm({
        defaultValues: {
            fullName: "",
            email: "",
            password: "",
        }
    });

    const onSubmit = (data) => {
        dispatch(register(data));
        console.log(data);
    }

    return (
        <div>
            <h1 className='text-xl font-bold text-center pb-3'>Create New Account</h1>
            <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-6'>
                    <FormField
                        control={form.control}
                        name="fullName"
                        render={({ field }) => (
                            <FormItem>
                                <FormControl>
                                    <Input {...field} className='border w-full border-gray-700 p-5' placeholder="Full Name" />
                                </FormControl>
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="email"
                        render={({ field }) => (
                            <FormItem>
                                <FormControl>
                                    <Input {...field} className='border w-full border-gray-700 p-5' placeholder="user@gmail.com" />
                                </FormControl>
                            </FormItem>
                        )}
                    />
                    <FormField
                        control={form.control}
                        name="password"
                        render={({ field }) => (
                            <FormItem>
                                <FormControl>
                                    <Input type="password" {...field} className='border w-full border-gray-700 p-5' placeholder="Your Password" />
                                </FormControl>
                            </FormItem>
                        )}
                    />
                    <Button type='submit' className='w-full py-5'>
                        Submit
                    </Button>
                </form>
            </Form>
        </div>
    )
}

export default SignupForm
