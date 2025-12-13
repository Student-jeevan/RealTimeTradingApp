import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogTrigger,DialogContent,DialogHeader,DialogTitle } from '@/components/ui/dialog'
import { WalletIcon , CopyIcon ,RefreshCw, DollarSign, UploadIcon, ShuffleIcon, LucideShuffle, ArrowDownCircle, ArrowUpCircle, ShoppingCart, TrendingUp, TrendingDown} from 'lucide-react'
import TopupForm from './TopupForm'
import Withdrawal from './WithdrawalForm'
import TransferForm from './TransferForm'
import { AvatarIcon, UpdateIcon } from '@radix-ui/react-icons'
import { Avatar } from '@radix-ui/react-avatar'
import { AvatarFallback } from '@/components/ui/avatar'
import { useDispatch, useSelector} from 'react-redux'
import { depositMoney, getUserWallet, getWalletTransactions } from '@/State/Wallet/Action'
import { useEffect } from "react";
import { useNavigate, useLocation } from 'react-router-dom'
function useQuery(){
    return new URLSearchParams(useLocation().search);

}

function Wallet() {

    const dispatch=useDispatch();
    const wallet = useSelector(store=>store.wallet) || {
        userWallet: {},
        transactions: []
    };
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
            // Refresh transactions after deposit
            setTimeout(() => {
                handleFetchWalletTransaction();
                handleFetchUserWallet();
            }, 1000);
        }

    },[orderId , paymentId , razorpayPaymentId]);
    useEffect(()=>{
        handleFetchUserWallet();
        handleFetchWalletTransaction();
    },[])
    const handleFetchUserWallet=()=>{
        dispatch(getUserWallet(localStorage.getItem("jwt")));
    }
    const handleFetchWalletTransaction = ()=>{
        dispatch(getWalletTransactions({jwt:localStorage.getItem("jwt")}));
    }

    const getTransactionIcon = (type) => {
        switch(type) {
            case 'ADD_MONEY':
                return <ArrowDownCircle className='text-green-500' size={24} />;
            case 'WITHDRAWAL':
                return <ArrowUpCircle className='text-red-500' size={24} />;
            case 'WALLET_TRANSFER':
                return <ShuffleIcon className='text-blue-500' size={24} />;
            case 'BUY_ASSET':
                return <ShoppingCart className='text-orange-500' size={24} />;
            case 'SELL_ASSET':
                return <TrendingUp className='text-green-500' size={24} />;
            default:
                return <LucideShuffle className='text-gray-500' size={24} />;
        }
    }

    const getTransactionTypeLabel = (type) => {
        switch(type) {
            case 'ADD_MONEY':
                return 'Add Money';
            case 'WITHDRAWAL':
                return 'Withdrawal';
            case 'WALLET_TRANSFER':
                return 'Wallet Transfer';
            case 'BUY_ASSET':
                return 'Buy Asset';
            case 'SELL_ASSET':
                return 'Sell Asset';
            default:
                return type || 'Transaction';
        }
    }

    const getTransactionAmountColor = (type) => {
        // Transactions that add money (green)
        if (type === 'ADD_MONEY' || type === 'SELL_ASSET') {
            return 'text-green-500';
        }
        // Transactions that deduct money (red)
        if (type === 'WITHDRAWAL' || type === 'BUY_ASSET') {
            return 'text-red-500';
        }
        // Neutral transactions (blue)
        return 'text-blue-500';
    }

    const formatDate = (dateString) => {
        if (!dateString) return '';
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'short', 
                day: 'numeric' 
            });
        } catch (e) {
            return dateString;
        }
    }

    const formatAmount = (amount, type) => {
        if (!amount) return '0';
        const sign = (type === 'ADD_MONEY' || type === 'SELL_ASSET') ? '+' : 
                     (type === 'WITHDRAWAL' || type === 'BUY_ASSET') ? '-' : '';
        return `${sign}${amount}`;
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
                            <span className='text-2xl font-semibold'>{wallet?.userWallet?.balance ?? 0}</span>
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
                        <UpdateIcon onClick={handleFetchWalletTransaction} className='h-7 w-7 p-0 cursor-pointer hover:text-gray-400' />
                    </div>
                    <div className='space-y-5'>
                        {wallet?.transactions && wallet.transactions.length > 0 ? (
                            wallet.transactions.map((item, i) => (
                                <Card key={item.id || i} className='px-5 flex justify-between items-center p-4 hover:bg-gray-800 transition-colors'>
                                    <div className='flex items-center gap-5 flex-1'>
                                        <div className='flex items-center justify-center w-12 h-12 rounded-full bg-gray-800'>
                                            {getTransactionIcon(item.type)}
                                        </div>
                                        <div className='space-y-1 flex-1'>
                                            <h1 className='font-semibold text-lg'>
                                                {getTransactionTypeLabel(item.type)}
                                            </h1>
                                            {item.purpose && (
                                                <p className='text-sm text-gray-400'>{item.purpose}</p>
                                            )}
                                            <p className='text-sm text-gray-500'>
                                                {formatDate(item.date)}
                                            </p>
                                        </div>
                                    </div>
                                    <div className='text-right'>
                                        <p className={`font-bold text-lg ${getTransactionAmountColor(item.type)}`}>
                                            {formatAmount(item.amount, item.type)} USD
                                        </p>
                                        {item.transferId && (
                                            <p className='text-xs text-gray-500 mt-1'>
                                                ID: {item.transferId}
                                            </p>
                                        )}
                                    </div>
                                </Card>
                            ))
                        ) : (
                            <Card className='p-8 text-center'>
                                <p className='text-gray-500'>No transactions found</p>
                                <p className='text-sm text-gray-400 mt-2'>Your transaction history will appear here</p>
                            </Card>
                        )}
                    </div>
                 </div>
            </div>
        </div>
    )
}

export default Wallet
