import React from 'react';
import CoinRow from './CoinRow';
import { Table, TableBody, TableHeader, TableRow, TableHead, TableCell } from '@/components/ui/table';

const CoinList = ({ coins = [], loading }) => {
  if (loading) {
    return (
      <div className="glass-card rounded-2xl flex flex-col pt-5">
        <h3 className="text-sm font-semibold text-muted-foreground px-5 pb-3">Market Overview</h3>
        <div className="p-5 flex flex-col gap-4">
          {[1,2,3,4,5].map(i => <div key={i} className="skeleton h-12 w-full rounded-lg" />)}
        </div>
      </div>
    );
  }

  return (
    <div className="glass-card rounded-2xl flex flex-col pt-5">
      <h3 className="text-sm font-semibold text-muted-foreground px-5 pb-3">Market Overview</h3>
      <div className="flex-1 overflow-auto px-5 pb-5">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Coin</TableHead>
              <TableHead className="text-right">Price</TableHead>
              <TableHead className="text-right">24h Change</TableHead>
              <TableHead className="text-right">Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {coins.slice(0, 10).map((coin) => (
              <CoinRow key={coin.id} coin={coin} />
            ))}
            {coins.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center py-6 text-muted-foreground text-xs">
                  No market data available
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

export default React.memo(CoinList);
