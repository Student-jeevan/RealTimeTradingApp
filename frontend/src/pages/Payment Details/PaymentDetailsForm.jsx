
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
import { addPaymentDetails } from '@/State/Withdrawal/Action'
function PaymentDetailsForm() {
    const useDispatch = useDispatch()
    const form = useForm({
        resolver:"",
        defaultValues:{
            accountHolderName:"",
            ifsc:"",
            accountNumber:"",
            bankName:""
        }
    })
    const onSubmit=(data)=>{
        console.log(data);
        dispatchEvent(addPaymentDetails({
            paymentDetails: data, jwt: localStorage.getItem("jwt");
        }));
        console.log(data);
    }
    return (
        <div className='px-10 py-2'>
            <Form {...form}>
                <form onSubmit={form.handleSubmit(onSubmit)} className='space-y-6'>
                     <FormField
                       control={form.control}
                        name="accountHolderName"
                        render={({ field }) => (
                            <FormItem>
                            <FormLabel>Account Holder Name</FormLabel>
                            <FormControl>
                                <Input name='accountHolderName' className='border w-full border-gray-700 p-5'  placeholder="Jeevan" {...field} />
                            </FormControl>
                            </FormItem>
                       )}
                   />
                    <FormField
                       control={form.control}
                        name="ifsc"
                        render={({ field }) => (
                            <FormItem>
                            <FormLabel>IFSC Code</FormLabel>
                            <FormControl>
                                <Input  className='border w-full border-gray-700 p-5'  placeholder="SBI45450" {...field} />
                            </FormControl>
                            </FormItem>
                       )}
                   />
                   <FormField
                       control={form.control}
                        name="accountNumber"
                        render={({ field }) => (
                            <FormItem>
                            <FormLabel>Account Number</FormLabel>
                            <FormControl>
                                <Input  className='border w-full border-gray-700 p-5'  placeholder="*********7878" {...field} />
                            </FormControl>
                            </FormItem>
                       )}
                   />
                    <FormField
                       control={form.control}
                        name="confirmAccountNumber"
                        render={({ field }) => (
                            <FormItem>
                            <FormLabel>Confirm Account Number</FormLabel>
                            <FormControl>
                                <Input  className='border w-full border-gray-700 p-5'  placeholder="Confirm Accoun Number" {...field} />
                            </FormControl>
                            </FormItem>
                       )}
                   />
                    <FormField
                       control={form.control}
                        name="bankName"
                        render={({ field }) => (
                            <FormItem>
                            <FormLabel>Bank Name</FormLabel>
                            <FormControl>
                                <Input  className='border w-full border-gray-700 p-5'  placeholder="Yes Bank" {...field} />
                            </FormControl>
                            </FormItem>
                       )}
                   />
                  <DialogClose className='w-full'>
                     <Button type='submit' className='w-full py-5'>
                     Submit
                   </Button>
                  </DialogClose>
                </form>
            </Form>
        </div>
    )
}

export default PaymentDetailsForm
