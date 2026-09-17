package com.ticketing.booking.application;

import com.ticketing.booking.domain.Schedule;
import com.ticketing.booking.domain.ScheduleNotFoundException;
import com.ticketing.booking.domain.ScheduleRepository;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회차의 좌석을 조회한다.
 *
 * 좌석은 회차가 아니라 공연에 붙어 있으므로, 회차를 먼저 찾아 그 공연 ID로 좌석을 가져온다. 예매 가능 여부는 예약이 생기는
 * 선점 단계에서 더한다.
 */
@Service
@RequiredArgsConstructor
// 조회만 하므로 readOnly로 둔다. Hibernate가 변경 감지용 스냅샷을 만들지 않아 가볍다.
@Transactional(readOnly = true)
public class SeatQueryService {

  private final ScheduleRepository scheduleRepository;
  private final SeatRepository seatRepository;

  /**
   * 회차의 좌석 전체를 id 순으로 돌려준다.
   *
   * 응답 모양은 web 계층이 정하도록, 여기서는 엔티티를 그대로 돌려준다. Seat은 지연 로딩할 연관이 없어 트랜잭션 밖에서
   * 읽어도 안전하다.
   */
  public List<Seat> findSeats(Long scheduleId) {
    // 없는 회차는 도메인 예외로 알린다. 몇 번 상태로 응답할지는 web의 BookingExceptionHandler가 정한다.
    Schedule schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    return seatRepository.findAllByPerformanceIdOrderByIdAsc(schedule.getPerformanceId());
  }
}
