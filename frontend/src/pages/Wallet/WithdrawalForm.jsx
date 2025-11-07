import { Input } from '@/components/ui/input'
import React from 'react'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { DialogClose } from '@/components/ui/dialog'
function Withdrawal() {
    const [amount, setAmount] = React.useState('')
    const handleChange = (e) => {
        setAmount(e.target.value)
    }
    const [paymentMethod, setPaymentMethod] = React.useState('RAZORPAY');
    const handlePaymentMethodChange = (value) => {
        setPaymentMethod(value);
    }
    const handleSubmit = () => {
      console.log(amount);
    }
    return (
        <div className='pt-10 space-y-5'>
            <div className='flex justify-between items-center rounded-md bg-slate-900 text-xl font-bold px-5 py-4'>
                <p>Available balance</p>
                <p>$90000</p>
            </div>
            <div className='flex flex-col items-center'>
                <h1>Enter Withdrawal amount</h1>

                <div className='flex items-center justify-center'>
                    <Input onChange={handleChange} value={amount} className='withdrawalInput py-7 border-none outline-none focus:outline-none px-0 text-2xl text-center' placeholder="$99999" type="number" />
                </div>
            </div>
            <div>
                <p className='pb-2'>Transfer to</p>
                <div className='flex items-center gap-5 border px-5 py-2  rounded-md'>
                    <img src="https://cdn-icons-png.flaticon.com/512/8327/8327746.png" className='h-8 w-8' />
                     <div>
                        <p className='text-xl font-bold'>Yes Bank</p>
                       <p className='text-xs'>
                         **********7778
                       </p>
                     </div>

                </div>
            </div>
            <DialogClose className='w-full'>
                <Button onClick={handleSubmit} className='w-full py-7 text-xl'>
                   Withdraw
               </Button>
            </DialogClose>
        </div>
    )
}

export default Withdrawal
