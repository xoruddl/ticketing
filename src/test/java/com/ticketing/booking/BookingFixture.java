package com.ticketing.booking;

import com.ticketing.booking.domain.Performance;
import com.ticketing.booking.domain.PerformanceRepository;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.Schedule;
import com.ticketing.booking.domain.ScheduleRepository;
import com.ticketing.booking.domain.ScheduledSeat;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatGrade;
import com.ticketing.booking.domain.SeatPosition;
import com.ticketing.booking.domain.SeatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

/**
 * 예매 테스트에 필요한 공연·회차·좌석을 DB에 준비한다.
 *
 * 통합 테스트들은 같은 MySQL 컨테이너를 함께 쓴다. 공연 하나를 같이 쓰면 한 테스트가 선점한 좌석이 다른 테스트에서
 * 예매 불가로 보일 수 있어, 부를 때마다 새 공연을 만들어 테스트끼리 데이터가 섞이지 않게 한다.
 *
 * 롤백(@Transactional) 대신 이 방식을 쓰는 이유: 이후 동시성 테스트에서는 여러 스레드가 커밋된 데이터를 봐야 해서
 * 테스트 트랜잭션으로 감쌀 수 없다.
 *
 * @TestComponent라 컴포넌트 스캔에 잡히지 않는다. 쓰는 테스트에서 @Import(BookingFixture.class)로 가져온다.
 */
@TestComponent
@RequiredArgsConstructor
public class BookingFixture {

  /** 픽스처 좌석의 가격. 테스트가 응답 가격이나 결제 금액을 비교할 때 숫자를 따로 쓰지 않고 이 값을 기준으로 쓴다. */
  public static final long SEAT_PRICE = 150_000L;

  /** 만료된 선점의 만료 시각. 테스트를 언제 돌려도 과거가 되도록 충분히 이른 시각으로 고정한다. */
  private static final LocalDateTime EXPIRED_AT = LocalDateTime.of(2020, 1, 1, 0, 0);

  /**
   * {@link #newUserId()}가 다음에 줄 사용자 ID. 테스트들이 상수로 쓰는 7, 8 같은 작은 값과 겹치지 않게 크게 시작한다.
   *
   * static인 이유: 테스트 클래스마다 픽스처 빈이 따로 생겨도 JVM 안에서 한 번 준 값을 다시 주지 않게 한다.
   */
  private static final AtomicLong NEXT_USER_ID = new AtomicLong(1_000_000L);

  // 실제 DB(Testcontainers MySQL)에 저장한다. 생성자는 Lombok이 만들고 스프링이 주입한다.
  private final PerformanceRepository performanceRepository;
  private final ScheduleRepository scheduleRepository;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;

  /**
   * 공연 1개, 회차 1개, 좌석 seatCount개를 저장한다.
   *
   * 좌석은 A구역 1열에 1번부터 번호를 붙인다. 예: seatCount=3 이면 A-1-1, A-1-2, A-1-3
   */
  public Stage createStage(int seatCount) {
    // 저장해야 auto_increment ID가 생기므로, 공연 → 회차 → 좌석 순서로 저장하며 앞의 ID를 넘긴다.
    Performance performance = performanceRepository.save(new Performance("레미제라블", "블루스퀘어"));
    // 엔티티는 연관관계 대신 ID로 참조하므로 공연 객체가 아니라 공연 ID를 넘긴다.
    Schedule schedule =
        scheduleRepository.save(
            new Schedule(performance.getId(), LocalDateTime.of(2026, 12, 24, 19, 0)));
    // 좌석은 회차가 아니라 공연에 붙는다. 같은 공연의 회차는 모두 이 좌석을 함께 쓴다.
    List<Seat> seats =
        IntStream.rangeClosed(1, seatCount)
            .mapToObj(number -> createSeat(performance, number))
            .toList();
    // saveAll이 돌려준 목록을 쓴다. 테스트가 좌석 ID를 꺼내 써야 하기 때문이다.
    return new Stage(schedule, seatRepository.saveAll(seats));
  }

  /**
   * 만료 시각이 이미 지난 선점을 저장한다. 만료된 선점으로 결제하면 거절되는지 볼 때 쓴다.
   *
   * 선점 API(ReservationService)를 거치지 않고 저장소로 바로 넣는다. API로 만들면 만료 시각이 "지금 + 5분"으로
   * 박혀서, 만료를 보려면 시계를 바꾸거나 5분을 기다려야 한다. 여기서는 만료 시각만 과거로 두면 된다.
   *
   * 상태는 HELD 그대로다. Step 0은 만료된 선점을 EXPIRED로 바꿔주지 않으므로, 실제로도 이런 행이 남는다.
   */
  public Reservation createExpiredHold(Stage stage, int seatIndex, Long userId) {
    ScheduledSeat seat = new ScheduledSeat(stage.scheduleId(), stage.seatId(seatIndex));
    return reservationRepository.save(Reservation.hold(seat, userId, EXPIRED_AT));
  }

  /**
   * 다른 테스트가 쓰지 않은 사용자 ID. 내 예매 목록처럼 사용자 기준으로 모아 보는 테스트에 쓴다.
   *
   * 통합 테스트들은 같은 DB를 함께 쓰므로, 상수 사용자(예: 7번)로 목록을 조회하면 다른 테스트가 만든 예약까지 섞여
   * 나온다. 공연을 부를 때마다 새로 만드는 것({@link #createStage})과 같은 이유다.
   */
  public static Long newUserId() {
    return NEXT_USER_ID.getAndIncrement();
  }

  /**
   * 번호만 다른 VIP 좌석 하나를 만든다(저장은 하지 않는다).
   *
   * 한 공연 안에서 위치가 겹치면 uk_seat_position 유니크 키에 걸리므로, 구역·열은 고정하고 번호로 구분한다.
   */
  private Seat createSeat(Performance performance, int number) {
    SeatPosition position = new SeatPosition("A", "1", number);
    return new Seat(performance.getId(), position, SeatGrade.VIP, SEAT_PRICE);
  }

  /**
   * 준비된 회차와 좌석. 저장 후 받은 엔티티라 ID가 채워져 있다.
   *
   * 테스트에서는 ID가 가장 자주 필요해 편의 메서드를 둔다. 예: stage.scheduleId()로 요청 URL을 만들고,
   * stage.seatId(0)으로 첫 좌석 ID를 비교한다.
   */
  public record Stage(Schedule schedule, List<Seat> seats) {

    /** 준비된 회차의 ID. */
    public Long scheduleId() {
      return schedule.getId();
    }

    /** index번째(0부터) 좌석의 ID. */
    public Long seatId(int index) {
      return seats.get(index).getId();
    }
  }
}
