import React, { useMemo } from 'react';
import Chart from 'react-apexcharts';

/**
 * Donut chart showing asset allocation percentages.
 * (Cache-busting comment)
 */
const AllocationChart = ({ data, loading }) => {
  const chartData = useMemo(() => {
    if (!data || data.length === 0) return null;

    return {
      series: data.map((d) => parseFloat(d.percentage) || 0),
      labels: data.map((d) => (d.coinSymbol || d.coinId || 'Unknown').toUpperCase()),
    };
  }, [data]);

  const options = useMemo(
    () => ({
      chart: {
        type: 'donut',
        background: 'transparent',
        animations: { enabled: true, easing: 'easeinout', speed: 800 },
      },
      labels: chartData?.labels || [],
      colors: [
        '#3b82f6', '#8b5cf6', '#06b6d4', '#f59e0b',
        '#ef4444', '#10b981', '#ec4899', '#64748b',
      ],
      stroke: { width: 2, colors: ['hsl(222, 47%, 6%)'] },
      dataLabels: { enabled: false },
      legend: {
        position: 'bottom',
        labels: { colors: '#94a3b8' },
        fontSize: '12px',
        fontFamily: 'Inter, sans-serif',
        markers: { width: 10, height: 10, radius: 3 },
      },
      plotOptions: {
        pie: {
          donut: {
            size: '72%',
            labels: {
              show: true,
              name: { fontSize: '13px', color: '#94a3b8', fontFamily: 'Inter, sans-serif' },
              value: {
                fontSize: '20px',
                fontWeight: 700,
                color: '#f1f5f9',
                fontFamily: 'Inter, sans-serif',
                formatter: (val) => `${parseFloat(val).toFixed(1)}%`,
              },
              total: {
                show: true,
                label: 'Total',
                color: '#94a3b8',
                fontFamily: 'Inter, sans-serif',
                formatter: () => `${data?.length || 0} assets`,
              },
            },
          },
        },
      },
      tooltip: {
        theme: 'dark',
        y: { formatter: (val) => `${val.toFixed(1)}%` },
      },
      responsive: [
        {
          breakpoint: 640,
          options: { chart: { height: 280 }, legend: { position: 'bottom' } },
        },
      ],
    }),
    [chartData, data]
  );

  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <div className="skeleton h-5 w-36 rounded mb-6" />
        <div className="skeleton h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (!chartData || chartData.series.length === 0) {
    return (
      <div className="glass-card rounded-2xl p-6">
        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-6">
          Asset Allocation
        </h3>
        <div className="flex flex-col items-center justify-center h-48 text-muted-foreground">
          <p className="text-sm">No assets to display</p>
        </div>
      </div>
    );
  }

  return (
    <div className="glass-card rounded-2xl p-6">
      <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-4">
        Asset Allocation
      </h3>
      <Chart
        options={options}
        series={chartData.series}
        type="donut"
        height={320}
      />

      {/* Asset list */}
      <div className="mt-4 space-y-2">
        {data.map((asset, i) => (
          <div key={asset.coinId} className="flex items-center justify-between text-sm px-1">
            <div className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-sm"
                style={{
                  backgroundColor: [
                    '#3b82f6', '#8b5cf6', '#06b6d4', '#f59e0b',
                    '#ef4444', '#10b981', '#ec4899', '#64748b',
                  ][i % 8],
                }}
              />
              <span className="text-muted-foreground">
                {(asset.coinSymbol || asset.coinId).toUpperCase()}
              </span>
            </div>
            <div className="text-right">
              <span className="font-medium">
                ${parseFloat(asset.currentValue || 0).toLocaleString('en-US', {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </span>
              <span className="text-muted-foreground ml-2 text-xs">
                {parseFloat(asset.percentage || 0).toFixed(1)}%
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AllocationChart;
