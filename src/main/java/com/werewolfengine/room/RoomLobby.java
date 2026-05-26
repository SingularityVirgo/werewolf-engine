package com.werewolfengine.room;

/**
 * Lobby metadata for Formal path B (PRD §4.2). Game rules stay in {@code game}; this is room lifecycle only.
 */
public final class RoomLobby {

    public static final int MAX_PLAYERS = 12;

    private final Long hostUserId;
    private final int aiCount;
    private final String boardType;

    public RoomLobby(Long hostUserId, int aiCount, String boardType) {
        if (aiCount < 0 || aiCount > MAX_PLAYERS) {
            throw new IllegalArgumentException("aiCount must be 0..12");
        }
        this.hostUserId = hostUserId;
        this.aiCount = aiCount;
        this.boardType = BoardTypes.resolveOrDefault(boardType);
        BoardTypes.requireSupported(this.boardType);
    }

    public RoomLobby(Long hostUserId, int aiCount) {
        this(hostUserId, aiCount, BoardTypes.STANDARD_12_PRYH_IDIOT);
    }

    public Long hostUserId() {
        return hostUserId;
    }

    public int aiCount() {
        return aiCount;
    }

    public String boardType() {
        return boardType;
    }

    /** Seats {@code (12 - aiCount + 1) .. 12} are reserved for AI when {@code aiCount > 0}. */
    public boolean isAiReservedSeat(int seatId) {
        if (aiCount <= 0) {
            return false;
        }
        return seatId > MAX_PLAYERS - aiCount;
    }
}
