import React from 'react';
import ApexChart from 'react-apexcharts';

const Chart = ({ series, options, type = "area", height = 300 }) => {
  return (
    <div style={{ height: `${height}px` }} className="w-full relative overflow-hidden rounded-xl">
      <ApexChart
        options={options}
        series={series}
        type={type}
        height="100%"
        width="100%"
      />
    </div>
  );
};

export default React.memo(Chart);
