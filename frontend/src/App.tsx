import React, { useState } from 'react';
import { HomePage } from './pages/HomePage';
import { SetupPage } from './pages/SetupPage';
import { GamePage } from './components/GamePage';

type Screen = 'home' | 'setup' | 'game';

interface GameConfig {
  roomId: string;
  seatId: number;
  userId: number;
  isOwner: boolean;
}

const App: React.FC = () => {
  const [screen, setScreen] = useState<Screen>('home');
  const [gameConfig, setGameConfig] = useState<GameConfig | null>(null);

  const handleEnterGame = (roomId: string, seatId: number, userId: number, isOwner: boolean) => {
    setGameConfig({ roomId, seatId, userId, isOwner });
    setScreen('game');
  };

  const handleBackToHome = () => {
    setGameConfig(null);
    setScreen('home');
  };

  if (screen === 'game' && gameConfig) {
    return (
      <GamePage
        roomId={gameConfig.roomId}
        seatId={gameConfig.seatId}
        userId={gameConfig.userId}
        isOwner={gameConfig.isOwner}
        onBackToHome={handleBackToHome}
      />
    );
  }

  if (screen === 'setup') {
    return (
      <SetupPage
        onBack={() => setScreen('home')}
        onCreated={(roomId, seatId, userId) => handleEnterGame(roomId, seatId, userId, true)}
      />
    );
  }

  return <HomePage onStart={() => setScreen('setup')} />;
};

export default App;
