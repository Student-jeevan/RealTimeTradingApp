import React from 'react';
import { ShieldAlert, Activity, Gauge } from 'lucide-react';

/**
 * Risk metrics display: Max Drawdown, Volatility, Risk Score (with progress bar).
 */
const RiskMetrics = ({ data, loading }) => {
  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-6 space-y-4">
        <div className="skeleton h-5 w-32 rounded" />
        {[...Array(3)].map((_, i) => (
          <div key={i} className="skeleton h-16 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (!data) return null;

  const riskScore = data.riskScore || 0;
  const riskLevel = data.riskLevel || 'LOW';
  const maxDrawdown = parseFloat(data.maxDrawdown) || 0;
  const volatility = parseFloat(data.volatility) || 0;

  const riskColorMap = {
    LOW: { text: 'text-emerald-400', bg: 'bg-emerald-500', bar: 'from-emerald-500 to-emerald-400' },
    MEDIUM: { text: 'text-amber-400', bg: 'bg-amber-500', bar: 'from-amber-500 to-amber-400' },
    HIGH: { text: 'text-red-400', bg: 'bg-red-500', bar: 'from-red-500 to-red-400' },
  };

  const riskColors = riskColorMap[riskLevel] || riskColorMap.LOW;

  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-5">
        Risk Analysis
      </h3>

      {/* Risk Score - Hero */}
      <div className="rounded-xl bg-white/[0.02] border border-border/10 p-5 mb-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-3">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${riskColors.bg}/10`}>
              <Gauge className={`w-5 h-5 ${riskColors.text}`} />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Risk Score</p>
              <p className={`text-2xl font-bold ${riskColors.text}`}>{riskScore}</p>
            </div>
          </div>
          <span className={`text-xs font-bold px-3 py-1.5 rounded-full ${riskColors.bg}/10 ${riskColors.text}`}>
            {riskLevel}
          </span>
        </div>

        {/* Progress bar */}
        <div className="h-2.5 rounded-full bg-white/5 overflow-hidden">
          <div
            className={`h-full rounded-full bg-gradient-to-r ${riskColors.bar} transition-all duration-700 ease-out`}
            style={{ width: `${Math.min(riskScore, 100)}%` }}
          />
        </div>
        <div className="flex justify-between mt-1.5 text-[10px] text-muted-foreground/60">
          <span>Low Risk</span>
          <span>High Risk</span>
        </div>
      </div>

      {/* Metrics */}
      <div className="space-y-3">
        <div className="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-border/10">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg flex items-center justify-center bg-red-500/10">
              <ShieldAlert className="w-4 h-4 text-red-400" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Max Drawdown</p>
              <p className="text-sm font-semibold text-red-400">
                {maxDrawdown.toFixed(2)}%
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-border/10">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg flex items-center justify-center bg-amber-500/10">
              <Activity className="w-4 h-4 text-amber-400" />
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Volatility (Std Dev)</p>
              <p className="text-sm font-semibold text-amber-400">
                {volatility.toFixed(4)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RiskMetrics;
