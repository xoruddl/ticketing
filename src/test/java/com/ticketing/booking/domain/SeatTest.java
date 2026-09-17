package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeatTest {

  private static final SeatPosition POSITION = new SeatPosition("A", "3", 12);

  @Test
  void 공연_없이_만들_수_없다() {
    assertThatThrownBy(() -> new Seat(null, POSITION, SeatGrade.VIP, 150_000))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 위치_없이_만들_수_없다() {
    assertThatThrownBy(() -> new Seat(1L, null, SeatGrade.VIP, 150_000))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 등급_없이_만들_수_없다() {
    assertThatThrownBy(() -> new Seat(1L, POSITION, null, 150_000))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 가격은_음수일_수_없다() {
    assertThatThrownBy(() -> new Seat(1L, POSITION, SeatGrade.VIP, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 가격이_0원인_좌석은_만들_수_있다() {
    assertThatCode(() -> new Seat(1L, POSITION, SeatGrade.S, 0)).doesNotThrowAnyException();
  }
}
