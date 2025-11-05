import React from 'react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
function Portfolio() {
    return (
        <div className='px-5 lg:px-20'>
            <h1 className='font-bold text-3xl pb-5'>Portfolio</h1>
             <Table className="w-full">
                  <TableHeader>
                    <TableRow>  
                      <TableHead className="w-[180px]">Asset</TableHead>
                      <TableHead className="w-[100px] text-center">Price</TableHead>
                      <TableHead className="w-[160px] text-center">Unit</TableHead>
                      <TableHead className="w-[160px] text-right">Change</TableHead>
                      <TableHead className="w-[100px] text-right">Change%</TableHead>
                      <TableHead className="w-[120px] text-right">Value </TableHead>
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
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
        </div>
    )
}
export default Portfolio
