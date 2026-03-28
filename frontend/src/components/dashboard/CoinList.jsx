import React, { useState } from 'react';
import CoinRow from './CoinRow';
import { Table, TableBody, TableHeader, TableRow, TableHead, TableCell } from '@/components/ui/table';
import { ScrollArea } from '@/components/ui/scroll-area';
import { ChevronLeft, ChevronRight } from 'lucide-react';

const PAGE_SIZE = 10;

const CoinList = ({ coins = [], loading, onCoinSelect, selectedCoinId }) => {
  const [page, setPage] = useState(1);

  const totalPages = Math.max(1, Math.ceil(coins.length / PAGE_SIZE));
  const paginatedCoins = coins.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  // Reset to page 1 whenever coins change (e.g. fresh load)
  React.useEffect(() => { setPage(1); }, [coins.length]);

  if (loading) {
    return (
      <div className="glass-card rounded-2xl flex flex-col h-full p-5">
        <h3 className="text-sm font-semibold text-muted-foreground pb-3">Market</h3>
        <div className="flex flex-col gap-3">
          {[1, 2, 3, 4, 5, 6].map(i => (
            <div key={i} className="skeleton h-12 w-full rounded-lg" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="glass-card rounded-2xl flex flex-col min-h-0">
      {/* Header */}
      <div className="px-4 pt-4 pb-2 flex items-center justify-between shrink-0">
        <h3 className="text-sm font-semibold text-muted-foreground">Market</h3>
        <span className="text-[10px] text-muted-foreground/60">{coins.length} coins</span>
      </div>

      {/* Table */}
      <ScrollArea className="flex-1 px-2">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent border-b border-border/20">
              <TableHead className="text-[11px]">Coin</TableHead>
              <TableHead className="text-right text-[11px]">Price</TableHead>
              <TableHead className="text-right text-[11px]">24h</TableHead>
              <TableHead className="text-right text-[11px]">Action</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {paginatedCoins.map((coin) => (
              <CoinRow
                key={coin.id}
                coin={coin}
                onSelect={onCoinSelect}
                isSelected={selectedCoinId === coin.id}
              />
            ))}
            {paginatedCoins.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} className="text-center py-8 text-muted-foreground text-xs">
                  No market data available
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </ScrollArea>

      {/* Pagination footer */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between px-4 py-3 border-t border-border/20 shrink-0">
          <span className="text-[11px] text-muted-foreground">
            Page {page} of {totalPages}
          </span>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1}
              className="p-1.5 rounded-lg hover:bg-secondary transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="w-3.5 h-3.5" />
            </button>

            {/* Page number pills */}
            {Array.from({ length: totalPages }, (_, i) => i + 1)
              .filter(n => n === 1 || n === totalPages || Math.abs(n - page) <= 1)
              .reduce((acc, n, idx, arr) => {
                if (idx > 0 && n - arr[idx - 1] > 1) acc.push('...');
                acc.push(n);
                return acc;
              }, [])
              .map((item, idx) =>
                item === '...' ? (
                  <span key={`ellipsis-${idx}`} className="px-1 text-[11px] text-muted-foreground">…</span>
                ) : (
                  <button
                    key={item}
                    onClick={() => setPage(item)}
                    className={`w-6 h-6 rounded-md text-[11px] font-medium transition-colors
                      ${page === item
                        ? 'bg-primary text-white'
                        : 'hover:bg-secondary text-muted-foreground'
                      }`}
                  >
                    {item}
                  </button>
                )
              )
            }

            <button
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              className="p-1.5 rounded-lg hover:bg-secondary transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default React.memo(CoinList);
