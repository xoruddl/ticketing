package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 요청한 회차가 없다. 예: GET /schedules/999/seats 에서 999번 회차가 없을 때
 *
 * 도메인 예외는 HTTP를 모른다. 이 예외를 어떤 상태 코드·메시지로 내보낼지는 web 계층의 BookingErrorCode가 정한다. 도메인이 Spring의
 * HttpStatus를 import하면 CLEAN_CODE.md의 DIP 신호에 걸리기 때문이다.
 */
@Getter
public class ScheduleNotFoundException extends RuntimeException {

  /** 찾지 못한 회차 ID. 응답·로그에서 어느 회차였는지 보여줄 때 쓴다. */
  private final Long scheduleId;

  public ScheduleNotFoundException(Long scheduleId) {
    super("회차가 없다: " + scheduleId);
    this.scheduleId = scheduleId;
  }
}
