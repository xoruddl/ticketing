package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PaymentTest {

  private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 10, 1, 19, 3);

  @Test
  void 결제하면_금액과_시각이_남는다() {
    Payment payment = Payment.pay(1L, 7L, 150_000L, PAID_AT);

    assertThat(payment.getReservationId()).isEqualTo(1L);
    assertThat(payment.getUserId()).isEqualTo(7L);
    assertThat(payment.getAmount()).isEqualTo(150_000L);
    assertThat(payment.getPaidAt()).isEqualTo(PAID_AT);
  }

  @Test
  void 예약_없이_결제할_수_없다() {
    assertThatThrownBy(() -> Payment.pay(null, 7L, 150_000L, PAID_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 사용자_없이_결제할_수_없다() {
    assertThatThrownBy(() -> Payment.pay(1L, null, 150_000L, PAID_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** 좌석 가격이 0원일 수 있어(초대석 등) 0원 결제는 허용한다. 음수만 막는다. */
  @Test
  void 음수_금액은_결제할_수_없다() {
    assertThatThrownBy(() -> Payment.pay(1L, 7L, -1L, PAID_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 결제_시각_없이_결제할_수_없다() {
    assertThatThrownBy(() -> Payment.pay(1L, 7L, 150_000L, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
