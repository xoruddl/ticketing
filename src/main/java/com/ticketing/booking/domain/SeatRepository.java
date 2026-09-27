package com.ticketing.booking.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 좌석 저장소. 좌석은 공연 단위로 만들어지므로, 회차의 좌석도 공연 ID로 찾는다. */
public interface SeatRepository extends JpaRepository<Seat, Long> {

  /**
   * 공연의 좌석 전체. 응답 순서가 매번 같도록 id 순으로 정렬한다.
   *
   * performance_id 조건은 uk_seat_position 유니크 인덱스의 첫 컬럼이라 인덱스를 탄다.
   */
  List<Seat> findAllByPerformanceIdOrderByIdAsc(Long performanceId);
}
