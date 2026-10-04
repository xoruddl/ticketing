package com.ticketing.booking.domain;

import jakarta.persistence.Embeddable;

/**
 * 어느 회차의 어느 좌석인가. 예: 10월 1일 19시 30분 회차의 A구역 3열 12번
 *
 * 좌석({@link Seat})은 공연에 붙어 있고 모든 회차가 같은 행을 함께 쓴다. 그래서 "팔렸다"를 말할 수 있는 단위는 좌석
 * 하나가 아니라 회차와 좌석의 쌍이다. 둘은 항상 함께 다니므로 하나의 값으로 묶는다.
 *
 * {@link Reservation} 안에 {@code @Embedded}로 저장되어, 테이블에는 schedule_id, seat_id 컬럼으로 풀려
 * 들어간다. Step 2에서 이중 선점을 막을 때 유니크 제약이 걸릴 후보도 이 두 컬럼이다.
 *
 * @param scheduleId 회차 ID. 회차 없이 좌석만으로 선점한 예약에서는 비어 있다
 * @param seatId 좌석 ID
 */
@Embeddable
public record ScheduledSeat(Long scheduleId, Long seatId) {

  /**
   * 좌석은 비어 있을 수 없다. JPA가 DB에서 읽어올 때도 이 검증을 거친다.
   *
   * 회차는 비어 있어도 된다. 회차를 빼는 동안 좌석만으로 선점한 예약이 회차 없이 저장되기 때문이다
   * (DECISIONS.md "도메인: 회차(Schedule)를 뺀다"). 전환이 끝나면 이 값 객체째 없앤다.
   */
  public ScheduledSeat {
    if (seatId == null) {
      throw new IllegalArgumentException("좌석 ID는 비어 있을 수 없다");
    }
  }
}
