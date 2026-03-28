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

  // Selected coin drives the chart — default to bitcoin
  const [selectedCoin, setSelectedCoin] = useState({ id: 'bitcoin', name: 'Bitcoin' });

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
        .catch(() => setLoadingAnalytics(false));
    } else {
      setLoadingAnalytics(false);
    }
  }, [dispatch]);

  // When top50 loads, default selected coin to the first one
  useEffect(() => {
    if (coin?.top50?.length > 0 && selectedCoin.id === 'bitcoin') {
      setSelectedCoin({ id: coin.top50[0].id, name: coin.top50[0].name });
    }
  }, [coin?.top50]);

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

  const handleCoinSelect = (coin) => {
    setSelectedCoin({ id: coin.id, name: coin.name });
  };

  return (
    <div className="space-y-6 animate-fade-in-up">

      {/* ===== Dashboard Header ===== */}
      <div>
        <h1 className="text-2xl font-bold">Dashboard</h1>
        <p className="text-sm text-muted-foreground mt-1">Welcome back, here's your portfolio overview</p>
      </div>

      {/* ===== 4 Stat Cards ===== */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Portfolio Value"
          value={`$${totalValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          icon={Wallet}
        />
        <StatCard
          title="Total PnL"
          value={portfolio?.totalPnl
            ? `${parseFloat(portfolio.totalPnl) >= 0 ? '+' : ''}$${Math.abs(parseFloat(portfolio.totalPnl)).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
            : '$0.00'}
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
          title="Today's Market"
          value={`${avgMarketChange >= 0 ? '+' : ''}${avgMarketChange.toFixed(2)}%`}
          icon={avgMarketChange >= 0 ? TrendingUp : TrendingDown}
          trend={avgMarketChange >= 0 ? 'up' : 'down'}
          trendValue="Top 50 avg"
        />
      </div>

      {/* ===== TOP SECTION: Coin List (left) + Price Chart (right) ===== */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

        {/* LEFT — Coin List (col-span-7), scrollable + paginated */}
        <div className="lg:col-span-7">
          <CoinList
            coins={coin?.top50 || []}
            loading={!coin?.top50 && loadingAnalytics}
            onCoinSelect={handleCoinSelect}
            selectedCoinId={selectedCoin.id}
          />
        </div>

        {/* RIGHT — Price Chart (col-span-5), updates on coin selection */}
        <div className="lg:col-span-5">
          <div className="glass-card rounded-2xl p-5 h-full flex flex-col">
            {/* Chart Header */}
            <div className="flex items-center justify-between mb-3 shrink-0">
              <div>
                <h3 className="text-sm font-semibold">{selectedCoin.name} Price</h3>
                <p className="text-xs text-muted-foreground mt-0.5">Click any coin to view its chart</p>
              </div>
              <span className="text-[10px] px-2 py-1 rounded-md bg-primary/10 text-primary font-medium">
                {selectedCoin.id}
              </span>
            </div>
            <div className="flex-1">
              <StockChart coinId={selectedCoin.id} />
            </div>
          </div>
        </div>
      </div>

      {/* ===== BOTTOM SECTION: Recent Transactions (full width) ===== */}
      <div className="glass-card rounded-2xl flex flex-col pb-6">
        <div className="flex items-center justify-between px-5 pt-5 pb-3 shrink-0">
          <h3 className="text-sm font-semibold text-muted-foreground">Recent Transactions</h3>
          <span className="text-[10px] text-muted-foreground/60">{recentOrders.length} records</span>
        </div>
        <div className="overflow-auto px-5">
          <Table>
            <TableHeader>
              <TableRow className="border-b border-border/20 hover:bg-transparent">
                <TableHead className="text-[11px]">Type</TableHead>
                <TableHead className="text-[11px]">Asset</TableHead>
                <TableHead className="text-right text-[11px]">Amount</TableHead>
                <TableHead className="text-right text-[11px]">Date</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recentOrders.map((orderItem, idx) => {
                const isBuy = orderItem.orderType === 'BUY';
                const d = new Date(orderItem.timestamp || orderItem.createdAt);
                return (
                  <TableRow key={idx} className="border-b border-border/10 hover:bg-secondary/20 transition-colors">
                    <TableCell>
                      <span className={`px-2 py-1 rounded-md text-[10px] font-bold uppercase
                        ${isBuy ? 'text-emerald-400 bg-emerald-500/10' : 'text-red-400 bg-red-500/10'}`}>
                        {orderItem.orderType}
                      </span>
                    </TableCell>
                    <TableCell className="font-medium text-sm">
                      <div className="flex items-center gap-2">
                        <Avatar className="h-6 w-6">
                          <AvatarImage src={orderItem.orderItem?.coin?.image} />
                          <AvatarFallback className="text-[10px]">
                            {orderItem.orderItem?.coin?.symbol?.charAt(0)?.toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                        <span>{orderItem.orderItem?.coin?.symbol?.toUpperCase()}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-right text-sm font-semibold">
                      ${orderItem.price?.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </TableCell>
                    <TableCell className="text-right text-xs text-muted-foreground">
                      {d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                    </TableCell>
                  </TableRow>
                );
              })}
              {recentOrders.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center py-8 text-muted-foreground text-xs">
                    No recent transactions
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
      </div>

    </div>
  );
};

export default React.memo(Home);
