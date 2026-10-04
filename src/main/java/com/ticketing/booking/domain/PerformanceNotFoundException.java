package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 요청한 공연이 없다. 예: GET /performances/999/seats 에서 999번 공연이 없을 때
 *
 * 도메인 예외는 HTTP를 모른다. 이 예외를 어떤 상태 코드·메시지로 내보낼지는 web 계층의 BookingErrorCode가 정한다. 도메인이 Spring의
 * HttpStatus를 import하면 CLEAN_CODE.md의 DIP 신호에 걸리기 때문이다.
 */
@Getter
public class PerformanceNotFoundException extends RuntimeException {

  /** 찾지 못한 공연 ID. 응답·로그에서 어느 공연이었는지 보여줄 때 쓴다. */
  private final Long performanceId;

  public PerformanceNotFoundException(Long performanceId) {
    super("공연이 없다: " + performanceId);
    this.performanceId = performanceId;
  }
}
