package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ScheduledSeatTest {

  @Test
  void 회차_없이_만들_수_없다() {
    assertThatThrownBy(() -> new ScheduledSeat(null, 100L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 좌석_없이_만들_수_없다() {
    assertThatThrownBy(() -> new ScheduledSeat(1L, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** 같은 좌석이라도 회차가 다르면 다른 상품이다. 이 프로젝트에서 "좌석이 팔렸다"의 단위가 된다. */
  @Test
  void 좌석이_같아도_회차가_다르면_다른_것이다() {
    assertThat(new ScheduledSeat(1L, 100L)).isNotEqualTo(new ScheduledSeat(2L, 100L));
  }

  @Test
  void 회차와_좌석이_같으면_같은_것이다() {
    assertThat(new ScheduledSeat(1L, 100L)).isEqualTo(new ScheduledSeat(1L, 100L));
  }
}
