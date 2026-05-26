/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        abyss: '#0d1210',
        stone: '#1a1814',
        'stone-surface': '#242019',
        'stone-border': '#3d3629',
        parchment: '#2a251c',
        'parchment-light': '#3a3228',
        ember: '#c9a227',
        'ember-muted': '#8a7020',
        mist: '#8a9a8e',
        night: '#0d1210',
        'night-elevated': '#1a1814',
        'night-surface': '#242019',
        'night-border': '#3d3629',
        gold: '#c9a227',
        'gold-muted': '#8a7020',
        wolf: '#6b1515',
        villager: '#2d4a28',
        seer: '#1a3348',
        witch: '#3a1a48',
        hunter: '#4a3018',
        idiot: '#4a4818',
        blood: '#a33030',
        'blood-muted': '#6b2020',
        'text-primary': '#e8e4dc',
        'text-secondary': '#a8a090',
        'text-muted': '#6a6458',
      },
      fontFamily: {
        sans: ['"Noto Sans SC"', 'system-ui', 'sans-serif'],
        display: ['"Noto Serif SC"', 'Cinzel', 'Georgia', 'serif'],
        mono: ['ui-monospace', 'monospace'],
      },
      fontSize: {
        display: ['clamp(2rem, 5vw, 3rem)', { lineHeight: '1.15', letterSpacing: '0.04em' }],
        title: ['1.125rem', { lineHeight: '1.35', letterSpacing: '0.01em' }],
        body: ['0.9375rem', { lineHeight: '1.55' }],
        label: ['0.75rem', { lineHeight: '1.4', letterSpacing: '0.08em' }],
      },
      borderRadius: {
        sm: '4px',
        md: '8px',
        lg: '12px',
      },
      animation: {
        'fade-in': 'fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1)',
        'speaking': 'speakingPulse 2s cubic-bezier(0.16, 1, 0.3, 1) infinite',
        'candle': 'candleFlicker 3s ease-in-out infinite',
      },
      keyframes: {
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        speakingPulse: {
          '0%, 100%': { boxShadow: '0 0 0 0 rgba(201, 162, 39, 0.35)' },
          '50%': { boxShadow: '0 0 0 4px rgba(201, 162, 39, 0.15)' },
        },
        candleFlicker: {
          '0%, 100%': { opacity: '0.85' },
          '50%': { opacity: '1' },
        },
      },
    },
  },
  plugins: [],
}
