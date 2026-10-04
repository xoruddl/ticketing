package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 요청한 공연이 없다. 예: GET /performances/999/seats 에서 999번 공연이 없을 때
 *
 * 회차를 빼면서 {@link ScheduleNotFoundException}의 자리를 이어받는다 (DECISIONS.md "도메인: 회차(Schedule)를 뺀다").
 *
 * 도메인 예외는 HTTP를 모른다({@link ScheduleNotFoundException} 주석 참고).
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
