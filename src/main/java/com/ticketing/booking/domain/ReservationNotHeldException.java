package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 선점 상태가 아닌 예약에 결제하려 했다. 예: 이미 결제해 CONFIRMED가 된 예약에 결제를 한 번 더 보냈을 때
 *
 * 결제는 HELD → CONFIRMED 전이 하나뿐이다. 이미 확정되었거나 취소·만료된 예약은 결제할 대상이 아니다.
 *
 * 같은 결제 요청이 다시 온 경우(재시도)도 지금은 이 예외로 거절한다. 두 번째 요청에 첫 결제 결과를 돌려주는
 * 멱등 처리는 Step 7–8에서 다룬다.
 *
 * 도메인 예외는 HTTP를 모른다({@link ScheduleNotFoundException} 주석 참고).
 */
@Getter
public class ReservationNotHeldException extends RuntimeException {

  /** 요청한 예약 ID. */
  private final Long reservationId;

  /** 거절 당시의 상태. 예: CONFIRMED. 왜 결제할 수 없는지 응답·로그에서 보여줄 때 쓴다. */
  private final ReservationStatus status;

  public ReservationNotHeldException(Long reservationId, ReservationStatus status) {
    super("선점 상태가 아닌 예약이다: 예약 " + reservationId + ", 상태 " + status);
    this.reservationId = reservationId;
    this.status = status;
  }
}
