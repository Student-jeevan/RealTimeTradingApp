import React, { useEffect } from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';

import { useDispatch, useSelector } from 'react-redux';
import { getWithdrawalHistory } from '@/State/Withdrawal/Action';

function WithdrawalHistory() {

  const dispatch = useDispatch();

  const { withdrawal } = useSelector(store => store);

  useEffect(() => {
    dispatch(getWithdrawalHistory(localStorage.getItem("jwt")));
  }, []);

  return (
    <div className='px-5 lg:px-20'>
      <h1 className='font-bold text-3xl pb-5'>Withdrawal</h1>

      <Table className="w-full">
        <TableHeader>
          <TableRow>
            <TableHead>Date</TableHead>
            <TableHead>Method</TableHead>
            <TableHead>Amount</TableHead>
            <TableHead>Status</TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          {withdrawal?.history?.map((item, index) => (
            <TableRow key={index} className="hover:bg-muted/50">
              
              {/* Date */}
              <TableCell>
                {new Date(item.date).toLocaleDateString()}
              </TableCell>

              {/* Method */}
              <TableCell>
                {item.method || "Bank Account"}
              </TableCell>

              {/* Amount */}
              <TableCell>
                ₹ {item.amount}
              </TableCell>

              {/* Status */}
              <TableCell className={
                item.status === "COMPLETED" ? 'text-green-500' :
                item.status === "PENDING" ? 'text-yellow-500' :
                'text-red-500'
              }>
                {item.status}
              </TableCell>

            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

export default WithdrawalHistory;
