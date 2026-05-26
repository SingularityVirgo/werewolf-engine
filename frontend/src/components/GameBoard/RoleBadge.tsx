import React from 'react';
import { Role, RoleNames } from '../../types/game';

interface RoleBadgeProps {
  role: Role | null;
  showName?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

const sizeClasses = {
  sm: 'text-label px-2 py-0.5',
  md: 'text-body px-2.5 py-0.5',
  lg: 'text-body px-3 py-1',
};

const roleColors: Record<Role, string> = {
  [Role.WEREWOLF]: 'bg-wolf/20 text-text-primary border-wolf/50',
  [Role.VILLAGER]: 'bg-villager/20 text-text-primary border-villager/50',
  [Role.SEER]: 'bg-seer/20 text-text-primary border-seer/50',
  [Role.WITCH]: 'bg-witch/20 text-text-primary border-witch/50',
  [Role.HUNTER]: 'bg-hunter/20 text-text-primary border-hunter/50',
  [Role.IDIOT]: 'bg-idiot/20 text-text-primary border-idiot/50',
};

export const RoleBadge: React.FC<RoleBadgeProps> = ({ role, showName = true, size = 'sm' }) => {
  if (!role) return null;

  return (
    <span
      className={`inline-flex items-center rounded-md border font-medium ${roleColors[role]} ${sizeClasses[size]}`}
    >
      {showName && <span>{RoleNames[role]}</span>}
    </span>
  );
};
