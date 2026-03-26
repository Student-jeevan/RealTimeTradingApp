import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import { TableRow, TableCell } from '@/components/ui/table';
import { Button } from '@/components/ui/button';

const CoinRow = ({ coin }) => {
  const navigate = useNavigate();
  const isPositive = (coin?.price_change_percentage_24h || 0) >= 0;

  return (
    <TableRow 
      className="cursor-pointer hover:bg-secondary/20 transition-all group"
      onClick={() => navigate(`/market/${coin.id}`)}
    >
      <TableCell>
        <div className="flex items-center gap-4">
          <Avatar className="h-8 w-8 ring-1 ring-border/50">
            <AvatarImage src={coin.image} alt={coin.name} />
            <AvatarFallback className="text-[10px]">{coin.symbol?.charAt(0)?.toUpperCase()}</AvatarFallback>
          </Avatar>
          <div className="flex flex-col">
            <span className="font-semibold text-sm group-hover:text-primary transition-colors">{coin.name}</span>
            <span className="text-xs text-muted-foreground uppercase">{coin.symbol}</span>
          </div>
        </div>
      </TableCell>
      <TableCell className="text-right font-medium">
        ${(coin.current_price || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
      </TableCell>
      <TableCell className="text-right">
        <span className={`inline-flex items-center px-2 py-0.5 rounded-md text-xs font-semibold ${isPositive ? 'text-emerald-400 bg-emerald-500/10' : 'text-red-400 bg-red-500/10'}`}>
            {isPositive ? '+' : ''}{(coin.price_change_percentage_24h || 0).toFixed(2)}%
        </span>
      </TableCell>
      <TableCell className="text-right">
        <Button 
          variant="ghost" 
          size="sm" 
          className="text-xs h-8 gradient-primary text-white opacity-0 group-hover:opacity-100 transition-opacity"
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/market/${coin.id}`);
          }}
        >
          Trade
        </Button>
      </TableCell>
    </TableRow>
  );
};

export default React.memo(CoinRow);
