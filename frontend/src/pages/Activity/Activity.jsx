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
import { getAllOrdersForUser } from '@/State/Order/Action';
import { useDispatch, useSelector } from 'react-redux';
import { store } from '@/State/Store';
import { calculateProfit } from '@/util/calculateProfit';
function Activity() {
  const dispatch = useDispatch()
  const {order} = useSelector(store=>store);
  useEffect(()=>{
    dispatch(getAllOrdersForUser({jwt:localStorage.getItem('jwt')}))
  },[]);
    return (
        <div className='px-5 lg:px-20'>
            <h1 className='font-bold text-3xl pb-5'>Activity</h1>
            <Table className="w-full">
                <TableHeader>
                    <TableRow>
                                      <TableHead>Date & Time</TableHead>
                                      <TableHead >Trading Pair</TableHead>
                                      <TableHead >Buy Price</TableHead>
                                      <TableHead >Sell Price</TableHead>
                                      <TableHead >Order Type  </TableHead>
                                      <TableHead >Profit/Loss</TableHead>
                                      <TableHead >Value</TableHead>
                    </TableRow>
                </TableHeader>
                            
                    <TableBody>
                    {order.orders.map((item, index) => (
                                      <TableRow key={index} className="hover:bg-muted/50">
                                         <TableCell>
                                            <p>2024/05/31</p>
                                            <p className='text-gray-400'>12:39:32</p>
                                         </TableCell>
                                        <TableCell className="font-medium flex items-center gap-3">
                                          <Avatar className="h-8 w-8">
                                            <AvatarImage
                                             src={item.orderItem.coin.image}
                                            />
                                            <AvatarFallback>B</AvatarFallback>
                                          </Avatar>
                                          <span>{item.orderItem.coin.name}</span>
                                        </TableCell>
                                        <TableCell >{item.orderItem.buyPrice}</TableCell>
                                        <TableCell >{item.orderItem.sellPrice}</TableCell>
                                        <TableCell>{item.orderType}</TableCell>
                                        <TableCell >{calculateProfit(item)}</TableCell>
                                          <TableCell >{item.price}
                                          </TableCell>
                                      </TableRow>
                                    ))}
                    </TableBody>
            </Table>
        </div>
    )
}

export default Activity
