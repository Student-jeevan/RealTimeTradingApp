import React from 'react';
import {
  TrendingUp,
  TrendingDown,
  DollarSign,
  PieChart,
  Target,
  BarChart3,
} from 'lucide-react';

/**
 * Top-row portfolio overview cards.
 * Shows: Total Portfolio Value, Realized PnL, Unrealized PnL, Total Invested.
 */
const AnalyticsCards = ({ data, loading }) => {
  const cards = [
    {
      title: 'Portfolio Value',
      value: data?.currentPortfolioValue,
      icon: DollarSign,
      gradient: 'from-blue-500/20 to-indigo-500/20',
      iconColor: 'text-blue-400',
      borderColor: 'border-blue-500/20',
    },
    {
      title: 'Realized PnL',
      value: data?.realizedPnl,
      icon: Target,
      isPnl: true,
      gradient: 'from-emerald-500/20 to-green-500/20',
      gradientNeg: 'from-red-500/20 to-rose-500/20',
      iconColor: 'text-emerald-400',
      iconColorNeg: 'text-red-400',
      borderColor: 'border-emerald-500/20',
      borderColorNeg: 'border-red-500/20',
    },
    {
      title: 'Unrealized PnL',
      value: data?.unrealizedPnl,
      icon: BarChart3,
      isPnl: true,
      gradient: 'from-emerald-500/20 to-green-500/20',
      gradientNeg: 'from-red-500/20 to-rose-500/20',
      iconColor: 'text-emerald-400',
      iconColorNeg: 'text-red-400',
      borderColor: 'border-emerald-500/20',
      borderColorNeg: 'border-red-500/20',
    },
    {
      title: 'Total Invested',
      value: data?.totalInvested,
      icon: PieChart,
      gradient: 'from-violet-500/20 to-purple-500/20',
      iconColor: 'text-violet-400',
      borderColor: 'border-violet-500/20',
    },
  ];

  if (loading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="glass-card rounded-2xl p-5 space-y-3">
            <div className="skeleton h-4 w-24 rounded" />
            <div className="skeleton h-8 w-32 rounded" />
            <div className="skeleton h-3 w-16 rounded" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((card) => {
        const numValue = parseFloat(card.value) || 0;
        const isNegative = card.isPnl && numValue < 0;
        const Icon = card.icon;

        return (
          <div
            key={card.title}
            className={`relative overflow-hidden rounded-2xl border p-5 transition-all duration-300 hover:scale-[1.02]
              ${isNegative ? card.borderColorNeg || card.borderColor : card.borderColor}
              bg-gradient-to-br ${isNegative ? card.gradientNeg || card.gradient : card.gradient}`}
            style={{ background: 'linear-gradient(135deg, hsla(217,33%,10%,0.8), hsla(222,47%,8%,0.6))' }}
          >
            {/* Gradient overlay */}
            <div className={`absolute inset-0 bg-gradient-to-br ${isNegative ? card.gradientNeg || card.gradient : card.gradient} opacity-40`} />

            <div className="relative z-10">
              <div className="flex items-center justify-between mb-3">
                <p className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wider">
                  {card.title}
                </p>
                <div className={`w-9 h-9 rounded-xl flex items-center justify-center bg-white/5 ${isNegative ? card.iconColorNeg || card.iconColor : card.iconColor}`}>
                  <Icon className="w-4 h-4" />
                </div>
              </div>

              <p className="text-2xl font-bold tracking-tight">
                {card.isPnl && numValue >= 0 && '+'}
                ${Math.abs(numValue).toLocaleString('en-US', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </p>

              {card.isPnl && (
                <div className={`flex items-center gap-1 mt-2 text-xs font-medium ${numValue >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                  {numValue >= 0 ? (
                    <TrendingUp className="w-3 h-3" />
                  ) : (
                    <TrendingDown className="w-3 h-3" />
                  )}
                  <span>{numValue >= 0 ? 'Profit' : 'Loss'}</span>
                </div>
              )}

              {data?.totalPnlPercentage != null && card.title === 'Portfolio Value' && (
                <div className={`flex items-center gap-1 mt-2 text-xs font-medium ${parseFloat(data.totalPnlPercentage) >= 0 ? 'text-emerald-400' : 'text-red-400'}`}>
                  {parseFloat(data.totalPnlPercentage) >= 0 ? (
                    <TrendingUp className="w-3 h-3" />
                  ) : (
                    <TrendingDown className="w-3 h-3" />
                  )}
                  <span>{parseFloat(data.totalPnlPercentage).toFixed(2)}% PnL</span>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default AnalyticsCards;
