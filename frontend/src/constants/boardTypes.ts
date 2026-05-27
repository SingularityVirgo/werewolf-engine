/** MVP board type sent to POST /api/room — must match backend BoardTypes.STANDARD_12_PRYH_IDIOT */
export const BOARD_TYPE_STANDARD = 'STANDARD_12_PRYH_IDIOT';

export interface BoardPreset {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
}

export const BOARD_PRESETS: BoardPreset[] = [
  {
    id: BOARD_TYPE_STANDARD,
    name: '预女猎愚 · 12 人标准场',
    description: '4 狼 · 4 民 · 预言家 · 女巫 · 猎人 · 愚者',
    enabled: true,
  },
  {
    id: 'GUARD_12',
    name: '守卫局 · 12 人',
    description: '即将开放',
    enabled: false,
  },
];
