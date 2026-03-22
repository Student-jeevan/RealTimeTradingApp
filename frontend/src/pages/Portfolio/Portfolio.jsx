import React, { useEffect } from 'react'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { useDispatch, useSelector } from 'react-redux';
import { getUserAssets } from '@/State/Asset/Action';
import { TrendingUp, TrendingDown, PieChart } from 'lucide-react';

function Portfolio() {
    const dispatch = useDispatch();
    const {asset} = useSelector((store)=>store)
    
    useEffect(()=>{
      const jwt = localStorage.getItem("jwt");
      if (jwt) {
        dispatch(getUserAssets(jwt));
      }
    },[dispatch])

    const totalValue = asset?.userAssets?.reduce(
      (sum, item) => sum + ((item?.quantity || 0) * (item?.coin?.current_price || item?.coin?.price || 0)), 0
    ) || 0;

    return (
        <div className='space-y-6'>
          {/* Header */}
          <div className="flex items-center justify-between">
            <div>
              <h1 className='text-2xl font-bold'>Portfolio</h1>
              <p className='text-sm text-muted-foreground mt-1'>Track your crypto holdings</p>
            </div>
            {totalValue > 0 && (
              <div className="glass-card rounded-2xl px-5 py-3 flex items-center gap-3">
                <PieChart className="w-5 h-5 text-primary" />
                <div>
                  <p className="text-[10px] text-muted-foreground uppercase tracking-wider">Total Value</p>
                  <p className="text-lg font-bold">${totalValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</p>
                </div>
              </div>
            )}
          </div>

          {/* Table */}
          <div className="glass-card rounded-2xl overflow-hidden">
             <Table className="w-full">
                  <TableHeader>
                    <TableRow className="border-b border-border/20 hover:bg-transparent">  
                      <TableHead className="text-muted-foreground text-[11px] font-semibold">Asset</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">Price</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">Holdings</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">24h Change</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">24h %</TableHead>
                      <TableHead className="text-right text-muted-foreground text-[11px] font-semibold">Value</TableHead>
                    </TableRow>
                  </TableHeader>
            
                  <TableBody>
                    {asset?.userAssets && asset.userAssets.length > 0 ? (
                      asset.userAssets.map((item, index) => {
                        const change = item?.coin?.price_change_24h || 0;
                        const changePct = item?.coin?.price_change_percentage_24h || 0;
                        const isPositive = change >= 0;
                        const value = ((item?.quantity || 0) * (item?.coin?.current_price || item?.coin?.price || 0));

                        return (
                          <TableRow key={index} className="border-b border-border/10 hover:bg-secondary/30 transition-colors">
                            <TableCell className="font-medium">
                              <div className="flex items-center gap-3">
                                <Avatar className="h-8 w-8">
                                  <AvatarImage src={item?.coin?.image}/>
                                  <AvatarFallback className="text-[10px]">{item?.coin?.symbol?.charAt(0)?.toUpperCase() || 'A'}</AvatarFallback>
                                </Avatar>
                                <div>
                                  <p className="text-sm font-semibold">{item?.coin?.name || 'N/A'}</p>
                                  <p className="text-[10px] text-muted-foreground uppercase">{item?.coin?.symbol}</p>
                                </div>
                              </div>
                            </TableCell>

                            <TableCell className="text-right text-sm font-medium">
                              ${(item?.coin?.current_price || item?.coin?.price || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </TableCell>
                            <TableCell className="text-right text-sm">{item?.quantity || 0}</TableCell>
                            <TableCell className={`text-right text-sm font-medium ${isPositive ? 'text-emerald-400' : 'text-red-400'}`}>
                              {isPositive ? '+' : ''}${change.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </TableCell>
                            <TableCell className="text-right">
                              <span className={`inline-flex items-center gap-0.5 text-xs font-semibold px-2 py-0.5 rounded-full
                                ${isPositive ? 'text-emerald-400 bg-emerald-500/10' : 'text-red-400 bg-red-500/10'}`}
                              >
                                {isPositive ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                                {changePct.toFixed(2)}%
                              </span>
                            </TableCell>
                            <TableCell className="text-right text-sm font-bold">
                              ${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </TableCell>
                          </TableRow>
                        );
                      })
                    ) : (
                      <TableRow>
                        <TableCell colSpan={6} className="text-center py-12 text-muted-foreground">
                          {asset?.loading ? (
                            <div className="flex flex-col items-center gap-2">
                              <div className="skeleton h-8 w-8 rounded-full" />
                              <p className="text-sm">Loading assets...</p>
                            </div>
                          ) : (
                            <div className="flex flex-col items-center gap-2">
                              <PieChart className="w-10 h-10 text-muted-foreground/40" />
                              <p className="text-sm">No assets found in your portfolio</p>
                              <p className="text-xs text-muted-foreground/60">Start trading to build your portfolio</p>
                            </div>
                          )}
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
          </div>
        </div>
    )
}
export default Portfolio
