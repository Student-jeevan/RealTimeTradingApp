import React from 'react';

const StatCard = ({ title, value, icon: Icon, trend, trendValue }) => {
  return (
    <div className="glass-card p-6 rounded-2xl flex flex-col justify-between">
      <div className="flex justify-between items-start mb-4">
        <div className="text-muted-foreground text-sm font-medium">{title}</div>
        {Icon && (
          <div className="p-2 bg-secondary/50 rounded-lg text-primary">
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>
      <div>
        <div className="text-2xl font-bold tracking-tight text-foreground">{value}</div>
        {trend && (
          <div className={`text-sm mt-1 flex items-center gap-1 font-medium ${trend === 'up' ? 'text-green-500' : trend === 'down' ? 'text-red-500' : 'text-muted-foreground'}`}>
            <span>{trendValue}</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default React.memo(StatCard);
