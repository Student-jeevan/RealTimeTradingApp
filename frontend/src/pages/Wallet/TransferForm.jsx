import React from 'react'
import { Input } from '@/components/ui/input'
import { DialogClose } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { useDispatch, useSelector } from 'react-redux'
import { transferMoney } from '@/State/Wallet/Action'
function TransferForm() {
     const dispatch=useDispatch();
    const {wallet} = useSelector(store=>store)
    const [FormData, setFormData] = React.useState({
        amount: '',
        walletId: '',
        purpose: '',
    })
    const handleChange = (e) => {
        setFormData({
            ...FormData,
            [e.target.name]: e.target.value,
        })
    } 
    const handleSubmit = () => {
      dispatch(transferMoney({
        jwt:localStorage.getItem("jwt"),
        walletId: FormData.walletId,
        reqData:{
            amount: FormData.amount,
            purpose: FormData.purpose,
        }
      }))
      console.log(FormData);
    }
    return (
        <div className='pt-10 space-y-5'>
            <div>
                <h1 className='pb-1'>Enter Amount</h1>
                <Input name="amount" onChange={handleChange} value={FormData.amount} className='py-7' placeholder="$9090990" />

            </div>
            <div>
                <h1 className='pb-1'>Wallet Id</h1>
                <Input name="walletId" onChange={handleChange} value={FormData.walletId} className='py-7' placeholder="#e#4d" />

            </div>
            <div>
                <h1 className='pb-1'>Enter purpose</h1>
                <Input name="purpose" onChange={handleChange} value={FormData.purpose} className='py-7' placeholder="gift for your friend" />

            </div>

            <DialogClose className='w-full'>
                <Button onClick={handleSubmit} className='w-full py-7'>
                     Submit
                </Button>
            </DialogClose>
        </div>
    )
}

export default TransferForm
