import React, { use, useEffect } from 'react';
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
import { useDispatch } from 'react-redux';

const AssetTable = ({coin, category}) => {
  const dispatch=useDispatch()
  const navigate  = useNavigate();
  return (
    <Table className="w-full">
      <TableHeader>
        <TableRow>
          <TableHead className="w-[180px]">Coin</TableHead>
          <TableHead className="w-[100px] text-center">Symbol</TableHead>
          <TableHead className="w-[160px] text-center">Volume</TableHead>
          <TableHead className="w-[160px] text-right">Market Cap</TableHead>
          <TableHead className="w-[100px] text-right">24h</TableHead>
          <TableHead className="w-[120px] text-right">Price</TableHead>
        </TableRow>
      </TableHeader>

      <TableBody>
        {coin.map((item, index) => (
          <TableRow key={item.id} className="hover:bg-muted/50">
            <TableCell onClick={() => navigate(`/market/${item.id}`)} className="font-medium flex items-center gap-3">
              <Avatar className="h-8 w-8">
                <AvatarImage
                  src={item.image}
                  alt="Bitcoin"
                />
                <AvatarFallback>B</AvatarFallback>
              </Avatar>
              <span>{item.name}</span>
            </TableCell>

            <TableCell className="text-center">{item.symbol}</TableCell>
            <TableCell className="text-center">{item.total_volume}</TableCell>
            <TableCell className="text-right">{item.market_cap}</TableCell>
            <TableCell className="text-right text-red-500">{item.price_change_percentage_24h}</TableCell>
            <TableCell className="text-right font-semibold">{item.current_price}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
};

export default AssetTable;
