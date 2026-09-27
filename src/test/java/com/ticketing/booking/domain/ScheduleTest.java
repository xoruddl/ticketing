package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ScheduleTest {

  @Test
  void 공연_없이_만들_수_없다() {
    assertThatThrownBy(() -> new Schedule(null, LocalDateTime.of(2026, 10, 1, 19, 30)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 시작_시각_없이_만들_수_없다() {
    assertThatThrownBy(() -> new Schedule(1L, null)).isInstanceOf(IllegalArgumentException.class);
  }
}
