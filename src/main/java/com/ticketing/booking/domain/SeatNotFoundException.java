package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 요청한 좌석이 없다. 예: POST /reservations 에 없는 좌석 ID를 보냈을 때
 *
 * 좌석이 공연에 속하므로 "이 공연의 좌석인가"는 따로 따지지 않는다. 좌석 ID 하나가 공연 하나를 가리킨다.
 *
 * 도메인 예외는 HTTP를 모른다({@link PerformanceNotFoundException} 주석 참고).
 */
@Getter
public class SeatNotFoundException extends RuntimeException {

  /** 찾지 못한 좌석 ID. 응답·로그에서 어느 좌석이었는지 보여줄 때 쓴다. */
  private final Long seatId;

  public SeatNotFoundException(Long seatId) {
    super("좌석이 없다: " + seatId);
    this.seatId = seatId;
  }
}
