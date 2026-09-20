package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ReservationStatusTest {

  @Test
  void 선점과_확정은_좌석을_차지한다() {
    assertThat(ReservationStatus.HELD.occupiesSeat()).isTrue();
    assertThat(ReservationStatus.CONFIRMED.occupiesSeat()).isTrue();
  }

  @Test
  void 만료와_취소는_좌석을_차지하지_않는다() {
    assertThat(ReservationStatus.EXPIRED.occupiesSeat()).isFalse();
    assertThat(ReservationStatus.CANCELED.occupiesSeat()).isFalse();
  }

  /** 상태를 하나씩 묻는 것과 목록으로 받는 것의 답이 어긋나면 안 된다. */
  @Test
  void 좌석을_차지하는_상태_목록은_선점과_확정이다() {
    assertThat(ReservationStatus.occupying())
        .containsExactlyInAnyOrder(ReservationStatus.HELD, ReservationStatus.CONFIRMED);
  }

  @Test
  void 좌석을_차지하는_상태_목록은_바꿀_수_없다() {
    assertThatThrownBy(() -> ReservationStatus.occupying().add(ReservationStatus.EXPIRED))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
