import { Avatar , AvatarImage } from '@/components/ui/avatar'
import { BookmarkFilledIcon, DotIcon } from '@radix-ui/react-icons'
import React, { useEffect } from 'react'
import { Bookmark } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useParams } from 'react-router-dom'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import TradingForm from '../Stock Detials/TradingForm'
import StockChart from '../Home/StockChart'
import { useDispatch, useSelector } from 'react-redux'
import { fetchCoinDetails } from '@/State/Coin/Action'
import { addItemToWatchlist } from '@/State/Watchlist/Action'
import { existInWatchlist } from '@/util/existInWatchlist'
import { getUserWatchlist } from '@/State/Watchlist/Action'
function StockDetails() {
  const {coin, watchlist} = useSelector(state => state)
  const [bookmarked, setBookmarked] = React.useState(false);
  const dispatch=useDispatch()
  const {id}=useParams()
  
  useEffect(()=>{
    const jwt = localStorage.getItem("jwt");
    if (jwt && id) {
      dispatch(fetchCoinDetails(id, jwt));
    }
    dispatch(getUserWatchlist(localStorage.getItem("jwt")));
  },[dispatch, id])
  
  const handleAddToWatchlist=()=>{
    const jwt = localStorage.getItem("jwt");
    const coinId = coin?.coinDetails?.id;
    if (jwt && coinId) {
      dispatch(addItemToWatchlist(coinId, jwt));
      setBookmarked(true);
    }
  }
  if (coin?.loading) {
    return (
      <div className='p-5 mt-5 flex items-center justify-center min-h-[400px]'>
        <p className='text-gray-500'>Loading coin details...</p>
      </div>
    );
  }

  if (coin?.error || !coin?.coinDetails) {
    return (
      <div className='p-5 mt-5 flex items-center justify-center min-h-[400px]'>
        <p className='text-red-500'>Error loading coin details. Please try again.</p>
      </div>
    );
  }

  const coinDetails = coin.coinDetails;
  const imageUrl = coinDetails?.image?.large || coinDetails?.image || '';
  const symbol = coinDetails?.symbol?.toUpperCase() || '';
  const name = coinDetails?.name || '';
  const price = coinDetails?.market_data?.current_price?.usd || coinDetails?.current_price || 0;
  const priceChange24h = coinDetails?.market_data?.price_change_24h || coinDetails?.priceChange24h || 0;
  const priceChangePercentage24h = coinDetails?.market_data?.price_change_percentage_24h || coinDetails?.priceChangePercentage24h || 0;
  const isPositive = priceChange24h >= 0;

  return (
    <div className='p-5 mt-5'>
      <div className='flex justify-between'>
        <div className='flex gap-5 items-center'>
          <Avatar>
            <AvatarImage src={imageUrl} alt={name} />
          </Avatar>
          <div>
            <div className='flex items-center gap-2'>
              <p>{symbol}</p>
              <DotIcon className='text-gray-400' />
              <p className='text-gray-400'>{name}</p>
            </div>
            <div className='flex items-end gap-2'>
              <p className='text-xl font-bold'>${price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
              <p className={isPositive ? 'text-green-600' : 'text-red-600'}>
                <span>{isPositive ? '+' : ''}${priceChange24h.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                <span> ({isPositive ? '+' : ''}{priceChangePercentage24h.toFixed(2)}%)</span>
              </p>
            </div>
          </div>
        </div>

        <div className='flex items-center gap-4'>
            <Button onClick={handleAddToWatchlist}>
           {existInWatchlist(watchlist.items,coin.coinDetails)? <BookmarkFilledIcon className='h-6 w-6' /> : <Bookmark className='h-6 w-6' />}
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
