
import React from 'react'
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
import { useDispatch } from 'react-redux'
import { login } from '@/State/Auth/Action'
function SiginForm() {
    const dispatch = useDispatch();
    const form = useForm({
        resolver:"",
        defaultValues:{
            email:"",
            password:"",
        }
    })
    const onSubmit=(data)=>{
        dispatch(login(data));
        console.log(data);
    }
    return (
        <div>
            <h1 className='text-xl font-bold text-center pb-3'>Login</h1>
            <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-6'>
                    <FormField
                       control={form.control}
                        name="email"
                        render={({ field }) => (
                            <FormItem>
                            <FormControl>
                                <Input  className='border w-full border-gray-700 p-5'  placeholder="user@gmail.com" {...field} />
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
                                <Input  className='border w-full border-gray-700 p-5'  placeholder="your password" {...field} />
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

export default SiginForm
