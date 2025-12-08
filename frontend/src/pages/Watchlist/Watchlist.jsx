import React, { useEffect } from 'react'
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
import { getUserWatchlist, addItemToWatchlist } from '@/State/Watchlist/Action';
import { useSelector, useDispatch } from 'react-redux';
function Watchlist() {
  const watchlist = useSelector(state => state.watchlist)
  const dispatch = useDispatch()
  
  const handleRemoveToWatchlist = async (coinId) => {
    const jwt = localStorage.getItem("jwt");
    await dispatch(addItemToWatchlist(coinId, jwt));
    // Refresh watchlist after adding/removing coin
    dispatch(getUserWatchlist(jwt));
  }
  
  useEffect(() => {
    dispatch(getUserWatchlist(localStorage.getItem("jwt")));
  }, [dispatch])
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
                            {watchlist.items && watchlist.items.length > 0 ? (
                              watchlist.items.map((item, index) => (
                                <TableRow key={index} className="hover:bg-muted/50">
                                  <TableCell className="font-medium flex items-center gap-3">
                                    <Avatar className="h-8 w-8">
                                      <AvatarImage
                                        src={item.image}
                                      />
                                      <AvatarFallback>B</AvatarFallback>
                                    </Avatar>
                                    <span>{item.name}</span>
                                  </TableCell>
                      
                                  <TableCell className="text-center">{item.symbol}</TableCell>
                                  <TableCell className="text-center">{item.totalVolume}</TableCell>
                                  <TableCell className="text-right">{item.market_cap}</TableCell>
                                  <TableCell className="text-right text-red-500">{item.price_change_percentage_24h}</TableCell>
                                  <TableCell className="text-right font-semibold">{item.current_price}</TableCell>
                                    <TableCell className="text-right font-semibold">
                                      <Button variant='ghost' onClick={() => handleRemoveToWatchlist(item.id)} size='icon' className='h-10 w-10' >
                                          < BookmarkFilledIcon className='w-6 h-6' />
                                      </Button>
                                    </TableCell>
                                </TableRow>
                              ))
                            ) : (
                              <TableRow>
                                <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                                  No items in watchlist
                                </TableCell>
                              </TableRow>
                            )}
                          </TableBody>
                        </Table>
        </div>
    )
}

export default Watchlist
