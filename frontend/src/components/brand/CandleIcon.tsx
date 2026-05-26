import React from 'react';

interface CandleIconProps {
  className?: string;
  size?: number;
}

export const CandleIcon: React.FC<CandleIconProps> = ({ className = '', size = 32 }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden
  >
    <rect x="14" y="14" width="4" height="14" rx="1" fill="currentColor" fillOpacity="0.35" />
    <path
      d="M16 6c2 4 3 6 3 8a3 3 0 1 1-6 0c0-2 1-4 3-8Z"
      fill="currentColor"
      className="candle-flame"
    />
  </svg>
);
