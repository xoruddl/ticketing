package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 요청한 회차에 그 좌석이 없다. 예: POST /reservations 에 다른 공연의 좌석 ID를 보냈을 때
 *
 * 두 경우를 하나로 묶는다 - 좌석 자체가 없는 경우와, 좌석은 있지만 그 회차의 공연 좌석이 아닌 경우다. 요청한
 * 사람에게는 "그 회차에서 고를 수 없는 좌석"으로 같고, 남의 공연에 몇 번 좌석이 있는지 알려줄 이유도 없다.
 *
 * 도메인 예외는 HTTP를 모른다({@link ScheduleNotFoundException} 주석 참고).
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
