import React from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { BookmarkFilledIcon } from '@radix-ui/react-icons';
function Watchlist() {
    const handleRemoveToWatchlist=(value)=>{
        console.log(value)
    }
    return (
        <div className='px-5 lg:px-20'>
                    <h1 className='font-bold text-3xl pb-5'>Watchlist</h1>
                     <Table className="w-full">
                          <TableHeader>
                            <TableRow>
                              <TableHead className="w-[180px]">Coin</TableHead>
                              <TableHead className="w-[100px] text-center">Symbol</TableHead>
                              <TableHead className="w-[160px] text-center">Volume</TableHead>
                              <TableHead className="w-[160px] text-right">Market Cap</TableHead>
                              <TableHead className="w-[100px] text-right">24h</TableHead>
                              <TableHead className="w-[120px] text-right">Price</TableHead>
                              <TableHead className="w-[120px] text-right text-red-600">Remove</TableHead>
                            </TableRow>
                          </TableHeader>
                    
                          <TableBody>
                            {[1, 1, 1, 1, 1, 1 , 1,1,1,1].map((item, index) => (
                              <TableRow key={index} className="hover:bg-muted/50">
                                <TableCell className="font-medium flex items-center gap-3">
                                  <Avatar className="h-8 w-8">
                                    <AvatarImage
                                      src="https://assets.coingecko.com/coins/images/1/large/bitcoin.png"
                                      alt="Bitcoin"
                                    />
                                    <AvatarFallback>B</AvatarFallback>
                                  </Avatar>
                                  <span>Bitcoin</span>
                                </TableCell>
                    
                                <TableCell className="text-center">BTC</TableCell>
                                <TableCell className="text-center">9,124,463,121</TableCell>
                                <TableCell className="text-right">$1,364,881,428,323</TableCell>
                                <TableCell className="text-right text-red-500">-0.20%</TableCell>
                                <TableCell className="text-right font-semibold">$69,249.00</TableCell>
                                  <TableCell className="text-right font-semibold">
                                    <Button variant='ghost'  onClick={()=>handleRemoveToWatchlist(item.id)} size='icon' clasName='h-10 w-10 '>
                                        <BookmarkFilledIcon className='w-6 h-6' />
                                    </Button>
                                  </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
        </div>
    )
}

export default Watchlist
