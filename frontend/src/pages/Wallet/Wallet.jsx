import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogTrigger,DialogContent,DialogHeader,DialogTitle } from '@/components/ui/dialog'
import { WalletIcon , CopyIcon ,RefreshCw, DollarSign, UploadIcon, ShuffleIcon, LucideShuffle} from 'lucide-react'
import TopupForm from './TopupForm'
import Withdrawal from './WithdrawalForm'
import TransferForm from './TransferForm'
import { AvatarIcon, UpdateIcon } from '@radix-ui/react-icons'
import { Avatar } from '@radix-ui/react-avatar'
import { AvatarFallback } from '@/components/ui/avatar'
import { useDispatch, useSelector} from 'react-redux'
import { depositMoney, getUserWallet } from '@/State/Wallet/Action'
import { useEffect } from "react";
import { useNavigate, useLocation } from 'react-router-dom'
function useQuery(){
    return new URLSearchParams(useLocation().search);

}

function Wallet() {

    const dispatch=useDispatch();
    const {wallet} = useSelector(store=>store)
    const query = useQuery();
    const orderId = query.get("order_id");
    const paymentId = query.get("payment_id");
    const razorpayPaymentId = query.get('razorpay_payment_id');
    const navigate = useNavigate();
    useEffect(()=>{
        if(orderId){
            dispatch(depositMoney({jwt:localStorage.getItem("jwt"),
                orderId,
                paymentId: razorpayPaymentId || paymentId,
                navigate
            }))
        }

    },[orderId , paymentId , razorpayPaymentId]);
    useEffect(()=>{
        handleFetchUserWallet();
    },[])
    const handleFetchUserWallet=()=>{
        dispatch(getUserWallet(localStorage.getItem("jwt")));
    }
    return (
        <div className='flex flex-col items-center'>
            <div className='pt-10 w-full lg:w-[60%]'>
                 <Card>
                    <CardHeader className='pb-9'>
                        <div className='flex justify-between items-center'>
                            <div className='flex items-center gap-5'>
                                <WalletIcon size={30} />
                                <div>
                                    <CardTitle className='text-2xl'>My Wallet</CardTitle>
                                    <div className='flex items-center gap-2'>
                                        <p className='text-gray-200 text-sm'>{wallet.userWallet?.id}</p>
                                        <CopyIcon size={15}  className='cursor-pointer hover:text-slate-300'/>
                                    </div>
                                </div>
                            </div>
                            <div>
                                <RefreshCw onClick={handleFetchUserWallet} className='w-6 h-6 cursor-pointer hover:text-gray-400' />
                            </div>
                        </div>
                    </CardHeader>
                    <CardContent>
                        <div className='flex items-center'>
                            <DollarSign/>
                            <span className='text-2xl font-semibold'>{wallet.userWallet.balance}</span>
                        </div>
                        <div className='flex gap-7 mt-5'>
                            <Dialog>
                                <DialogTrigger>
                                    <div className='h-24 w-24 hover:text-gray-400 cursor-pointer flex flex-col items-center justify-center rounded-md shadow-slate-800 shadow-md'>
                                         <UploadIcon/>
                                        <span className='text-sm mt-2'>Add Money</span>
                                    </div>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle>
                                            Top UP Your Wallet
                                        </DialogTitle>
                                    </DialogHeader>
                                    <TopupForm/>
                                </DialogContent>
                            </Dialog>
                            <Dialog>
                                <DialogTrigger>
                                    <div className='h-24 w-24 hover:text-gray-400 cursor-pointer flex flex-col items-center justify-center rounded-md shadow-slate-800 shadow-md'>
                                         <UploadIcon/>
                                        <span className='text-sm mt-2'>Withdrawal</span>
                                    </div>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle>
                                            Request Withdrawal
                                        </DialogTitle>
                                    </DialogHeader>
                                    <Withdrawal/>
                                </DialogContent>
                            </Dialog>
                            <Dialog>
                                <DialogTrigger>
                                    <div className='h-24 w-24 hover:text-gray-400 cursor-pointer flex flex-col items-center justify-center rounded-md shadow-slate-800 shadow-md'>
                                         <ShuffleIcon/>
                                        <span className='text-sm mt-2'>Transfer</span>
                                    </div>
                                </DialogTrigger>
                                <DialogContent>
                                    <DialogHeader>
                                        <DialogTitle className='text-center text-xl'>
                                            Transfer to other wallet
                                        </DialogTitle>
                                    </DialogHeader>
                                    <TransferForm/>
                                </DialogContent>
                            </Dialog>
                        </div>
                    </CardContent>
                 </Card>
                 <div className='py-5 pt-10'>
                    <div className='flex gap-2 items-center pb-5'>
                        <h1 className='text-2xl font-semibold'>History</h1>
                        <UpdateIcon className='h-7 w-7 p-0 cursor-pointer hover:text-gray-400' />
                    </div>
                    <div className='space-y-5'>
                            {[1,1,1,1,1,1,1].map((item , i)=><div key={i}>
                                <Card className=' px-5 flex justify-between items-center p-2'>
                                    <div className='flex items-center gap-5 '>
                                        <Avatar>
                                            <AvatarFallback>
                                                <LucideShuffle/>
                                            </AvatarFallback>
                                        </Avatar>
                                        <div className='space-y-1'>
                                            <h1>Buy Asset</h1>
                                            <p className='text-sm text-gray-500'>2025-08-18</p>
                                        </div>
                                    </div>
                                    <div className=''>
                                        <p className='text-green-500'>998USD</p>
                                    </div>
                                </Card>
                            </div>)}
                    </div>
                 </div>
            </div>
        </div>
    )
}

export default Wallet
