package com.ticketing.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import com.ticketing.booking.domain.PerformanceNotFoundException;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** 좌석 조회의 예매 가능 여부. 실제 DB에 예약을 만들어 두고 조회 결과가 달라지는지 확인한다. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class SeatQueryServiceTest {

  private static final Long USER_ID = 7L;

  @Autowired SeatQueryService seatQueryService;
  @Autowired ReservationService reservationService;
  @Autowired BookingFixture fixture;
  @Autowired ReservationRepository reservationRepository;

  @Test
  void 아무도_선점하지_않은_공연은_모든_좌석이_예매_가능하다() {
    Stage stage = fixture.createStage(3);

    List<SeatAvailability> seats =
        seatQueryService.findSeatAvailabilitiesByPerformance(stage.performanceId());

    assertThat(seats).hasSize(3).allMatch(SeatAvailability::available);
  }

  @Test
  void 선점된_좌석만_예매_불가로_나온다() {
    Stage stage = fixture.createStage(3);
    reservationService.hold(stage.seatId(1), USER_ID);

    List<SeatAvailability> seats =
        seatQueryService.findSeatAvailabilitiesByPerformance(stage.performanceId());

    // 아래 단언이 순서에 기대므로, 좌석이 어떤 순서로 오는지를 먼저 못 박는다.
    // 이게 없으면 정렬이 뒤집혀도 테스트가 통과한다. 셋 중 가운데가 막힌 모양은 뒤집어도 똑같기 때문이다.
    assertThat(seats)
        .extracting(availability -> availability.seat().getId())
        .containsExactly(stage.seatId(0), stage.seatId(1), stage.seatId(2));
    // 선점한 가운데 좌석만 막히고 양옆은 그대로다.
    assertThat(seats).extracting(SeatAvailability::available).containsExactly(true, false, true);
  }

  /**
   * 차지된 좌석은 이 공연의 좌석 중에서만 찾는다. 다른 공연에서 선점된 좌석이 이 공연의 목록을 막으면 안 된다.
   *
   * 예약에는 공연 ID가 없어서, 이 구분은 "이 공연의 좌석 ID 목록"을 넘기는 것으로만 지켜진다.
   */
  @Test
  void 다른_공연의_선점은_이_공연의_좌석을_막지_않는다() {
    Stage stage = fixture.createStage(2);
    Stage other = fixture.createStage(2);
    reservationService.hold(other.seatId(0), USER_ID);

    assertThat(seatQueryService.findSeatAvailabilitiesByPerformance(stage.performanceId()))
        .allMatch(SeatAvailability::available);
    // 선점한 공연에서는 실제로 막혀 있어야 한다. 이 단언이 없으면 선점이 아무 일도 하지 않았을 때도
    // 위 단언이 그냥 통과해, 공연끼리 갈라져 있다는 것을 확인하지 못한 채 초록이 된다.
    assertThat(seatQueryService.findSeatAvailabilitiesByPerformance(other.performanceId()))
        .extracting(SeatAvailability::available)
        .containsExactly(false, true);
  }

  /**
   * 만료 시각이 지난 선점도 좌석을 계속 막는다. 고쳐야 할 것이지만 아직 그대로 둔다.
   *
   * 만료된 선점을 누가 언제 풀어줄지가 Step 3의 문제다. 여기서 조회할 때만 슬쩍 풀어주면 선점 거절 쪽과 기준이
   * 갈라져, 목록에는 예매 가능인데 누르면 거절되는 좌석이 생긴다. 이 테스트는 지금 상태를 드러내 두는 것이고,
   * Step 3에서 기대값이 바뀐다.
   */
  @Test
  void 만료된_선점도_아직은_좌석을_막는다() {
    Stage stage = fixture.createStage(1);
    // 서비스를 거치지 않고 저장한다. 선점 API로는 이미 지난 만료 시각을 만들 수 없다.
    LocalDateTime alreadyPassed = LocalDateTime.of(2020, 1, 1, 0, 0);
    reservationRepository.save(Reservation.hold(stage.seatId(0), USER_ID, alreadyPassed));

    List<SeatAvailability> seats =
        seatQueryService.findSeatAvailabilitiesByPerformance(stage.performanceId());

    assertThat(seats).singleElement().extracting(SeatAvailability::available).isEqualTo(false);
  }

  @Test
  void 없는_공연의_좌석은_조회할_수_없다() {
    long missingPerformanceId = 0L;

    assertThatThrownBy(
            () -> seatQueryService.findSeatAvailabilitiesByPerformance(missingPerformanceId))
        .isInstanceOf(PerformanceNotFoundException.class);
  }
}
