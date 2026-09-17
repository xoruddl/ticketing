package com.ticketing.booking;

import com.ticketing.booking.domain.Performance;
import com.ticketing.booking.domain.PerformanceRepository;
import com.ticketing.booking.domain.Schedule;
import com.ticketing.booking.domain.ScheduleRepository;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatGrade;
import com.ticketing.booking.domain.SeatPosition;
import com.ticketing.booking.domain.SeatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

/**
 * 예매 테스트에 필요한 공연·회차·좌석을 DB에 준비한다.
 *
 * 테스트끼리 DB를 함께 쓰므로, 부를 때마다 새 공연을 만들어 다른 테스트의 데이터와 섞이지 않게 한다.
 * 쓰는 테스트에서 @Import(BookingFixture.class)로 가져온다.
 */
@TestComponent
@RequiredArgsConstructor
public class BookingFixture {

  /** 픽스처 좌석의 가격. 결제 금액을 검증할 때 기준으로 쓴다. */
  public static final long SEAT_PRICE = 150_000L;

  private final PerformanceRepository performanceRepository;
  private final ScheduleRepository scheduleRepository;
  private final SeatRepository seatRepository;

  /**
   * 공연 1개, 회차 1개, 좌석 seatCount개를 저장한다.
   *
   * 좌석은 A구역 1열에 1번부터 번호를 붙인다. 예: seatCount=3 이면 A-1-1, A-1-2, A-1-3
   */
  public Stage createStage(int seatCount) {
    Performance performance = performanceRepository.save(new Performance("레미제라블", "블루스퀘어"));
    Schedule schedule =
        scheduleRepository.save(
            new Schedule(performance.getId(), LocalDateTime.of(2026, 12, 24, 19, 0)));
    List<Seat> seats =
        IntStream.rangeClosed(1, seatCount)
            .mapToObj(number -> createSeat(performance, number))
            .toList();
    return new Stage(schedule, seatRepository.saveAll(seats));
  }

  private Seat createSeat(Performance performance, int number) {
    SeatPosition position = new SeatPosition("A", "1", number);
    return new Seat(performance.getId(), position, SeatGrade.VIP, SEAT_PRICE);
  }

  /** 준비된 회차와 좌석. 저장 후 받은 엔티티라 ID가 채워져 있다. */
  public record Stage(Schedule schedule, List<Seat> seats) {

    public Long scheduleId() {
      return schedule.getId();
    }

    /** index번째(0부터) 좌석의 ID. */
    public Long seatId(int index) {
      return seats.get(index).getId();
    }
  }
}
