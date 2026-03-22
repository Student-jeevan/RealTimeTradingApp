import React from 'react';
import Chart from "react-apexcharts";
import { Button } from '@/components/ui/button';
import { useDispatch, useSelector } from 'react-redux';
import { useEffect } from 'react';
import { fetchMarketChart } from '@/State/Coin/Action';

const timeSeries = [
  { keyword: "DIGITAL_CURRENCY_DAILY", key: "Time Series (Daily)", label: "1 Day", value: 1 },
  { keyword: "DIGITAL_CURRENCY_WEEKLY", key: "Weekly Time Series ", label: "1 Week ", value: 7 },
  { keyword: "DIGITAL_CURRENCY_MONTHLY", key: "Monthly Time Series ", label: "1 Month ", value: 30 },
  { keyword: "DIGITAL_CURRENCY_MONTHLY", key: "Yearly Time Series ", label: "1 year", value: 365 },
];

function StockChart({ coinId }) {
  const dispatch = useDispatch();
  const coin = useSelector((state) => state.coin);
  const [activeLable, setActiveLable] = React.useState("1 Day");

  const series = [
    {
      name: "Price",
      data: coin.marketChart?.data || [],
    },
  ];

  const options = {
    chart: {
      id: "area-datetime",
      type: "area",
      height: "100%",
      zoom: { autoScaleYaxis: true },
      toolbar: { show: false },
      background: 'transparent',
      fontFamily: 'Inter, sans-serif',
    },
    dataLabels: { enabled: false },
    xaxis: {
      type: "datetime",
      tickAmount: 6,
      labels: {
        style: { colors: '#94a3b8', fontSize: '10px' },
      },
      axisBorder: { show: false },
      axisTicks: { show: false },
    },
    yaxis: {
      labels: {
        style: { colors: '#94a3b8', fontSize: '10px' },
        formatter: (val) => {
          if (val >= 1000) return `$${(val / 1000).toFixed(1)}K`;
          return `$${val.toFixed(2)}`;
        },
      },
    },
    colors: ["#3b82f6"],
    stroke: {
      curve: 'smooth',
      width: 2,
    },
    markers: {
      colors: ["#fff"],
      strokeColors: "#3b82f6",
      strokeWidth: 2,
      size: 0,
      hover: { size: 5 },
    },
    tooltip: {
      theme: "dark",
      style: { fontSize: '12px' },
      x: { format: 'MMM dd, yyyy' },
    },
    fill: {
      type: "gradient",
      gradient: {
        shadeIntensity: 1,
        opacityFrom: 0.4,
        opacityTo: 0.05,
        stops: [0, 100],
        colorStops: [
          { offset: 0, color: '#3b82f6', opacity: 0.3 },
          { offset: 100, color: '#3b82f6', opacity: 0.02 },
        ],
      },
    },
    grid: {
      borderColor: "hsla(217, 32.6%, 22%, 0.3)",
      strokeDashArray: 4,
      show: true,
      xaxis: { lines: { show: false } },
      padding: { left: 8, right: 8 },
    },
  };

  const handleActiveLable = (value) => {
    setActiveLable(value);
  };

  useEffect(() => {
    if (coinId) {
      const selectedTimeSeries = timeSeries.find((item) => item.label === activeLable);
      const days = selectedTimeSeries ? selectedTimeSeries.value : 1;
      const jwt = localStorage.getItem("jwt");
      dispatch(fetchMarketChart(coinId, days, jwt));
    }
  }, [dispatch, coinId, activeLable]);

  return (
    <div className="w-full">
      {/* Time period tabs */}
      <div className="flex gap-1.5 mb-4 flex-wrap">
        {timeSeries.map((item) => (
          <button
            key={item.label}
            onClick={() => handleActiveLable(item.label)}
            className={`px-3 py-1 rounded-lg text-xs font-medium transition-all duration-200
              ${activeLable === item.label
                ? 'gradient-primary text-white shadow-lg shadow-blue-500/20'
                : 'bg-secondary/50 text-muted-foreground hover:bg-secondary hover:text-foreground'
              }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      {/* Chart container — responsive height, no overflow */}
      <div className="w-full h-[280px] relative overflow-hidden rounded-xl">
        <Chart
          options={options}
          series={series}
          height="100%"
          width="100%"
          type="area"
        />
      </div>
    </div>
  );
}

export default StockChart;
