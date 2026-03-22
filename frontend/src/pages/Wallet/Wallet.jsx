import React from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogTrigger,DialogContent,DialogHeader,DialogTitle } from '@/components/ui/dialog'
import { WalletIcon , CopyIcon ,RefreshCw, DollarSign, UploadIcon, ShuffleIcon, LucideShuffle, ArrowDownCircle, ArrowUpCircle, ShoppingCart, TrendingUp, TrendingDown} from 'lucide-react'
import TopupForm from './TopupForm'
import Withdrawal from './WithdrawalForm'
import TransferForm from './TransferForm'
import { UpdateIcon } from '@radix-ui/react-icons'
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
        const iconClass = "w-5 h-5";
        switch(type) {
            case 'ADD_MONEY': return <ArrowDownCircle className={`${iconClass} text-emerald-400`} />;
            case 'WITHDRAWAL': return <ArrowUpCircle className={`${iconClass} text-red-400`} />;
            case 'WALLET_TRANSFER': return <ShuffleIcon className={`${iconClass} text-blue-400`} />;
            case 'BUY_ASSET': return <ShoppingCart className={`${iconClass} text-amber-400`} />;
            case 'SELL_ASSET': return <TrendingUp className={`${iconClass} text-emerald-400`} />;
            default: return <LucideShuffle className={`${iconClass} text-muted-foreground`} />;
        }
    }

    const getTransactionTypeLabel = (type) => {
        switch(type) {
            case 'ADD_MONEY': return 'Add Money';
            case 'WITHDRAWAL': return 'Withdrawal';
            case 'WALLET_TRANSFER': return 'Wallet Transfer';
            case 'BUY_ASSET': return 'Buy Asset';
            case 'SELL_ASSET': return 'Sell Asset';
            default: return type || 'Transaction';
        }
    }

    const getTransactionAmountColor = (type) => {
        if (type === 'ADD_MONEY' || type === 'SELL_ASSET') return 'text-emerald-400';
        if (type === 'WITHDRAWAL' || type === 'BUY_ASSET') return 'text-red-400';
        return 'text-blue-400';
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
        return `${sign}$${Number(amount).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }

    const actionButtons = [
      {
        label: 'Add Money',
        icon: <ArrowDownCircle className="w-5 h-5" />,
        gradient: 'from-emerald-500 to-teal-600',
        form: <TopupForm />,
        dialogTitle: 'Top Up Your Wallet',
      },
      {
        label: 'Withdraw',
        icon: <ArrowUpCircle className="w-5 h-5" />,
        gradient: 'from-red-500 to-pink-600',
        form: <Withdrawal />,
        dialogTitle: 'Request Withdrawal',
      },
      {
        label: 'Transfer',
        icon: <ShuffleIcon className="w-5 h-5" />,
        gradient: 'from-blue-500 to-purple-600',
        form: <TransferForm />,
        dialogTitle: 'Transfer to Other Wallet',
      },
    ];

    return (
        <div className='space-y-6'>
          {/* Header */}
          <div>
            <h1 className='text-2xl font-bold'>Wallet</h1>
            <p className='text-sm text-muted-foreground mt-1'>Manage your funds</p>
          </div>

          <div className='grid grid-cols-1 lg:grid-cols-3 gap-6'>
            {/* Balance Card */}
            <div className='lg:col-span-2'>
              <div className='glass-card rounded-2xl p-6'>
                <div className='flex justify-between items-start mb-6'>
                  <div className='flex items-center gap-4'>
                    <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-purple-600 shadow-lg">
                      <WalletIcon className="w-6 h-6 text-white" />
                    </div>
                    <div>
                      <p className='text-sm text-muted-foreground'>Available Balance</p>
                      <div className='flex items-baseline gap-1 mt-1'>
                        <span className='text-3xl font-bold'>${(wallet?.userWallet?.balance ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2 })}</span>
                        <span className='text-xs text-muted-foreground'>USD</span>
                      </div>
                    </div>
                  </div>
                  <button onClick={handleFetchUserWallet} className='p-2 rounded-lg hover:bg-secondary transition-colors'>
                    <RefreshCw className='w-4 h-4 text-muted-foreground hover:text-foreground' />
                  </button>
                </div>

                {/* Action Buttons */}
                <div className='flex gap-3'>
                  {actionButtons.map((btn) => (
                    <Dialog key={btn.label}>
                      <DialogTrigger asChild>
                        <button className='flex flex-col items-center gap-2 px-4 py-3 rounded-xl hover:bg-secondary/50 transition-all hover:scale-105 flex-1'>
                          <div className={`p-2.5 rounded-xl bg-gradient-to-br ${btn.gradient}`}>
                            {React.cloneElement(btn.icon, { className: 'w-5 h-5 text-white' })}
                          </div>
                          <span className='text-xs font-medium text-muted-foreground'>{btn.label}</span>
                        </button>
                      </DialogTrigger>
                      <DialogContent>
                        <DialogHeader>
                          <DialogTitle>{btn.dialogTitle}</DialogTitle>
                        </DialogHeader>
                        {btn.form}
                      </DialogContent>
                    </Dialog>
                  ))}
                </div>
              </div>
            </div>

            {/* Quick Info */}
            <div className='glass-card rounded-2xl p-5 flex flex-col justify-center'>
              <div className="flex items-center gap-2 mb-3">
                <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <p className="text-xs text-muted-foreground">Wallet Active</p>
              </div>
              <div className="space-y-3">
                <div>
                  <p className="text-[10px] text-muted-foreground uppercase tracking-wider">Wallet ID</p>
                  <div className="flex items-center gap-2 mt-0.5">
                    <p className="text-xs text-foreground/80 font-mono">#{wallet.userWallet?.id}</p>
                    <CopyIcon className="w-3 h-3 text-muted-foreground cursor-pointer hover:text-foreground" />
                  </div>
                </div>
                <div>
                  <p className="text-[10px] text-muted-foreground uppercase tracking-wider">Transactions</p>
                  <p className="text-lg font-bold">{wallet?.transactions?.length || 0}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Transaction History */}
          <div>
            <div className='flex items-center justify-between mb-4'>
              <h2 className='text-lg font-semibold'>Recent Transactions</h2>
              <button onClick={handleFetchWalletTransaction} className='p-2 rounded-lg hover:bg-secondary transition-colors'>
                <RefreshCw className='w-4 h-4 text-muted-foreground' />
              </button>
            </div>
            <div className='space-y-2'>
              {wallet?.transactions && wallet.transactions.length > 0 ? (
                wallet.transactions.map((item, i) => (
                  <div key={item.id || i} className='glass-card rounded-xl flex justify-between items-center p-4 hover:bg-secondary/20 transition-colors'>
                    <div className='flex items-center gap-4'>
                      <div className='flex items-center justify-center w-10 h-10 rounded-xl bg-secondary/60'>
                        {getTransactionIcon(item.type)}
                      </div>
                      <div className='space-y-0.5'>
                        <p className='font-medium text-sm'>
                          {getTransactionTypeLabel(item.type)}
                        </p>
                        {item.purpose && (
                          <p className='text-[11px] text-muted-foreground'>{item.purpose}</p>
                        )}
                        <p className='text-[10px] text-muted-foreground/70'>
                          {formatDate(item.date)}
                        </p>
                      </div>
                    </div>
                    <div className='text-right'>
                      <p className={`font-bold text-sm ${getTransactionAmountColor(item.type)}`}>
                        {formatAmount(item.amount, item.type)}
                      </p>
                    </div>
                  </div>
                ))
              ) : (
                <div className='glass-card rounded-2xl p-10 text-center'>
                  <WalletIcon className='w-10 h-10 text-muted-foreground/40 mx-auto mb-3' />
                  <p className='text-sm text-muted-foreground'>No transactions found</p>
                  <p className='text-xs text-muted-foreground/60 mt-1'>Your transaction history will appear here</p>
                </div>
              )}
            </div>
          </div>
        </div>
    )
}

export default Wallet
