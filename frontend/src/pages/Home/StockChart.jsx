import React from 'react';
import Chart from "react-apexcharts";
import { Button } from '@/components/ui/button';
import { useDispatch, useSelector } from 'react-redux';
import { useEffect } from 'react';
import { fetchMarketChart } from '@/State/Coin/Action';
const timeSeries = [{
    keyword: "DIGITAL_CURRENCY_DAILY",
    key: "Time Series (Daily)",
    label: "1 Day",
    value: 1,    
    },
    {
    keyword: "DIGITAL_CURRENCY_WEEKLY",
    key: "Weekly Time Series ",
    label: "1 Week ",
    value: 7,    
    },

    {
    keyword: "DIGITAL_CURRENCY_MONTHLY",
    key: "Monthly Time Series ",
    label: "1 Month ",
    value: 30,    
    },
    {
    keyword: "DIGITAL_CURRENCY_MONTHLY",
    key: "Yearly Time Series ",
    label: "1 year",
    value: 365,    
    },

];
function StockChart({ coinId }) {
  const dispatch = useDispatch()
  const coin = useSelector(state => state.coin)
    const [activeLable , setActiveLable] = React.useState("1 Day");
  
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
      height: 450,
      zoom: {
        autoScaleYaxis: true,
      },
    },
    dataLabels: {
      enabled: false,
    },
    xaxis: {
      type: "datetime",
      tickAmount: 6,
    },
    colors:["#758AA2"],
    markers: {
      colors: ["#fff"],
      strokeColors: "#fff",
      strokeWidth: 1,
      size: 0,
      style: "hollow",
    },
    tooltip: {
      theme: "dark",
    },
    fill: {
      type: "gradient",
      gradient: {
        shadeIntensity: 1,
        opacityFrom: 0.7,
        opacityTo: 0.9,
        stops: [0, 100],
      },
    },
    grid: {
      borderColor: "#47535E",
      strokeDashArray: 4,
      show: true,
    },
  };
  const handleActiveLable=(value)=>{
    setActiveLable(value);
  }
  
  useEffect(()=>{
    if (coinId) {
      const selectedTimeSeries = timeSeries.find(item => item.label === activeLable);
      const days = selectedTimeSeries ? selectedTimeSeries.value : 1;
      const jwt = localStorage.getItem("jwt");
      dispatch(fetchMarketChart(coinId, days, jwt));
    }
  },[dispatch, coinId, activeLable])
  return (
    <div>
        <div className="space-x-3">
            {timeSeries.map((item)=>
                <Button variant={activeLable === item.label ? "default" : "outline"} onClick={()=>handleActiveLable(item.label)} key={item.label}>{item.label}</Button>
            )}
        </div>
        <div id="chart-timeline">
            <Chart options={options} series={series} height={450} type="area"/>
        </div>
    </div>
  );
}

export default StockChart;
