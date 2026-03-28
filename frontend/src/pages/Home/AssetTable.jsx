import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { useNavigate } from 'react-router-dom';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import { TrendingUp, TrendingDown } from 'lucide-react';

const AssetTable = ({ coin, category }) => {
  const navigate = useNavigate();

  const formatNumber = (num) => {
    if (!num) return '—';
    if (num >= 1e12) return `$${(num / 1e12).toFixed(2)}T`;
    if (num >= 1e9) return `$${(num / 1e9).toFixed(2)}B`;
    if (num >= 1e6) return `$${(num / 1e6).toFixed(2)}M`;
    return `$${num.toLocaleString()}`;
  };

  // ScrollArea must wrap the Table, not be inside it.
  // Having a <div> (ScrollArea) inside <table> is invalid HTML and causes a render crash.
  return (
    <ScrollArea className={`${category === "all" ? "h-[50vh]" : "h-[55vh]"}`}>
      <Table className="w-full">
        <TableHeader>
          <TableRow className="border-b border-border/20 hover:bg-transparent">
            <TableHead className="w-[40px] text-muted-foreground text-[11px] font-semibold">#</TableHead>
            <TableHead className="w-[180px] text-muted-foreground text-[11px] font-semibold">Coin</TableHead>
            <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">Price</TableHead>
            <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">24h %</TableHead>
            <TableHead className="text-right text-muted-foreground text-[11px] font-semibold hidden md:table-cell">Volume</TableHead>
            <TableHead className="text-right text-muted-foreground text-[11px] font-semibold hidden lg:table-cell">Market Cap</TableHead>
          </TableRow>
        </TableHeader>

        <TableBody>
          {(coin || []).map((item, index) => {
            const changePercent = item.price_change_percentage_24h || 0;
            const isPositive = changePercent >= 0;

            return (
              <TableRow
                key={item.id}
                onClick={() => navigate(`/market/${item.id}`)}
                className="cursor-pointer border-b border-border/10 hover:bg-secondary/30 transition-colors group"
              >
                <TableCell className="text-xs text-muted-foreground">
                  {item.market_cap_rank || index + 1}
                </TableCell>
                <TableCell className="font-medium">
                  <div className="flex items-center gap-3">
                    <Avatar className="h-7 w-7">
                      <AvatarImage src={item.image} alt={item.name} />
                      <AvatarFallback className="text-[10px]">{item.symbol?.charAt(0)?.toUpperCase()}</AvatarFallback>
                    </Avatar>
                    <div>
                      <p className="text-sm font-semibold group-hover:text-primary transition-colors">{item.name}</p>
                      <p className="text-[10px] text-muted-foreground uppercase">{item.symbol}</p>
                    </div>
                  </div>
                </TableCell>
                <TableCell className="text-right text-sm font-semibold">
                  ${item.current_price?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </TableCell>
                <TableCell className="text-right">
                  <span className={`inline-flex items-center gap-0.5 text-xs font-semibold px-2 py-0.5 rounded-full
                    ${isPositive ? 'text-emerald-400 bg-emerald-500/10' : 'text-red-400 bg-red-500/10'}`}
                  >
                    {isPositive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                    {changePercent.toFixed(2)}%
                  </span>
                </TableCell>
                <TableCell className="text-right text-xs text-muted-foreground hidden md:table-cell">
                  {formatNumber(item.total_volume)}
                </TableCell>
                <TableCell className="text-right text-xs text-muted-foreground hidden lg:table-cell">
                  {formatNumber(item.market_cap)}
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </ScrollArea>
  );
};

export default AssetTable;
