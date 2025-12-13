import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { 
  getWithdrawalHistory, 
  getAllWithdrawalRequest, 
  proceedWithdrawal 
} from '@/State/Withdrawal/Action';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { isAdmin } from '@/utils/roleUtils';

function Withdrawal() {
  const dispatch = useDispatch();
  const jwt = localStorage.getItem('jwt');
  const auth = useSelector(store => store.auth);
  const user = auth.user;
  const admin = isAdmin(user);

  const { history, requests, loading, error } = useSelector(store => store.withdrawal);

  useEffect(() => {
    if (!jwt) return;
    if (admin) dispatch(getAllWithdrawalRequest(jwt));
    else dispatch(getWithdrawalHistory(jwt));
  }, [dispatch, jwt, admin]);

  const handleApprove = (id) => {
    console.log('APPROVE CLICKED', id);
    dispatch(proceedWithdrawal({ id, jwt, accept: true }));
  };

  const handleReject = (id) => {
    console.log('REJECT CLICKED', id);
    dispatch(proceedWithdrawal({ id, jwt, accept: false }));
  };

  const data = admin ? requests : history;

  return (
    <div className="px-5 lg:px-20">
      <h1 className="font-bold text-3xl pb-5">Withdrawal</h1>

      {error && <p className="text-red-500 mb-3">{error}</p>}

      <Table className="w-full">
        <TableHeader>
          <TableRow>
            <TableHead>Date</TableHead>
            <TableHead>User</TableHead>
            <TableHead>Method</TableHead>
            <TableHead>Amount</TableHead>
            <TableHead>Status</TableHead>
            {admin && <TableHead className="text-right">Actions</TableHead>}
          </TableRow>
        </TableHeader>

        <TableBody>
          {data?.length > 0 ? data.map((item, index) => (
            <TableRow key={item.id || index}>
              <TableCell>{new Date(item.date).toLocaleDateString()}</TableCell>
              <TableCell>{item.user?.fullName || "Unknown"}</TableCell>
              <TableCell>{item.method || 'Bank Account'}</TableCell>
              <TableCell>₹ {item.amount}</TableCell>

              <TableCell>
                <Badge className={
                  item.status === 'SUCCESS'
                    ? 'bg-green-500'
                    : item.status === 'PENDING'
                    ? 'bg-yellow-500'
                    : 'bg-red-500'
                }>
                  {item.status}
                </Badge>
              </TableCell>

              {admin && item.status === 'PENDING' && (
                <TableCell className="text-right space-x-2">
                  <Button
                    type="button"
                    onClick={() => handleApprove(item.id)}
                    className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 text-sm"
                  >
                    Approve
                  </Button>

                  <Button
                    type="button"
                    onClick={() => handleReject(item.id)}
                    className="bg-red-600 hover:bg-red-700 text-white px-3 py-1 text-sm"
                  >
                    Reject
                  </Button>
                </TableCell>
              )}
            </TableRow>
          )) : (
            <TableRow>
              <TableCell colSpan={admin ? 6 : 5} className="text-center py-5 text-gray-500">
                No withdrawal records
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  );
}

export default Withdrawal;
