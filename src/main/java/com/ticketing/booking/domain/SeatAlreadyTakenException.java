package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 이미 팔린 좌석을 선점하려 했다. 예: 다른 사용자가 선점했거나 확정한 좌석에 POST /reservations 를 보냈을 때
 *
 * 요청이 잘못된 것이 아니라 좌석의 상태가 바뀐 것이다. 같은 요청을 조금 전에 보냈다면 성공했을 수 있고, 만료나
 * 취소로 좌석이 풀리면 다시 성공할 수도 있다. 그래서 응답도 "잘못된 요청"이 아니라 "지금은 안 된다"로 내보낸다
 * ({@link com.ticketing.booking.web.BookingErrorCode}).
 *
 * 도메인 예외는 HTTP를 모른다({@link ScheduleNotFoundException} 주석 참고).
 */
@Getter
public class SeatAlreadyTakenException extends RuntimeException {

  /** 이미 팔린 회차·좌석. 응답·로그에서 어느 좌석이었는지 보여줄 때 쓴다. */
  private final ScheduledSeat seat;

  public SeatAlreadyTakenException(ScheduledSeat seat) {
    super("이미 팔린 좌석이다: 회차 " + seat.scheduleId() + ", 좌석 " + seat.seatId());
    this.seat = seat;
  }
}
