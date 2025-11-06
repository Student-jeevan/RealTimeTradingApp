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
function Activity() {
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
                    {[1, 1, 1, 1, 1, 1 , 1,1,1,1].map((item, index) => (
                                      <TableRow key={index} className="hover:bg-muted/50">
                                         <TableCell>
                                            <p>2024/05/31</p>
                                            <p className='text-gray-400'>12:39:32</p>
                                         </TableCell>
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
                                        <TableCell >$69249</TableCell>
                                        <TableCell >$1,364,881,428,323</TableCell>
                                        <TableCell>-0.20000</TableCell>
                                        <TableCell >$69,249.00</TableCell>
                                          <TableCell >345
                                          </TableCell>
                                      </TableRow>
                                    ))}
                    </TableBody>
            </Table>
        </div>
    )
}

export default Activity
