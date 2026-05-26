import React from 'react';

type BrandBackdropVariant = 'home' | 'setup' | 'game';

interface BrandBackdropProps {
  variant?: BrandBackdropVariant;
  children: React.ReactNode;
}

export const BrandBackdrop: React.FC<BrandBackdropProps> = ({ variant = 'home', children }) => {
  return (
    <div className={`brand-backdrop brand-backdrop--${variant} min-h-screen relative overflow-hidden`}>
      <img
        src="/assets/bg-chamber.svg"
        alt=""
        aria-hidden
        className="brand-backdrop__svg absolute inset-0 w-full h-full object-cover pointer-events-none"
      />

      <div className="brand-backdrop__grain absolute inset-0 pointer-events-none" aria-hidden />
      <div className="brand-backdrop__vignette absolute inset-0 pointer-events-none" aria-hidden />

      <div className="relative z-10">{children}</div>
    </div>
  );
};
