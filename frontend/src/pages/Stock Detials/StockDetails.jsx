import { Avatar , AvatarImage } from '@/components/ui/avatar'
import { BookmarkFilledIcon, DotIcon } from '@radix-ui/react-icons'
import React from 'react'
import { Bookmark } from 'lucide-react'
import { Button } from '@/components/ui/button'
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
function StockDetails() {
  const [bookmarked, setBookmarked] = React.useState(false);

  return (
    <div className='p-5 mt-5'>
      <div className='flex justify-between'>
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
              <p className='text-xl font-bold'>$6554</p>
              <p className='text-red-600'>
                <span>-13123224.565</span>
                <span>(-0.233433%)</span>
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
        <StockChart/>
      </div>
    </div>
  )
}

export default StockDetails
