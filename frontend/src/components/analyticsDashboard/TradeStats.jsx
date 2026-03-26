import React from 'react';
import { Trophy, XCircle, Target, TrendingUp, BarChart3, Zap } from 'lucide-react';

/**
 * Trade performance stats: total trades, win rate, avg profit, best/worst trade.
 */
const TradeStats = ({ data, loading }) => {
  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-6 space-y-4">
        <div className="skeleton h-5 w-40 rounded" />
        {[...Array(5)].map((_, i) => (
          <div key={i} className="skeleton h-12 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (!data) return null;

  const winRate = parseFloat(data.winRate) || 0;
  const avgProfit = parseFloat(data.avgProfitPerTrade) || 0;
  const bestTrade = parseFloat(data.bestTradeProfit) || 0;
  const worstTrade = parseFloat(data.worstTradeProfit) || 0;

  const stats = [
    {
      label: 'Total Trades',
      value: data.totalTrades || 0,
      icon: BarChart3,
      color: 'text-blue-400',
      bg: 'bg-blue-500/10',
    },
    {
      label: 'Win Rate',
      value: `${winRate.toFixed(1)}%`,
      icon: Target,
      color: winRate >= 50 ? 'text-emerald-400' : 'text-amber-400',
      bg: winRate >= 50 ? 'bg-emerald-500/10' : 'bg-amber-500/10',
    },
    {
      label: 'Avg Profit / Trade',
      value: `${avgProfit >= 0 ? '+' : ''}$${Math.abs(avgProfit).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
      icon: TrendingUp,
      color: avgProfit >= 0 ? 'text-emerald-400' : 'text-red-400',
      bg: avgProfit >= 0 ? 'bg-emerald-500/10' : 'bg-red-500/10',
    },
    {
      label: 'Best Trade',
      value: `+$${bestTrade.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
      icon: Trophy,
      color: 'text-amber-400',
      bg: 'bg-amber-500/10',
    },
    {
      label: 'Worst Trade',
      value: `-$${Math.abs(worstTrade).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
      icon: XCircle,
      color: 'text-red-400',
      bg: 'bg-red-500/10',
    },
  ];

  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-5">
        Trade Performance
      </h3>

      <div className="space-y-3">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div
              key={stat.label}
              className="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-border/10 hover:border-border/30 transition-colors"
            >
              <div className="flex items-center gap-3">
                <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${stat.bg}`}>
                  <Icon className={`w-4 h-4 ${stat.color}`} />
                </div>
                <span className="text-sm text-muted-foreground">{stat.label}</span>
              </div>
              <span className={`text-sm font-bold ${stat.color}`}>{stat.value}</span>
            </div>
          );
        })}
      </div>

      {/* Win/Loss bar */}
      {(data.winCount > 0 || data.lossCount > 0) && (
        <div className="mt-5">
          <div className="flex items-center justify-between text-xs text-muted-foreground mb-2">
            <span className="text-emerald-400">{data.winCount} wins</span>
            <span className="text-red-400">{data.lossCount} losses</span>
          </div>
          <div className="h-2 rounded-full bg-white/5 overflow-hidden flex">
            <div
              className="h-full bg-gradient-to-r from-emerald-500 to-emerald-400 rounded-l-full transition-all duration-500"
              style={{ width: `${winRate}%` }}
            />
            <div
              className="h-full bg-gradient-to-r from-red-400 to-red-500 rounded-r-full transition-all duration-500"
              style={{ width: `${100 - winRate}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default TradeStats;
