import React, { useState, useEffect } from 'react';
import { BarChart3, RefreshCw } from 'lucide-react';
import AnalyticsCards from '@/components/analyticsDashboard/AnalyticsCards';
import PerformanceChart from '@/components/analyticsDashboard/PerformanceChart';
import AllocationChart from '@/components/analyticsDashboard/AllocChart';
import TradeStats from '@/components/analyticsDashboard/TradeStats';
import RiskMetrics from '@/components/analyticsDashboard/RiskMetrics';
import {
  fetchPortfolioMetrics,
  fetchTradePerformance,
  fetchRiskMetrics,
  fetchAssetAllocation,
} from '@/services/analyticsApi';

/**
 * Analytics Dashboard Page.
 * Fetches all analytics data and renders overview cards, charts, and metrics.
 */
const Analytics = () => {
  const [portfolio, setPortfolio] = useState(null);
  const [performance, setPerformance] = useState(null);
  const [risk, setRisk] = useState(null);
  const [allocation, setAllocation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadAnalytics = async () => {
    setLoading(true);
    setError(null);

    try {
      const [portfolioRes, performanceRes, riskRes, allocationRes] = await Promise.allSettled([
        fetchPortfolioMetrics(),
        fetchTradePerformance(),
        fetchRiskMetrics(),
        fetchAssetAllocation(),
      ]);

      if (portfolioRes.status === 'fulfilled') setPortfolio(portfolioRes.value);
      if (performanceRes.status === 'fulfilled') setPerformance(performanceRes.value);
      if (riskRes.status === 'fulfilled') setRisk(riskRes.value);
      if (allocationRes.status === 'fulfilled') setAllocation(allocationRes.value);

      // Check if all failed
      const allFailed = [portfolioRes, performanceRes, riskRes, allocationRes].every(
        (r) => r.status === 'rejected'
      );
      if (allFailed) {
        setError('Failed to load analytics data. Please try again.');
      }
    } catch (e) {
      setError('Failed to load analytics data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAnalytics();
  }, []);

  return (
    <div className="space-y-6 animate-fade-in-up">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Analytics</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Track your trading performance and portfolio insights
          </p>
        </div>
        <button
          onClick={loadAnalytics}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium
                     bg-white/5 border border-border/20 text-muted-foreground
                     hover:bg-white/10 hover:text-foreground transition-all duration-200
                     disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Error State */}
      {error && (
        <div className="glass-card rounded-2xl p-5 border border-red-500/20 bg-red-500/5">
          <div className="flex items-center gap-3">
            <BarChart3 className="w-5 h-5 text-red-400" />
            <div>
              <p className="text-sm font-medium text-red-400">{error}</p>
              <button
                onClick={loadAnalytics}
                className="text-xs text-red-400/80 hover:text-red-300 underline mt-1"
              >
                Retry
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Portfolio Overview Cards */}
      <AnalyticsCards data={portfolio} loading={loading} />

      {/* Performance Chart (full width) */}
      <PerformanceChart portfolioData={portfolio} loading={loading} />

      {/* Two-column: Allocation + Trade Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <AllocationChart data={allocation} loading={loading} />
        <TradeStats data={performance} loading={loading} />
      </div>

      {/* Risk Metrics (full width on small, half on large) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <RiskMetrics data={risk} loading={loading} />

        {/* Summary card */}
        <div className="glass-card rounded-2xl p-6">
          <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-5">
            Quick Summary
          </h3>
          <div className="space-y-4">
            {[
              {
                label: 'Total PnL',
                value: portfolio
                  ? `${parseFloat(portfolio.totalPnl) >= 0 ? '+' : ''}$${Math.abs(parseFloat(portfolio.totalPnl) || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                  : '--',
                color: portfolio && parseFloat(portfolio.totalPnl) >= 0 ? 'text-emerald-400' : 'text-red-400',
              },
              {
                label: 'Total Deposits',
                value: portfolio
                  ? `$${parseFloat(portfolio.totalDeposits || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                  : '--',
                color: 'text-blue-400',
              },
              {
                label: 'Total Withdrawals',
                value: portfolio
                  ? `$${parseFloat(portfolio.totalWithdrawals || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                  : '--',
                color: 'text-violet-400',
              },
              {
                label: 'Win Rate',
                value: performance ? `${parseFloat(performance.winRate || 0).toFixed(1)}%` : '--',
                color: performance && parseFloat(performance.winRate) >= 50 ? 'text-emerald-400' : 'text-amber-400',
              },
              {
                label: 'Assets Held',
                value: allocation ? `${allocation.length} coins` : '--',
                color: 'text-cyan-400',
              },
              {
                label: 'Risk Level',
                value: risk?.riskLevel || '--',
                color: risk?.riskLevel === 'LOW' ? 'text-emerald-400' : risk?.riskLevel === 'HIGH' ? 'text-red-400' : 'text-amber-400',
              },
            ].map((item) => (
              <div key={item.label} className="flex items-center justify-between py-2 border-b border-border/10 last:border-0">
                <span className="text-sm text-muted-foreground">{item.label}</span>
                <span className={`text-sm font-bold ${item.color}`}>
                  {loading ? <span className="skeleton inline-block h-4 w-16 rounded" /> : item.value}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
