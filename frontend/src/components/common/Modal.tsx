import React, { useEffect } from 'react';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
}

export const Modal: React.FC<ModalProps> = ({ open, onClose, title, children }) => {
  useEffect(() => {
    if (open) {
      const handler = (e: KeyboardEvent) => {
        if (e.key === 'Escape') onClose();
      };
      window.addEventListener('keydown', handler);
      return () => window.removeEventListener('keydown', handler);
    }
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-night/80" onClick={onClose} aria-hidden="true" />
      <div className="relative panel max-w-lg w-full mx-4 animate-fade-in">
        {title && (
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-title font-semibold text-gold">{title}</h2>
            <button
              type="button"
              onClick={onClose}
              className="text-text-muted hover:text-text-primary text-xl leading-none w-8 h-8 flex items-center justify-center"
              aria-label="关闭"
            >
              ×
            </button>
          </div>
        )}
        {children}
      </div>
    </div>
  );
};
