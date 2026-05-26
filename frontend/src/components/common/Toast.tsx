import React, { useEffect, useState } from 'react';

interface ToastProps {
  message: string;
  type?: 'info' | 'error' | 'success';
  duration?: number;
  onDone: () => void;
}

export const Toast: React.FC<ToastProps> = ({ message, type = 'info', duration = 3000, onDone }) => {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setVisible(false);
      setTimeout(onDone, 300);
    }, duration);
    return () => clearTimeout(timer);
  }, [duration, onDone]);

  const styles = {
    info: 'border-night-border bg-night-elevated text-text-primary',
    error: 'border-blood/50 bg-night-elevated text-blood',
    success: 'border-gold/30 bg-night-elevated text-gold',
  };

  return (
    <div
      className={`fixed top-4 right-4 z-50 px-4 py-3 rounded-md border text-body transition-opacity duration-300 ${
        styles[type]
      } ${visible ? 'opacity-100' : 'opacity-0'}`}
      role="status"
    >
      {message}
    </div>
  );
};
