import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { DotFilledIcon } from '@radix-ui/react-icons'
import { Input } from '@/components/ui/input'
import React from 'react'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { useDispatch } from 'react-redux'
import { paymentHandler } from '@/State/Wallet/Action'

function TopupForm() {
    const [amount, setAmount] = React.useState('')
    const handleChange = (e) => {
        setAmount(e.target.value)
    }
    const [paymentMethod, setPaymentMethod] = React.useState('RAZORPAY');
    const dispatch = useDispatch();
    const handlePaymentMethodChange = (value) => {
        setPaymentMethod(value);
    }
    const handleSubmit = () => {
      console.log(amount, paymentMethod);
      dispatch(paymentHandler({jwt:localStorage.getItem("jwt"),paymentMethod, amount}))
    }
    return (
        <div className='pt-10 space-y-5'>
            <div>
                <h1 className='pb-1'>Enter Amount</h1>
                <Input onChange={handleChange} value={amount}    className='py-7 text-lg ' placeholder="$9999" />
            </div>
            <div>
                <h1 className='pb-1' >Select Payment method</h1>
                <RadioGroup onValueChange={(value)=>handlePaymentMethodChange(value)} className='flex' default="RAZORPAY">
                    <div className='flex items-center space-x-2 border p-3 px-5'>
                        <RadioGroupItem icon={DotFilledIcon} className='h-9 w-9' value="RAZORPAY" id="r1" />
                         <Label htmlFor="r1">
                            <div className='bg-white rounded-md px-5 py-2 w-32'>
                                <img src= "https://upload.wikimedia.org/wikipedia/commons/thumb/8/89/Razorpay_logo.svg/1200px-Razorpay_logo.svg.png" />

                            </div>
                         </Label>
                    </div>
                    <div className='flex items-center space-x-2 border p-3 px-5'>
                        <RadioGroupItem icon={DotFilledIcon} className='h-9 w-9' value="STRIPE" id="r2" />
                         <Label htmlFor="r2">
                            <div className='bg-white rounded-md px-5  w-32'>
                                <img src= "https://upload.wikimedia.org/wikipedia/commons/thumb/b/ba/Stripe_Logo%2C_revised_2016.svg/2560px-Stripe_Logo%2C_revised_2016.svg.png" className='h-9' />

                            </div>
                         </Label>
                    </div>
                </RadioGroup>
            </div>
           <Button onClick={handleSubmit} className='w-full py-7'>

            Submit
            </Button>
        </div>
    )
}

export default TopupForm
