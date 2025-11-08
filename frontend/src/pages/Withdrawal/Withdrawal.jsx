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
function Withdrawal() {
    return (
        <div className='px-5 lg:px-20'>
                    <h1 className='font-bold text-3xl pb-5'>Withdrawal</h1>
                    <Table className="w-full">
                        <TableHeader>
                            <TableRow>
                                              <TableHead>Date</TableHead>
                                              <TableHead >Method</TableHead>
                                              <TableHead >Amount</TableHead>
                                              <TableHead >Status</TableHead>
                            </TableRow>
                        </TableHeader>
                                    
                            <TableBody>
                            {[1, 1, 1, 1, 1, 1 , 1,1,1,1].map((item, index) => (
                                              <TableRow key={index} className="hover:bg-muted/50">
                                                 <TableCell>
                                                    <p>jun 2,2024 at 11:45</p>
                                                 
                                                 </TableCell>
                                                <TableCell >Bank Account</TableCell>
                                                  <TableCell >$ 345</TableCell>
                                                    <TableCell className='text-green-500' >Completed</TableCell>
                                              </TableRow>
                                            ))}
                            </TableBody>
                    </Table>
                </div>
    )
}

export default Withdrawal
