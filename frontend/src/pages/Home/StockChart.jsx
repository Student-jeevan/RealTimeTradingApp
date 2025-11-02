import React from 'react';
import Chart from "react-apexcharts";
import ReactApexChart from 'react-apexcharts';
import { Button } from '@/components/ui/button';
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

];
function StockChart() {
    const [activeLable , setActiveLable] = React.useState("1 Day");
  const series = [
    {
      name: "Stock Price",
      data: [
       [1761919254401, 110063.440989497],
    [1761922893542, 110588.715837899],
    [1761926491982, 110323.4911411],
    [1761930066568, 109076.49487045],
    [1761933676681, 109260.980835603],
    [1761937300715, 109297.637469183],
    [1761940869872, 109935.673957773],
    [1761944517945, 109423.682319654],
    [1761948082968, 109554.259829857],
    [1761951689839, 109571.261802111],
    [1761955295221, 109573.905556291],
    [1761958895977, 109633.203947427],
    [1761962512384, 109717.24113096],
    [1761966056113, 109737.306277463],
    [1761969754311, 110082.9363431],
    [1761973590222, 110129.122472592],
    [1761976904553, 110124.535045069],
    [1761980487560, 109931.104052825],
    [1761984074797, 110017.09767501],
    [1761987725521, 110159.766144421],
    [1761991274015, 109919.604079423],
    [1761995251814, 110109.543119504],
    [1761998495785, 110082.743738497],
    [1762002096558, 109985.338507768],
    [1762005994955, 109909.423794201],
    [1762009305106, 109850.436998377],
    [1762012939121, 110123.151508401],
    [1762016475818, 110433.817306904],
    [1762020082228, 110211.912774415],
    [1762023665343, 110328.646179277],
    [1762027283421, 110277.991378611],
    [1762030939575, 110387.562648447],
    [1762034503749, 110015.896441541],
    [1762038083125, 110026.69447299],
    [1762041738020, 110066.941740518],
    [1762045282452, 109932.61994702],
    [1762048916081, 109953.557982315],
    [1762052684302, 109967.137028754],
    [1762056063966, 110006.751618011],
    [1762059680129, 110505.725059069],
    [1762063279654, 110444.548455353],
    [1762066912707, 110658.135180899],
    [1762070454136, 110883.209448248],
    [1762074121768, 110778.323488943],
    [1762077613238, 110596.231947484],
    [1762081294946, 110391.521729862],
    [1762084915141, 111057.647933644],
    [1762088593784, 110688.825329624],
    [1762090276000, 110355.909706409]
      ],
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
  return (
    <div>
        <div className="space-x-3">
            {timeSeries.map((item)=>
                <Button variant={activeLable==item.label?"":"outline"} onClick={()=>handleActiveLable(item.label)} key={item.label}>{item.label}</Button>
            )}
        </div>
        <div id="chart-timeline">
            <Chart options={options} series={series} height={450} type="area"/>
        </div>
    </div>
  );
}

export default StockChart;
