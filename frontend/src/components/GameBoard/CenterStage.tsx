import React, { useState, useRef, useEffect } from 'react';

import {

  ChatMessage,

  GameLogEntry,

  GamePhase,

  PhaseNames,

  Role,

} from '../../types/game';



type CenterTab = 'public' | 'wolf' | 'judge';



interface CenterStageProps {

  phase: GamePhase;

  roomId: string;

  round: number;

  messages: ChatMessage[];

  gameLog: GameLogEntry[];

  myRole: Role | null;

  mySeatId: number | null;

  currentSpeakerId: number | null;

  wolfChatInPhase: boolean;

  onSendMessage: (content: string, isWolfChat: boolean) => void;

}



function isPublicChatPhase(phase: GamePhase): boolean {

  return phase === GamePhase.DAY_DISCUSS || phase === GamePhase.DAY_VOTE || phase === GamePhase.VOTE_RESULT;

}



function isNightAnnouncerPhase(phase: GamePhase): boolean {

  return (

    phase === GamePhase.WAITING ||

    phase.toString().startsWith('NIGHT') ||

    phase === GamePhase.NIGHT_DEATH_ANNOUNCE ||

    phase === GamePhase.EXILE_DEATH_ANNOUNCE ||

    phase === GamePhase.LAST_WORDS ||

    phase === GamePhase.CHECK_WIN ||

    phase === GamePhase.GAME_OVER

  );

}



function renderChatMessage(msg: ChatMessage, mySeatId: number | null) {

  return (

    <div key={`${msg.timestamp}-${msg.seatId}`} className={`text-body ${msg.seatId === mySeatId ? 'text-ember' : 'text-text-secondary'}`}>

      <span className="font-mono font-medium tabular-nums">

        {msg.seatId === mySeatId ? '我' : `#${msg.seatId}`}

      </span>

      <span className="text-text-muted mx-1">·</span>

      <span>{msg.content}</span>

    </div>

  );

}



function ChatMessageList({

  messages,

  mySeatId,

  isWolfChat,

  bottomRef,

}: {

  messages: ChatMessage[];

  mySeatId: number | null;

  isWolfChat: boolean;

  bottomRef: React.RefObject<HTMLDivElement>;

}) {

  const filtered = isWolfChat

    ? messages.filter((m) => m.isWolfChat)

    : messages.filter((m) => !m.isWolfChat);



  return (

    <div className={`flex-1 overflow-y-auto space-y-2 mb-3 min-h-[160px] ${isWolfChat ? 'bg-wolf/5 rounded-md p-2 -mx-1' : ''}`}>

      {filtered.length === 0 && (

        <p className="text-label text-text-muted text-center py-8">暂无消息</p>

      )}

      {filtered.map((msg) => renderChatMessage(msg, mySeatId))}

      <div ref={bottomRef} />

    </div>

  );

}



function AnnouncerFeed({

  gameLog,

  round,

  bottomRef,

}: {

  gameLog: GameLogEntry[];

  round: number;

  bottomRef: React.RefObject<HTMLDivElement>;

}) {

  const announceEntries = gameLog.filter(

    (e) => e.type === 'event' || e.type === 'death' || e.type === 'system' || e.type === 'vote'

  );



  return (

    <div className="flex-1 overflow-y-auto space-y-2 min-h-[160px]">

      {announceEntries.length === 0 && (

        <p className="text-body text-text-muted text-center py-8">等待法官宣读…</p>

      )}

      {announceEntries.slice(-20).map((entry) => (

        <div key={entry.id} className="flex gap-2 text-body">

          {entry.round > 0 && (

            <span className="font-mono text-label text-text-muted tabular-nums shrink-0">R{entry.round}</span>

          )}

          <span className={entry.type === 'death' ? 'text-blood' : 'text-text-secondary'}>

            {entry.message}

          </span>

        </div>

      ))}

      <div ref={bottomRef} />

    </div>

  );

}



