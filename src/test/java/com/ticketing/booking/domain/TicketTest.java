package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TicketTest {

  private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 10, 1, 19, 3);

  @Test
  void 발권하면_입장_코드가_붙는다() {
    Ticket ticket = Ticket.issue(1L, ISSUED_AT);

    assertThat(ticket.getReservationId()).isEqualTo(1L);
    assertThat(ticket.getIssuedAt()).isEqualTo(ISSUED_AT);
    // UUID 문자열이라 36자다. 컬럼 길이(V3의 varchar(36))와 맞아야 저장된다.
    assertThat(ticket.getCode()).hasSize(36);
  }

  /** 코드는 입장에서 티켓을 구분하는 값이라 장마다 달라야 한다. */
  @Test
  void 티켓마다_입장_코드가_다르다() {
    Ticket first = Ticket.issue(1L, ISSUED_AT);
    Ticket second = Ticket.issue(2L, ISSUED_AT);

    assertThat(first.getCode()).isNotEqualTo(second.getCode());
  }

  @Test
  void 예약_없이_발권할_수_없다() {
    assertThatThrownBy(() -> Ticket.issue(null, ISSUED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 발권_시각_없이_발권할_수_없다() {
    assertThatThrownBy(() -> Ticket.issue(1L, null)).isInstanceOf(IllegalArgumentException.class);
  }
}
