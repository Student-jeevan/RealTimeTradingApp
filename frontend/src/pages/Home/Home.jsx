import React, { useEffect, useState, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { getTop50CoinList } from '@/State/Coin/Action';
import { getUserAssets } from '@/State/Asset/Action';
import { getAllOrdersForUser } from '@/State/Order/Action';
import { fetchPortfolioMetrics } from '@/services/analyticsApi';
import { Wallet, TrendingUp, TrendingDown, Layers, Activity } from 'lucide-react';

import StatCard from '@/components/ui/StatCard';
import { Table, TableRow, TableCell, TableHeader, TableHead, TableBody } from '@/components/ui/table';
import StockChart from './StockChart';
import { SkeletonPage } from '@/components/ui/SkeletonLoader';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import CoinList from '@/components/dashboard/CoinList';

const Home = () => {
  const dispatch = useDispatch();
  const { coin, asset, order } = useSelector((state) => state);
  const [portfolio, setPortfolio] = useState(null);
  const [loadingAnalytics, setLoadingAnalytics] = useState(true);

  useEffect(() => {
    dispatch(getTop50CoinList());
    const jwt = localStorage.getItem("jwt");
    if (jwt) {
      dispatch(getUserAssets(jwt));
      dispatch(getAllOrdersForUser({ jwt }));
      
      fetchPortfolioMetrics()
        .then(res => {
          setPortfolio(res);
          setLoadingAnalytics(false);
        })
        .catch(e => {
          setLoadingAnalytics(false);
        });
    } else {
      setLoadingAnalytics(false);
    }
  }, [dispatch]);

  const totalValue = useMemo(() => {
    return asset?.userAssets?.reduce(
      (sum, item) => sum + ((item?.quantity || 0) * (item?.coin?.current_price || item?.coin?.price || 0)), 0
    ) || 0;
  }, [asset?.userAssets]);

  const avgMarketChange = useMemo(() => {
    const coins = coin?.top50 || [];
    return coins.length > 0 
      ? coins.reduce((sum, c) => sum + (c.price_change_percentage_24h || 0), 0) / coins.length 
      : 0;
  }, [coin?.top50]);

  if (!coin?.top50 && loadingAnalytics) {
    return <SkeletonPage />;
  }

  const recentOrders = order?.orders?.slice(0, 8) || [];
  const activeAssets = asset?.userAssets?.slice(0, 8) || [];

  return (
    <div className='relative space-y-6 animate-fade-in-up'>
      {/* ===== Dashboard Header ===== */}
      <div>
        <h1 className='text-2xl font-bold'>Dashboard</h1>
        <p className='text-sm text-muted-foreground mt-1'>Welcome back, here's your portfolio overview</p>
      </div>

      {/* ===== 4 Top Cards ===== */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Portfolio Value"
          value={`$${totalValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          icon={Wallet}
        />
        <StatCard
          title="Total PnL"
          value={portfolio?.totalPnl ? `${parseFloat(portfolio.totalPnl) >= 0 ? '+' : ''}$${Math.abs(parseFloat(portfolio.totalPnl)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : '$0.00'}
          icon={Activity}
          trend={parseFloat(portfolio?.totalPnl || 0) >= 0 ? 'up' : 'down'}
          trendValue="All time"
        />
        <StatCard
          title="Active Assets"
          value={asset?.userAssets?.length || 0}
          icon={Layers}
        />
        <StatCard
          title="Today's Market Trend"
          value={`${avgMarketChange >= 0 ? '+' : ''}${avgMarketChange.toFixed(2)}%`}
          icon={avgMarketChange >= 0 ? TrendingUp : TrendingDown}
          trend={avgMarketChange >= 0 ? 'up' : 'down'}
          trendValue="Top 50 avg"
        />
      </div>

      {/* ===== Single Main Chart ===== */}
      <div className="glass-card rounded-2xl p-5 space-y-4">
        <h3 className="text-sm font-semibold text-muted-foreground">Portfolio Performance Proxy (BTC)</h3>
        <StockChart coinId="bitcoin" />
      </div>

      {/* ===== Grid for Assets & Transactions ===== */}
      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6 pb-12">
        {/* Assets List */}
        <div className="glass-card rounded-2xl flex flex-col pt-5">
          <h3 className="text-sm font-semibold text-muted-foreground px-5 pb-3">Top Holdings</h3>
          <div className="flex-1 overflow-auto px-5 pb-5">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Asset</TableHead>
                  <TableHead className="text-right">Price</TableHead>
                  <TableHead className="text-right">Holdings</TableHead>
                  <TableHead className="text-right">Value</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {activeAssets.map((item, idx) => {
                  const val = ((item?.quantity || 0) * (item?.coin?.current_price || item?.coin?.price || 0));
                  const change = item?.coin?.price_change_percentage_24h || 0;
                  return (
                    <TableRow key={idx}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <Avatar className="h-8 w-8">
                            <AvatarImage src={item?.coin?.image} />
                            <AvatarFallback className="text-[10px]">{item?.coin?.symbol?.charAt(0)?.toUpperCase()}</AvatarFallback>
                          </Avatar>
                          <div className="flex flex-col">
                            <span className="text-xs font-semibold">{item?.coin?.symbol?.toUpperCase()}</span>
                            <span className={`text-[10px] ${change >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                              {change >= 0 ? '+' : ''}{change.toFixed(2)}%
                            </span>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-right text-sm">${(item?.coin?.current_price || item?.coin?.price || 0).toLocaleString(undefined, {minimumFractionDigits: 2})}</TableCell>
                      <TableCell className="text-right text-sm">{item?.quantity?.toFixed(4)}</TableCell>
                      <TableCell className="text-right text-sm font-bold">${val.toLocaleString('en-US', { minimumFractionDigits: 2 })}</TableCell>
                    </TableRow>
                  )
                })}
                {activeAssets.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-6 text-muted-foreground text-xs">No active assets found</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </div>

        {/* Recent Transactions */}
        <div className="glass-card rounded-2xl flex flex-col pt-5">
          <h3 className="text-sm font-semibold text-muted-foreground px-5 pb-3">Recent Transactions</h3>
          <div className="flex-1 overflow-auto px-5 pb-5">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Type</TableHead>
                  <TableHead>Asset</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead className="text-right">Date</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {recentOrders.map((orderItem, idx) => {
                  const isBuy = orderItem.orderType === 'BUY';
                  const d = new Date(orderItem.timestamp || orderItem.createdAt);
                  return (
                    <TableRow key={idx}>
                      <TableCell>
                        <span className={`px-2 py-1 rounded-md text-[10px] font-bold uppercase ${isBuy ? 'text-emerald-400 bg-emerald-500/10' : 'text-red-400 bg-red-500/10'}`}>
                          {orderItem.orderType}
                        </span>
                      </TableCell>
                      <TableCell className="font-medium text-sm flex items-center gap-2">
                          <Avatar className="h-6 w-6">
                              <AvatarImage src={orderItem.orderItem?.coin?.image} />
                              <AvatarFallback className="text-[10px]">{orderItem.orderItem?.coin?.symbol?.charAt(0)?.toUpperCase()}</AvatarFallback>
                          </Avatar>
                        {orderItem.orderItem?.coin?.symbol?.toUpperCase()}
                      </TableCell>
                      <TableCell className="text-right text-sm font-semibold">
                        ${orderItem.price?.toLocaleString(undefined, {minimumFractionDigits: 2})}
                      </TableCell>
                      <TableCell className="text-right text-xs text-muted-foreground">
                        {d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                      </TableCell>
                    </TableRow>
                  )
                })}
                {recentOrders.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-6 text-muted-foreground text-xs">No recent transactions</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </div>
      </div>

      {/* ===== Market Overview ===== */}
      <CoinList coins={coin?.top50 || []} loading={!coin?.top50 && loadingAnalytics} />
      
    </div>
  );
};

export default React.memo(Home);
