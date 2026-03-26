import React, { useMemo } from 'react';
import Chart from 'react-apexcharts';

/**
 * Portfolio performance area chart.
 * Shows portfolio value trend over time.
 * Uses the totalPnlPercentage from the portfolio metrics as summary.
 */
const PerformanceChart = ({ portfolioData, loading }) => {
  // Generate mock time-series from available data for demo purposes.
  // In production, this would come from a /api/analytics/snapshots endpoint.
  const chartData = useMemo(() => {
    if (!portfolioData) return null;

    const value = parseFloat(portfolioData.currentPortfolioValue) || 0;
    const invested = parseFloat(portfolioData.totalInvested) || 0;

    if (value === 0 && invested === 0) return null;

    // Generate 30-day simulated history based on current values
    const days = 30;
    const now = new Date();
    const categories = [];
    const portfolioSeries = [];
    const investedSeries = [];

    for (let i = days - 1; i >= 0; i--) {
      const date = new Date(now);
      date.setDate(date.getDate() - i);
      categories.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));

      // Simulate gradual growth from invested to current value
      const progress = (days - i) / days;
      const noise = 1 + (Math.sin(i * 0.5) * 0.03);
      const interpolated = invested + (value - invested) * progress * noise;

      portfolioSeries.push(Math.max(0, parseFloat(interpolated.toFixed(2))));
      investedSeries.push(parseFloat(invested.toFixed(2)));
    }

    return { categories, portfolioSeries, investedSeries };
  }, [portfolioData]);

  const options = useMemo(
    () => ({
      chart: {
        type: 'area',
        background: 'transparent',
        toolbar: { show: false },
        zoom: { enabled: false },
        animations: { enabled: true, easing: 'easeinout', speed: 800 },
      },
      grid: {
        borderColor: 'hsla(217, 32.6%, 22%, 0.2)',
        strokeDashArray: 3,
        xaxis: { lines: { show: false } },
      },
      stroke: { curve: 'smooth', width: [2.5, 1.5] },
      fill: {
        type: 'gradient',
        gradient: {
          shadeIntensity: 1,
          opacityFrom: 0.25,
          opacityTo: 0.02,
          stops: [0, 100],
        },
      },
      colors: ['#3b82f6', '#8b5cf6'],
      xaxis: {
        categories: chartData?.categories || [],
        labels: {
          style: { colors: '#64748b', fontSize: '10px', fontFamily: 'Inter' },
          rotate: -45,
          rotateAlways: false,
          maxHeight: 40,
        },
        axisBorder: { show: false },
        axisTicks: { show: false },
        tickAmount: 8,
      },
      yaxis: {
        labels: {
          style: { colors: '#64748b', fontSize: '11px', fontFamily: 'Inter' },
          formatter: (val) => `$${val >= 1000 ? (val / 1000).toFixed(1) + 'K' : val.toFixed(0)}`,
        },
      },
      tooltip: {
        theme: 'dark',
        x: { show: true },
        y: {
          formatter: (val) =>
            `$${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
        },
      },
      legend: {
        labels: { colors: '#94a3b8' },
        fontSize: '12px',
        fontFamily: 'Inter, sans-serif',
      },
      dataLabels: { enabled: false },
    }),
    [chartData]
  );

  const series = useMemo(
    () => [
      { name: 'Portfolio Value', data: chartData?.portfolioSeries || [] },
      { name: 'Invested', data: chartData?.investedSeries || [] },
    ],
    [chartData]
  );

  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="skeleton h-5 w-48 rounded mb-6" />
        <div className="skeleton h-72 w-full rounded-xl" />
      </div>
    );
  }

  if (!chartData) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-6">
          Portfolio Performance
        </h3>
        <div className="flex flex-col items-center justify-center h-48 text-muted-foreground">
          <p className="text-sm">No performance data yet</p>
          <p className="text-xs text-muted-foreground/60 mt-1">Start trading to see your chart</p>
        </div>
      </div>
    );
  }

  return (
    <div className="glass-card rounded-2xl p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider">
          Portfolio Performance
        </h3>
        {portfolioData?.totalPnlPercentage != null && (
          <span
            className={`text-xs font-bold px-2.5 py-1 rounded-full ${
              parseFloat(portfolioData.totalPnlPercentage) >= 0
                ? 'text-emerald-400 bg-emerald-500/10'
                : 'text-red-400 bg-red-500/10'
            }`}
          >
            {parseFloat(portfolioData.totalPnlPercentage) >= 0 ? '+' : ''}
            {parseFloat(portfolioData.totalPnlPercentage).toFixed(2)}%
          </span>
        )}
      </div>
      <Chart options={options} series={series} type="area" height={300} />
    </div>
  );
};

export default PerformanceChart;
