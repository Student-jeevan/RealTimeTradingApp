import { Input } from '@/components/ui/input'
import React from 'react'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { DialogClose } from '@/components/ui/dialog'
import { useDispatch, useSelector } from 'react-redux'
import { withdrawalRequest } from '@/State/Withdrawal/Action'
import { getUserWallet } from '@/State/Wallet/Action';
import { toast } from 'sonner'

function Withdrawal() {

    const [amount, setAmount] = React.useState('')
    const dispatch = useDispatch();
    const { withdrawal, wallet } = useSelector(store => store);

    const handleChange = (e) => {
        setAmount(e.target.value)
    }

    const [paymentMethod, setPaymentMethod] = React.useState('RAZORPAY');

    const handlePaymentMethodChange = (value) => {
        setPaymentMethod(value);
    }

    // ✅ Mask account number except last 4 digits
    const maskAccountNumber = (num = "") => {
        if (!num) return "";
        const last4 = num.slice(-4);
        return "*".repeat(num.length - 4) + last4;
    };

    const handleSubmit = () => {
        if (wallet.userWallet?.balance < Number(amount)) {
            // Ideally use toast here, but relying on backend error message prop logic for display if implemented, 
            // but user requested "Show error message...". 
            // I will let the backend fail based on the requirement "Validate: If withdrawal amount > wallet balance -> return error". 
            // But I can also block it here.
            // "Prevent withdrawal submission when balance is insufficient." -> OK, I will block here.
            toast.error("Insufficient balance");
            return;
        }
        dispatch(withdrawalRequest({ amount, jwt: localStorage.getItem("jwt") }));
    }

    React.useEffect(() => {
        dispatch(getUserWallet(localStorage.getItem("jwt")));
    }, []);

    React.useEffect(() => {
        if (withdrawal.error) {
            // Error handling is managed by store state, maybe show a message
        }
    }, [withdrawal.error])


    return (
        <div className='pt-10 space-y-5'>
            <div className='flex justify-between items-center rounded-md bg-slate-900 text-xl font-bold px-5 py-4'>
                <p>Available balance</p>
                <p>${wallet.userWallet?.balance}</p>
            </div>

            <div className='flex flex-col items-center'>
                <h1>Enter Withdrawal amount</h1>

                <div className='flex items-center justify-center'>
                    <Input
                        onChange={handleChange}
                        value={amount}
                        className='withdrawalInput py-7 border-none outline-none focus:outline-none px-0 text-2xl text-center'
                        placeholder="$99999"
                        type="number"
                    />
                </div>
            </div>

            <div>
                <p className='pb-2'>Transfer to</p>

                <div className='flex items-center gap-5 border px-5 py-2 rounded-md'>
                    <img
                        src="https://cdn-icons-png.flaticon.com/512/8327/8327746.png"
                        className='h-8 w-8'
                    />
                    <div>
                        <p className='text-xl font-bold'>
                            {withdrawal.paymentDetails?.bankName}
                        </p>

                        <p className='text-xs'>
                            {maskAccountNumber(withdrawal.paymentDetails?.accountNumber)}
                        </p>
                    </div>
                </div>
            </div>

            {withdrawal.error && (
                <div className='text-red-500 text-center font-bold'>
                    {withdrawal.error}
                </div>
            )}

            <DialogClose className='w-full'>
                <Button
                    onClick={handleSubmit}
                    className='w-full py-7 text-xl'
                >
                    Withdraw
                </Button>
            </DialogClose>
        </div>
    )
}

export default Withdrawal
