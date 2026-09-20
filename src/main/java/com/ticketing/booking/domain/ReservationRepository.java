package com.ticketing.booking.domain;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

/** 예약 저장소. 좌석이 팔렸는지는 좌석이 아니라 이 표의 예약으로 판단한다({@link Reservation} 주석 참고). */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  /**
   * 이 회차·좌석을 차지하고 있는 예약이 있는가. 선점 요청을 거절할지 판단할 때 쓴다.
   *
   * 어떤 상태가 좌석을 차지하는지는 저장소가 정하지 않는다. 부르는 쪽이 상태 목록을 준다
   * ({@link ReservationStatus#occupiesSeat()}).
   *
   * seat 조건은 schedule_id, seat_id 두 컬럼 비교로 풀리고, idx_reservation_schedule_seat 인덱스를 탄다.
   *
   * 이 확인과 저장 사이에 다른 요청이 끼어들 수 있다. Step 0은 막지 않는다 — Step 1에서 재현할 대상이다.
   */
  boolean existsBySeatAndStatusIn(ScheduledSeat seat, Collection<ReservationStatus> statuses);
}