export const CenterStage: React.FC<CenterStageProps> = ({

  phase,

  roomId,

  round,

  messages,

  gameLog,

  myRole,

  mySeatId,

  currentSpeakerId,

  wolfChatInPhase,

  onSendMessage,

}) => {

  const [input, setInput] = useState('');

  const [activeTab, setActiveTab] = useState<CenterTab>('public');

  const bottomRef = useRef<HTMLDivElement>(null);



  const isWolf = myRole === Role.WEREWOLF;

  const isNightWolfPhase = phase === GamePhase.NIGHT_WOLF;

  const showDayWolfTab = isWolf && wolfChatInPhase && isPublicChatPhase(phase);

  const showNightWolfTab = isWolf && isNightWolfPhase;

  const isWolfChat = activeTab === 'wolf';



  const canSpeakPublic =

    (phase === GamePhase.DAY_DISCUSS || phase === GamePhase.LAST_WORDS) &&

    currentSpeakerId != null &&

    currentSpeakerId === mySeatId &&

    !isWolfChat;



  const canChatWolfDay = showDayWolfTab && isWolfChat;

  const canChatWolfNight = showNightWolfTab && isWolfChat;



  useEffect(() => {

    if (!showDayWolfTab && !showNightWolfTab && activeTab === 'wolf') {

      setActiveTab('public');

    }

  }, [showDayWolfTab, showNightWolfTab, activeTab]);



  useEffect(() => {

    if (showNightWolfTab) {

      setActiveTab('wolf');

    }

  }, [showNightWolfTab, phase]);



  useEffect(() => {

    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });

  }, [messages, gameLog, activeTab, phase]);



  const handleSend = () => {

    if (!input.trim()) return;

    onSendMessage(input.trim(), isWolfChat);

    setInput('');

  };



  const handleKeyDown = (e: React.KeyboardEvent) => {

    if (e.key === 'Enter' && !e.shiftKey) {

      e.preventDefault();

      handleSend();

    }

  };



  if (phase === GamePhase.WAITING) {

    return (

      <div className="game-table-surface border border-stone-border rounded-sm flex flex-col items-center justify-center min-h-[280px] text-center space-y-3 p-6">

        <p className="text-label text-text-muted uppercase">等待开局</p>

        <p className="font-mono text-body text-ember">{roomId}</p>

        <p className="text-body text-text-secondary">房主准备后点击开始</p>

      </div>

    );

  }



  if (isPublicChatPhase(phase)) {

    return (

      <div className="game-table-surface border border-stone-border rounded-sm flex flex-col min-h-[280px] h-full p-4">

        <div className="flex border-b border-stone-border mb-3">

          <button

            type="button"

            className={`flex-1 pb-2 text-label font-medium uppercase ${activeTab === 'public' ? 'tab-active' : 'tab-inactive'}`}

            onClick={() => setActiveTab('public')}

          >

            公频

          </button>

          {showDayWolfTab && (

            <button

              type="button"

              className={`flex-1 pb-2 text-label font-medium uppercase ${activeTab === 'wolf' ? 'tab-active text-wolf border-wolf' : 'tab-inactive'}`}

              onClick={() => setActiveTab('wolf')}

            >

              狼频道

            </button>

          )}

        </div>



        {phase === GamePhase.DAY_VOTE && (

          <p className="text-label text-ember mb-2 uppercase">投票阶段 · 请在底栏选择目标</p>

        )}



        {phase === GamePhase.DAY_DISCUSS && currentSpeakerId != null && !isWolfChat && (

          <p className="text-label text-text-secondary mb-2">

            当前发言：<span className="font-mono text-ember">#{currentSpeakerId}</span>

          </p>

        )}



        <ChatMessageList messages={messages} mySeatId={mySeatId} isWolfChat={isWolfChat} bottomRef={bottomRef} />



        {(canSpeakPublic || canChatWolfDay) && (

          <div className="flex gap-2">

            <input

              className="input-field flex-1 text-body"

              placeholder={isWolfChat ? '狼频道发言…' : '公频发言…'}

              value={input}

              onChange={(e) => setInput(e.target.value)}

              onKeyDown={handleKeyDown}

            />

            <button type="button" className="btn-primary text-body px-4" onClick={handleSend}>

              发送

            </button>

          </div>

        )}



        {phase === GamePhase.DAY_DISCUSS && !canSpeakPublic && !isWolfChat && (

          <p className="text-label text-text-muted text-center py-2">等待 #{currentSpeakerId ?? '—'} 发言</p>

        )}

      </div>

    );

  }



  if (isNightAnnouncerPhase(phase)) {

    const phaseLabel = PhaseNames[phase] || phase;



    if (showNightWolfTab) {

      return (

        <div className="game-table-surface border border-stone-border rounded-sm flex flex-col min-h-[280px] p-4">

          <header className="border-b border-stone-border pb-3 mb-3">

            <p className="text-label text-text-muted uppercase">狼人行动</p>

            <h2 className="text-title font-semibold text-text-primary mt-1">{phaseLabel}</h2>

            {round > 0 && (

              <p className="text-label text-text-muted font-mono tabular-nums mt-1">第 {round} 轮</p>

            )}

          </header>



          <div className="flex border-b border-stone-border mb-3">

            <button

              type="button"

              className={`flex-1 pb-2 text-label font-medium uppercase ${activeTab === 'judge' ? 'tab-active' : 'tab-inactive'}`}

              onClick={() => setActiveTab('judge')}

            >

              法官区

            </button>

            <button

              type="button"

              className={`flex-1 pb-2 text-label font-medium uppercase ${activeTab === 'wolf' ? 'tab-active text-wolf border-wolf' : 'tab-inactive'}`}

              onClick={() => setActiveTab('wolf')}

            >

              狼频道

            </button>

          </div>



          {activeTab === 'judge' ? (

            <AnnouncerFeed gameLog={gameLog} round={round} bottomRef={bottomRef} />

          ) : (

            <>

              <ChatMessageList messages={messages} mySeatId={mySeatId} isWolfChat bottomRef={bottomRef} />

              {canChatWolfNight && (

                <div className="flex gap-2 mt-3 pt-3 border-t border-stone-border">

                  <input

                    className="input-field flex-1 text-body"

                    placeholder="狼频道发言…"

                    value={input}

                    onChange={(e) => setInput(e.target.value)}

                    onKeyDown={handleKeyDown}

                  />

                  <button type="button" className="btn-primary text-body px-4" onClick={handleSend}>

                    发送

                  </button>

                </div>

              )}

            </>

          )}



          {activeTab === 'wolf' && !canChatWolfNight && (

            <p className="text-label text-text-muted text-center py-2">可在底栏或此处发送狼频道消息</p>

          )}

        </div>

      );

    }



    return (

      <div className="game-table-surface border border-stone-border rounded-sm flex flex-col min-h-[280px] p-4">

        <header className="border-b border-stone-border pb-3 mb-3">

          <p className="text-label text-text-muted uppercase">法官区</p>

          <h2 className="text-title font-semibold text-text-primary mt-1">{phaseLabel}</h2>

          {round > 0 && (

            <p className="text-label text-text-muted font-mono tabular-nums mt-1">第 {round} 轮</p>

          )}

          {phase === GamePhase.LAST_WORDS && currentSpeakerId != null && (

            <p className="text-body text-ember mt-2">#{currentSpeakerId} 遗言中</p>

          )}

        </header>



        <AnnouncerFeed gameLog={gameLog} round={round} bottomRef={bottomRef} />



        {phase === GamePhase.LAST_WORDS && canSpeakPublic && (

          <div className="flex gap-2 mt-3 pt-3 border-t border-stone-border">

            <input

              className="input-field flex-1 text-body"

              placeholder="遗言…"

              value={input}

              onChange={(e) => setInput(e.target.value)}

              onKeyDown={handleKeyDown}

            />

            <button type="button" className="btn-primary text-body px-4" onClick={handleSend}>

              发送

            </button>

          </div>

        )}

      </div>

    );

  }



  return (

    <div className="game-table-surface border border-stone-border rounded-sm flex items-center justify-center min-h-[280px] p-4">

      <p className="text-body text-text-secondary">{PhaseNames[phase] || phase}</p>

    </div>

  );

};

