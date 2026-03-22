import React from 'react';

/**
 * Reusable stat card with gradient icon background,
 * title, value, and optional trend indicator.
 */
const StatCard = ({
  title,
  value,
  icon,
  trend,
  trendLabel = 'vs yesterday',
  gradient = 'primary',
  className = '',
}) => {
  const iconGradientMap = {
    primary: 'from-blue-500 to-purple-600',
    success: 'from-emerald-500 to-teal-600',
    warning: 'from-amber-500 to-orange-600',
    danger: 'from-red-500 to-pink-600',
  };

  return (
    <div
      className={`glass-card rounded-2xl p-5 hover:scale-[1.02] transition-all duration-300 animate-fade-in-up ${className}`}
    >
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
            {title}
          </p>
          <p className="text-2xl font-bold">{value}</p>
          {trend !== undefined && (
            <div className="flex items-center gap-1 mt-1">
              <span
                className={`text-xs font-semibold ${
                  trend >= 0 ? 'text-emerald-400' : 'text-red-400'
                }`}
              >
                {trend >= 0 ? '↑' : '↓'} {Math.abs(trend).toFixed(2)}%
              </span>
              <span className="text-[10px] text-muted-foreground">{trendLabel}</span>
            </div>
          )}
        </div>

        {icon && (
          <div
            className={`p-2.5 rounded-xl bg-gradient-to-br ${iconGradientMap[gradient]} shadow-lg`}
          >
            {React.cloneElement(icon, { className: 'w-5 h-5 text-white' })}
          </div>
        )}
      </div>
    </div>
  );
};

export default StatCard;
