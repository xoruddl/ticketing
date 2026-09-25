package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 요청한 예약이 없다. 예: POST /reservations/999/payment 에서 999번 예약이 없을 때
 *
 * 도메인 예외는 HTTP를 모른다({@link ScheduleNotFoundException} 주석 참고).
 */
@Getter
public class ReservationNotFoundException extends RuntimeException {

  /** 찾지 못한 예약 ID. 응답·로그에서 어느 예약이었는지 보여줄 때 쓴다. */
  private final Long reservationId;

  public ReservationNotFoundException(Long reservationId) {
    super("예약이 없다: " + reservationId);
    this.reservationId = reservationId;
  }
}
