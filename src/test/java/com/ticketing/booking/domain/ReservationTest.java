package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReservationTest {

  private static final ScheduledSeat SEAT = new ScheduledSeat(1L, 100L);
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 10, 1, 19, 5);

  @Test
  void 회차_좌석_없이_선점할_수_없다() {
    assertThatThrownBy(() -> Reservation.hold(null, 7L, EXPIRES_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 사용자_없이_선점할_수_없다() {
    assertThatThrownBy(() -> Reservation.hold(SEAT, null, EXPIRES_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 만료_시각_없이_선점할_수_없다() {
    assertThatThrownBy(() -> Reservation.hold(SEAT, 7L, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** 예약은 언제나 선점으로 태어난다. 확정된 상태로 만들어지는 길은 없다. */
  @Test
  void 선점한_예약은_좌석을_차지한다() {
    Reservation reservation = Reservation.hold(SEAT, 7L, EXPIRES_AT);

    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
    assertThat(reservation.getStatus().occupiesSeat()).isTrue();
  }

  @Test
  void 만료_시각_전에는_만료되지_않았다() {
    Reservation reservation = Reservation.hold(SEAT, 7L, EXPIRES_AT);

    assertThat(reservation.isExpired(EXPIRES_AT.minusNanos(1_000))).isFalse();
  }

  /** 경계는 만료 쪽으로 붙인다. "19시 5분까지 유효"가 아니라 "19시 5분이 되면 끝"이다. */
  @Test
  void 만료_시각과_같은_순간부터_만료다() {
    Reservation reservation = Reservation.hold(SEAT, 7L, EXPIRES_AT);

    assertThat(reservation.isExpired(EXPIRES_AT)).isTrue();
  }

  @Test
  void 만료_시각이_지나면_만료다() {
    Reservation reservation = Reservation.hold(SEAT, 7L, EXPIRES_AT);

    assertThat(reservation.isExpired(EXPIRES_AT.plusMinutes(1))).isTrue();
  }
}
