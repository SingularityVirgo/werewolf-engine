package com.werewolfengine.room;

/**
 * MVP board presets. Rules engine still uses a single fixed layout; {@code boardType} is reserved for product/API.
 */
public final class BoardTypes {

    public static final String STANDARD_12_PRYH_IDIOT = "STANDARD_12_PRYH_IDIOT";

    private BoardTypes() {
    }

    public static String resolveOrDefault(String boardType) {
        if (boardType == null || boardType.isBlank()) {
            return STANDARD_12_PRYH_IDIOT;
        }
        return boardType.trim();
    }

    public static void requireSupported(String boardType) {
        if (!STANDARD_12_PRYH_IDIOT.equals(boardType)) {
            throw new IllegalArgumentException("Unsupported boardType: " + boardType);
        }
    }
}
