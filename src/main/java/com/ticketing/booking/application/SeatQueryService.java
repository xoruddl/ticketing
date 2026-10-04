package com.ticketing.booking.application;

import com.ticketing.booking.domain.PerformanceNotFoundException;
import com.ticketing.booking.domain.PerformanceRepository;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공연의 좌석을 조회한다.
 *
 * 좌석은 공연에 붙어 있으므로 공연 ID로 좌석을 가져온다. 예매 가능 여부는 그 좌석들의 예약을 보고 좌석마다 붙인다.
 */
@Service
@RequiredArgsConstructor
// 조회만 하므로 readOnly로 둔다. Hibernate가 변경 감지용 스냅샷을 만들지 않아 가볍다.
@Transactional(readOnly = true)
public class SeatQueryService {

  private final PerformanceRepository performanceRepository;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;

  /**
   * 공연의 좌석을 예매 가능 여부와 함께 id 순으로 돌려준다.
   *
   * 조회를 두 번 한다. 좌석 목록 한 번, 그 좌석 중 차지된 좌석 ID 한 번. 좌석마다 팔렸는지 묻지 않는 이유는 좌석이
   * N개면 조회도 N번이 되기 때문이다. 좌석 목록을 먼저 가져오는 이유는 예약에 공연 ID가 없어서다. 좌석 ID를 알아야
   * 차지된 좌석을 물을 수 있다({@link ReservationRepository#findOccupiedSeatIdsAmong} 주석 참고).
   *
   * 예매 가능 여부는 어디에도 저장되어 있지 않다. 좌석 표에는 그런 컬럼이 없고, 여기서 두 결과를 대조해 만든다.
   *
   * 어떤 상태가 좌석을 차지하는지는 선점을 거절할 때와 같은 기준을 쓴다({@link ReservationStatus#occupying()}).
   * 이 둘이 갈라지면 목록에는 예매 가능인데 누르면 거절되는 좌석이 생긴다.
   */
  public List<SeatAvailability> findSeatAvailabilitiesByPerformance(Long performanceId) {
    // 좌석이 하나도 없는 것과 공연이 없는 것을 구분하려고 공연부터 확인한다. 좌석 목록만 보면 둘 다 빈 목록이다.
    // 없는 공연을 몇 번 상태로 응답할지는 여기서 정하지 않는다. web의 BookingExceptionHandler가 정한다.
    if (!performanceRepository.existsById(performanceId)) {
      throw new PerformanceNotFoundException(performanceId);
    }

    // 순서(id 순)는 저장소 메서드 이름이 보장한다.
    List<Seat> seats = seatRepository.findAllByPerformanceIdOrderByIdAsc(performanceId);
    List<Long> seatIds = seats.stream().map(Seat::getId).toList();
    Set<Long> occupiedSeatIds =
        reservationRepository.findOccupiedSeatIdsAmong(seatIds, ReservationStatus.occupying());

    return seats.stream()
        // 두 조회 결과를 여기서 합친다. 차지된 좌석 목록에 없으면 예매할 수 있다.
        // occupiedSeatIds가 Set이라 좌석 수만큼 대조해도 해시 조회라 싸다.
        .map(seat -> new SeatAvailability(seat, !occupiedSeatIds.contains(seat.getId())))
        .toList();
  }
}
