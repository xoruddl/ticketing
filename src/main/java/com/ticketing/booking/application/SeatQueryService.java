package com.ticketing.booking.application;

import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.Schedule;
import com.ticketing.booking.domain.ScheduleNotFoundException;
import com.ticketing.booking.domain.ScheduleRepository;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatRepository;
import java.util.List;
import java.util.Set;
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
  private final ReservationRepository reservationRepository;

  /**
   * 회차의 좌석을 예매 가능 여부와 함께 id 순으로 돌려준다.
   *
   * 조회를 두 번 한다. 좌석 목록 한 번, 그 회차에서 차지된 좌석 ID 한 번. 좌석마다 팔렸는지 묻지 않는 이유는
   * 좌석이 N개면 조회도 N번이 되기 때문이다({@link ReservationRepository#findOccupiedSeatIds} 주석 참고).
   *
   * 예매 가능 여부는 어디에도 저장되어 있지 않다. 좌석 표에는 그런 컬럼이 없고, 여기서 두 결과를 대조해 만든다.
   * 같은 좌석이라도 회차가 다르면 답이 다르기 때문이다.
   *
   * 어떤 상태가 좌석을 차지하는지는 선점을 거절할 때와 같은 기준을 쓴다({@link ReservationStatus#occupying()}).
   * 이 둘이 갈라지면 목록에는 예매 가능인데 누르면 거절되는 좌석이 생긴다.
   */
  public List<SeatAvailability> findSeatAvailabilities(Long scheduleId) {
    // 회차를 먼저 찾는 일은 두 가지를 한다. 없는 회차를 여기서 거르고, 좌석을 찾을 때 쓸 공연 ID를 얻는다.
    // 없는 회차를 몇 번 상태로 응답할지는 여기서 정하지 않는다. web의 BookingExceptionHandler가 정한다.
    Schedule schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

    // 팔린 좌석은 "회차" 기준으로 묻는다. 아래 좌석 목록이 "공연" 기준인 것과 대비된다.
    // 좌석 행은 공연에 한 벌뿐이고 모든 회차가 함께 쓰므로, 팔렸는지는 회차마다 따로 계산해야 한다.
    // 여기에 공연 ID를 넘기면 다른 날짜에 팔린 좌석까지 막혀버린다.
    Set<Long> occupiedSeatIds =
        reservationRepository.findOccupiedSeatIds(scheduleId, ReservationStatus.occupying());

    // 좌석 목록은 "공연"에서 가져온다. 순서(id 순)는 저장소 메서드 이름이 보장한다.
    return seatRepository.findAllByPerformanceIdOrderByIdAsc(schedule.getPerformanceId()).stream()
        // 두 조회 결과를 여기서 합친다. 차지된 좌석 목록에 없으면 예매할 수 있다.
        // occupiedSeatIds가 Set이라 좌석 수만큼 대조해도 해시 조회라 싸다.
        .map(seat -> new SeatAvailability(seat, !occupiedSeatIds.contains(seat.getId())))
        .toList();
  }

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
