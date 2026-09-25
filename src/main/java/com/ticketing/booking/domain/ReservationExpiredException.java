package com.ticketing.booking.domain;

import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 선점 유효 시간이 지난 예약에 결제하려 했다. 예: 19시 5분에 만료되는 선점에 19시 6분에 결제를 보냈을 때
 *
 * 예약의 상태는 아직 HELD일 수 있다. Step 0은 만료된 선점을 EXPIRED로 바꿔주는 처리가 없어서, 만료는 상태가 아니라
 * 만료 시각과 지금을 비교해 판단한다({@link Reservation#isExpired}). 누가 언제 상태를 바꿀지는 Step 3에서 정한다.
 *
 * 도메인 예외는 HTTP를 모른다({@link ScheduleNotFoundException} 주석 참고).
 */
@Getter
public class ReservationExpiredException extends RuntimeException {

  /** 만료된 예약 ID. */
  private final Long reservationId;

  /** 선점이 끝난 시각. 사용자에게 "언제 끝났는지" 보여줄 때 쓴다. */
  private final LocalDateTime expiresAt;

  public ReservationExpiredException(Long reservationId, LocalDateTime expiresAt) {
    super("선점 유효 시간이 지났다: 예약 " + reservationId + ", 만료 " + expiresAt);
    this.reservationId = reservationId;
    this.expiresAt = expiresAt;
  }
}
