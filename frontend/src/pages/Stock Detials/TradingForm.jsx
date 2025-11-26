import React from 'react'
import { Input } from '@/components/ui/input'
import { Avatar, AvatarImage } from '@/components/ui/avatar'
import { DotIcon } from '@radix-ui/react-icons' 
import { Button } from '@/components/ui/button'
import { useState, useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { getUserWallet } from '@/State/Wallet/Action'

function TradingForm() {
    const [amount , setAmount] =useState(0);
    const [orderType , setOrderType] = useState("BUY");
    const [Quantity, setQuantity] = useState(0)
    const {coin, wallet} = useSelector((store)=>store)
    const dispatch = useDispatch();

    const coinPrice = coin?.coinDetails?.market_data?.current_price?.usd ?? 0;

    useEffect(() => {
        // Ensure wallet data is loaded when this form is used
        if (!wallet?.userWallet || wallet.userWallet.balance === undefined) {
            const jwt = localStorage.getItem("jwt");
            if (jwt) {
                dispatch(getUserWallet(jwt));
            }
        }
    }, [dispatch, wallet?.userWallet]);

    const handleChange = (e) => {
         const amount = e.target.value;
         setAmount(amount)
         if (!coinPrice) {
            setQuantity(0);
            return;
         }
         const volume = calculateBuyCost(amount , coinPrice);
         setQuantity(volume);
    }
    const calculateBuyCost = (amount, price)=>{
        let volume = amount/price

        let decimalPlaces =Math.max(2,price.toString().split(".")[1]?.length || 2);
        return volume.toFixed(decimalPlaces);
    }
    return (
        <div className='space-y-10 p-5'>
            <div>
                <div className='flex gap-4 items-center justify-between'>
                    <Input className='py-7 focus:outline-none' placeholder="Enter Amount.." onChange={handleChange} type="number" />
                    <div>
                        <p className='border text-2xl flex justify-center items-center w-36 h-14 rounded-md'>{Quantity}</p>
                    </div>
                </div>
                {false &&  <h1 className='text-red-600 text-center pt-4'>Insufficient Wallet balance to buy</h1>}
            </div>
            <div className='flex gap-5 items-center'>
                      <Avatar>
                        <AvatarImage src="https://assets.coingecko.com/coins/images/279/large/ethereum.png?1547034954" />
                      </Avatar>
                      <div>
                        <div className='flex items-center gap-2'>
                          <p>BTC</p>
                          <DotIcon className='text-gray-400' />
                          <p className='text-gray-400'>Bitcoin</p>
                        </div>
                        <div className='flex items-end gap-2'>
                          <p className='text-xl font-bold'>{coinPrice}</p>
                          <p className='text-red-600'>
                            <span>-13123224.565</span>
                            <span>(-0.233433%)</span>
                          </p>
                        </div>
                      </div>
            </div>
            <div className='flex items-center justify-between'>
                <p>Order Type</p>
                <p>Market Order</p>
            </div>
            <div className='flex items-center justify-between'>
                <p>{orderType=="BUY"?"Available Cash":"Available Quantity"}</p>
                <p>{orderType=="BUY"? (wallet?.userWallet?.balance ?? 0):23.08}</p>
            </div>
            <div>
                <Button className={`w-full py-6 ${orderType=="SELL"?"bg-red-600 text-white":""} `} >
                    {orderType}
                </Button>
                <Button variant="link" className='w-full mt-5 text-xl' onClick={()=>setOrderType(orderType=="BUY"?"SELL":"BUY") }> 
                    {orderType=="BUY"?"Or Sell":"Or Buy"}
                </Button>
            </div>
        </div>
    )
}

export default TradingForm
