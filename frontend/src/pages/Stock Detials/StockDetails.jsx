import { Avatar , AvatarImage } from '@/components/ui/avatar'
import { BookmarkFilledIcon, DotIcon } from '@radix-ui/react-icons'
import React, { useEffect } from 'react'
import { Bookmark } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useParams } from 'react-router-dom'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import TradingForm from '../Stock Detials/TradingForm'
import StockChart from '../Home/StockChart'
import { useDispatch, useSelector } from 'react-redux'
import { fetchCoinDetails } from '@/State/Coin/Action'
function StockDetails() {
  const coin = useSelector(state => state.coin)
  const [bookmarked, setBookmarked] = React.useState(false);
  const dispatch=useDispatch()
  const {id}=useParams()
    useEffect(()=>{
      const jwt = localStorage.getItem("jwt");
      dispatch(fetchCoinDetails(id, jwt));
  },[dispatch,id])
  return (
    <div className='p-5 mt-5'>
      <div className='flex justify-between'>
        <div className='flex gap-5 items-center'>
          <Avatar>
          <AvatarImage src={coin.coinDetails?.image.large} />
          </Avatar>
          <div>
            <div className='flex items-center gap-2'>
              <p>{coin.coinDetails?.symbol.toUpperCase()}</p>
              <DotIcon className='text-gray-400' />
              <p className='text-gray-400'>{coin.coinDetails?.name}</p>
            </div>
            <div className='flex items-end gap-2'>
              <p className='text-xl font-bold'>{coin.coinDetails?.market_data.current_price.usd}</p>
              <p className='text-red-600'>
                <span>{coin.coinDetails?.market_data.market_cap_change_24h}</span>
                <span>({coin.coinDetails?.market_data.market_cap_change_percentage_24h}%)</span>
              </p>
            </div>
          </div>
        </div>

        <div className='flex items-center gap-4'>
            <Button onClick={() => setBookmarked(!bookmarked)}>
          {bookmarked ? <BookmarkFilledIcon className='h-6 w-6' /> : <Bookmark className='h-6 w-6' />}
        </Button>

        <Dialog>
          <DialogTrigger>
            <Button size="lg">Trade</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>How Much Do you want to spend?</DialogTitle>
            </DialogHeader>
            <TradingForm/>
          </DialogContent>
        </Dialog>
        </div>

      </div>
      <div className='mt-14' >
        <StockChart coinId={id}/>
      </div>
    </div>
  )
}

export default StockDetails
