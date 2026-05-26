import React from 'react';

interface OrnatePanelProps {
  children: React.ReactNode;
  className?: string;
  tone?: 'stone' | 'parchment';
}

export const OrnatePanel: React.FC<OrnatePanelProps> = ({
  children,
  className = '',
  tone = 'stone',
}) => {
  const toneClass = tone === 'parchment' ? 'ornate-panel--parchment' : 'ornate-panel--stone';

  return (
    <div className={`ornate-panel ${toneClass} ${className}`}>
      <span className="ornate-panel__corner ornate-panel__corner--tl" aria-hidden />
      <span className="ornate-panel__corner ornate-panel__corner--tr" aria-hidden />
      <span className="ornate-panel__corner ornate-panel__corner--bl" aria-hidden />
      <span className="ornate-panel__corner ornate-panel__corner--br" aria-hidden />
      <div className="ornate-panel__inner">{children}</div>
    </div>
  );
};
