import React from 'react';

/**
 * Skeleton loaders for various UI elements.
 * Uses CSS shimmer animation defined in index.css.
 */

export const SkeletonCard = ({ className = '' }) => (
  <div className={`glass-card rounded-2xl p-5 space-y-3 ${className}`}>
    <div className="skeleton h-3 w-20" />
    <div className="skeleton h-7 w-32" />
    <div className="skeleton h-3 w-24" />
  </div>
);

export const SkeletonChart = ({ className = '' }) => (
  <div className={`glass-card rounded-2xl p-5 ${className}`}>
    <div className="skeleton h-4 w-32 mb-4" />
    <div className="skeleton h-[250px] w-full rounded-xl" />
  </div>
);

export const SkeletonTableRow = () => (
  <div className="flex items-center gap-4 px-4 py-3">
    <div className="skeleton h-8 w-8 rounded-full" />
    <div className="flex-1 space-y-2">
      <div className="skeleton h-3 w-28" />
      <div className="skeleton h-2 w-16" />
    </div>
    <div className="skeleton h-4 w-20" />
    <div className="skeleton h-4 w-16" />
  </div>
);

export const SkeletonTable = ({ rows = 5, className = '' }) => (
  <div className={`glass-card rounded-2xl overflow-hidden ${className}`}>
    <div className="px-5 py-3 border-b border-border/30">
      <div className="skeleton h-4 w-32" />
    </div>
    {Array.from({ length: rows }).map((_, i) => (
      <SkeletonTableRow key={i} />
    ))}
  </div>
);

export const SkeletonPage = () => (
  <div className="space-y-6">
    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
      {Array.from({ length: 4 }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
    <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
      <SkeletonChart className="xl:col-span-2" />
      <SkeletonChart />
    </div>
    <SkeletonTable rows={6} />
  </div>
);
