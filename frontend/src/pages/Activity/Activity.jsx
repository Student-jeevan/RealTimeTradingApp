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
import { ArrowUpDown, Clock } from 'lucide-react';

function Activity() {
  const dispatch = useDispatch()
  const {order} = useSelector(store=>store);
  useEffect(()=>{
    dispatch(getAllOrdersForUser({jwt:localStorage.getItem('jwt')}))
  },[]);

  const formatDate = (dateStr) => {
    if (!dateStr) return { date: '—', time: '' };
    try {
      const d = new Date(dateStr);
      return {
        date: d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
        time: d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      };
    } catch {
      return { date: '—', time: '' };
    }
  };

    return (
        <div className='space-y-6'>
            {/* Header */}
            <div>
              <h1 className='text-2xl font-bold'>Activity</h1>
              <p className='text-sm text-muted-foreground mt-1'>Your recent trading activity</p>
            </div>

            {/* Table */}
            <div className="glass-card rounded-2xl overflow-hidden">
              <Table className="w-full">
                <TableHeader>
                    <TableRow className="border-b border-border/20 hover:bg-transparent">
                      <TableHead className="text-muted-foreground text-[11px] font-semibold">Date & Time</TableHead>
                      <TableHead className="text-muted-foreground text-[11px] font-semibold">Trading Pair</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">Buy Price</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">Sell Price</TableHead>
                      <TableHead className="text-center text-muted-foreground text-[11px] font-semibold">Type</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">P/L</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">Value</TableHead>
                    </TableRow>
                </TableHeader>
                            
                <TableBody>
                  {order.orders && order.orders.length > 0 ? (
                    order.orders.map((item, index) => {
                      const profit = calculateProfit(item);
                      const { date, time } = formatDate(item.timestamp || item.createdAt);
                      const isBuy = item.orderType === 'BUY';

                      return (
                        <TableRow key={index} className="border-b border-border/10 hover:bg-secondary/30 transition-colors">
                           <TableCell>
                              <div>
                                <p className="text-sm">{date}</p>
                                <p className='text-[10px] text-muted-foreground'>{time}</p>
                              </div>
                           </TableCell>
                          <TableCell className="font-medium">
                            <div className="flex items-center gap-3">
                              <Avatar className="h-7 w-7">
                                <AvatarImage src={item.orderItem.coin.image} />
                                <AvatarFallback className="text-[10px]">{item.orderItem.coin.symbol?.charAt(0)?.toUpperCase()}</AvatarFallback>
                              </Avatar>
                              <span className="text-sm font-semibold">{item.orderItem.coin.name}</span>
                            </div>
                          </TableCell>
                          <TableCell className="text-right text-sm">${item.orderItem.buyPrice}</TableCell>
                          <TableCell className="text-right text-sm">${item.orderItem.sellPrice}</TableCell>
                          <TableCell className="text-center">
                            <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider
                              ${isBuy ? 'text-emerald-400 bg-emerald-500/10' : 'text-red-400 bg-red-500/10'}`}
                            >
                              {item.orderType}
                            </span>
                          </TableCell>
                          <TableCell className="text-right text-sm font-semibold">{profit}</TableCell>
                          <TableCell className="text-right text-sm font-bold">${item.price}</TableCell>
                        </TableRow>
                      );
                    })
                  ) : (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center py-12 text-muted-foreground">
                        <div className="flex flex-col items-center gap-2">
                          <ArrowUpDown className="w-10 h-10 text-muted-foreground/40" />
                          <p className="text-sm">No trading activity yet</p>
                          <p className="text-xs text-muted-foreground/60">Your trades will appear here</p>
                        </div>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
            </Table>
            </div>
        </div>
    )
}

export default Activity
