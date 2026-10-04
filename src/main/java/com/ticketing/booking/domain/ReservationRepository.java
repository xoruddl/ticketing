package com.ticketing.booking.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 예약 저장소. 좌석이 팔렸는지는 좌석이 아니라 이 표의 예약으로 판단한다({@link Reservation} 주석 참고). */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  /**
   * 이 좌석을 차지하고 있는 예약이 있는가. 선점 요청을 거절할지 판단할 때 쓴다.
   *
   * 어떤 상태가 좌석을 차지하는지는 저장소가 정하지 않는다. 부르는 쪽이 상태 목록을 준다
   * ({@link ReservationStatus#occupying()}).
   *
   * seat_id 조건은 V5의 idx_reservation_seat 인덱스를 탄다.
   *
   * 이 확인과 저장 사이에 다른 요청이 끼어들 수 있다. 아직 막지 않는다 — Step 1에서 재현했고 Step 2에서 막는다.
   */
  boolean existsBySeatIdAndStatusIn(Long seatId, Collection<ReservationStatus> statuses);

  /**
   * 주어진 좌석 중 이미 차지된 좌석의 ID. 공연의 좌석 목록에 예매 가능 여부를 채울 때 쓴다.
   *
   * 좌석을 하나씩 {@link #existsBySeatIdAndStatusIn}으로 묻지 않는 이유는, 좌석이 N개면 조회도 N번이 되기
   * 때문이다. 한 번에 가져와 메모리에서 대조한다.
   *
   * 예약에는 공연 ID가 없어서, 부르는 쪽이 공연의 좌석 ID 목록을 넘긴다. 좌석 표와 JOIN하지 않으므로 이 쿼리는 예약
   * 표만 읽는다. 예약 행 전체가 아니라 seat_id만 읽는다. 여기서 필요한 것은 "어느 좌석이 팔렸는가"뿐이다.
   *
   * 좌석 목록이 비어 있으면 빈 결과를 돌려준다.
   *
   * 돌려주는 것이 Set인 이유: 지금은 같은 좌석에 예약이 둘 생길 수 있다. 막는 장치가 없어서다(Step 1에서
   * 재현했다). 중복은 여기서 접고, 부르는 쪽은 좌석 ID가 있는지만 본다.
   */
  @Query(
      """
      select r.seatId from Reservation r
      where r.seatId in :seatIds and r.status in :statuses
      """)
  Set<Long> findOccupiedSeatIdsAmong(
      @Param("seatIds") Collection<Long> seatIds,
      @Param("statuses") Collection<ReservationStatus> statuses);

  /**
   * 이 사용자의 예약 전부, 최근에 만든 것부터. 내 예매 목록(GET /reservations?userId=)에 쓴다.
   *
   * 상태로 거르지 않는다. 만료·취소된 예약도 사용자에게는 "내가 했던 예매"라 목록에 보인다.
   *
   * 최근 순은 id 역순으로 정한다. 예약에 만든 시각 컬럼이 없고, auto_increment id가 만든 순서를 따른다.
   * user_id 조건은 V2의 idx_reservation_user 인덱스를 탄다. 페이지 나누기는 하지 않는다.
   */
  List<Reservation> findAllByUserIdOrderByIdDesc(Long userId);
}
