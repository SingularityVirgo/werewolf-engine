import React from 'react';
import { BrandBackdrop } from '../components/brand/BrandBackdrop';
import { CandleIcon } from '../components/brand/CandleIcon';
import { OrnatePanel } from '../components/brand/OrnatePanel';

interface HomePageProps {
  onStart: () => void;
}

export const HomePage: React.FC<HomePageProps> = ({ onStart }) => {
  return (
    <BrandBackdrop variant="home">
      <div className="min-h-screen flex flex-col lg:flex-row lg:items-stretch animate-fade-in">
        <section className="flex-1 flex flex-col justify-end lg:justify-center px-8 py-14 lg:py-20 lg:pl-16 lg:pr-8 max-w-xl">
          <div className="flex items-center gap-3 mb-8 text-ember/80">
            <CandleIcon size={28} className="text-ember candle-glow shrink-0" />
            <span className="text-label uppercase tracking-[0.2em] text-mist">烛火法庭</span>
          </div>

          <h1 className="font-display text-display font-semibold text-text-primary leading-tight">
            狼人杀
          </h1>
          <p className="mt-4 text-body text-mist max-w-sm leading-relaxed">
            十二人围坐，预女猎愚标准局。石室已备好座位，只等法官落槌。
          </p>

          <p className="mt-10 text-label text-text-muted uppercase tracking-widest hidden lg:block">
            werewolf-engine
          </p>
        </section>

        <section className="flex items-center justify-center px-6 pb-12 lg:pb-0 lg:px-12 lg:w-[min(420px,38vw)] lg:border-l lg:border-stone-border/40">
          <OrnatePanel tone="parchment" className="w-full max-w-sm">
            <p className="text-label text-text-muted uppercase tracking-wider mb-4">今夜开局</p>
            <p className="text-body text-text-secondary mb-8">
              选择板子，创建房间。你将入座 #1，与其余十一席对局。
            </p>
            <button type="button" className="btn-primary w-full text-body" onClick={onStart}>
              进入牌室
            </button>
          </OrnatePanel>
        </section>

        <p className="text-label text-text-muted uppercase tracking-widest text-center pb-6 lg:hidden">
          werewolf-engine
        </p>
      </div>
    </BrandBackdrop>
  );
};
