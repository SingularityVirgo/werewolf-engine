package com.werewolfengine.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardTypesTest {

    @Test
    void resolveOrDefault_usesStandardWhenBlank() {
        assertThat(BoardTypes.resolveOrDefault(null)).isEqualTo(BoardTypes.STANDARD_12_PRYH_IDIOT);
        assertThat(BoardTypes.resolveOrDefault("  ")).isEqualTo(BoardTypes.STANDARD_12_PRYH_IDIOT);
    }

    @Test
    void requireSupported_rejectsUnknown() {
        assertThatThrownBy(() -> BoardTypes.requireSupported("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
