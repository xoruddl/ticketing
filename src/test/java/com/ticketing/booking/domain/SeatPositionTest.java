package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeatPositionTest {

  @Test
  void 위치를_구역_열_번호로_표시한다() {
    SeatPosition position = new SeatPosition("A", "3", 12);

    assertThat(position.label()).isEqualTo("A구역 3열 12번");
  }

  @Test
  void 좌석_번호는_1_이상이어야_한다() {
    assertThatThrownBy(() -> new SeatPosition("A", "3", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 구역이나_열이_비어_있으면_만들_수_없다() {
    assertThatThrownBy(() -> new SeatPosition(" ", "3", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SeatPosition("A", "", 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
