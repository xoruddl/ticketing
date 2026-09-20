package com.ticketing.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.Schedule;
import com.ticketing.booking.domain.ScheduleNotFoundException;
import com.ticketing.booking.domain.ScheduleRepository;
import com.ticketing.booking.domain.ScheduledSeat;
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
  @Autowired ScheduleRepository scheduleRepository;
  @Autowired ReservationRepository reservationRepository;

  @Test
  void 아무도_선점하지_않은_회차는_모든_좌석이_예매_가능하다() {
    Stage stage = fixture.createStage(3);

    List<SeatAvailability> seats = seatQueryService.findSeatAvailabilities(stage.scheduleId());

    assertThat(seats).hasSize(3).allMatch(SeatAvailability::available);
  }

  @Test
  void 선점된_좌석만_예매_불가로_나온다() {
    Stage stage = fixture.createStage(3);
    reservationService.hold(stage.scheduleId(), stage.seatId(1), USER_ID);

    List<SeatAvailability> seats = seatQueryService.findSeatAvailabilities(stage.scheduleId());

    // 좌석은 id 순으로 오므로 순서가 픽스처가 만든 순서와 같다. 가운데 좌석만 막혀 있어야 한다.
    assertThat(seats).extracting(SeatAvailability::available).containsExactly(true, false, true);
    assertThat(seats.get(1).seat().getId()).isEqualTo(stage.seatId(1));
  }

  /**
   * 좌석 행은 공연에 하나뿐이고 모든 회차가 함께 쓴다. 그래서 "팔렸다"는 회차마다 따로 계산되어야 한다.
   *
   * 이 구분이 깨지면 24일 공연의 A-1-2가 팔렸다는 이유로 25일 공연의 A-1-2까지 막힌다.
   */
  @Test
  void 다른_회차의_선점은_이_회차의_좌석을_막지_않는다() {
    Stage stage = fixture.createStage(2);
    // 같은 공연의 다른 회차. 좌석은 위 stage와 같은 행을 공유한다.
    Schedule otherSchedule =
        scheduleRepository.save(
            new Schedule(
                stage.schedule().getPerformanceId(), LocalDateTime.of(2026, 12, 25, 19, 0)));
    reservationService.hold(otherSchedule.getId(), stage.seatId(0), USER_ID);

    List<SeatAvailability> seats = seatQueryService.findSeatAvailabilities(stage.scheduleId());

    assertThat(seats).allMatch(SeatAvailability::available);
  }

  /**
   * 만료 시각이 지난 선점도 좌석을 계속 막는다. 고쳐야 할 것이지만 Step 0에서는 그대로 둔다.
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
    reservationRepository.save(
        Reservation.hold(
            new ScheduledSeat(stage.scheduleId(), stage.seatId(0)), USER_ID, alreadyPassed));

    List<SeatAvailability> seats = seatQueryService.findSeatAvailabilities(stage.scheduleId());

    assertThat(seats).singleElement().extracting(SeatAvailability::available).isEqualTo(false);
  }

  @Test
  void 없는_회차의_좌석은_조회할_수_없다() {
    long missingScheduleId = 0L;

    assertThatThrownBy(() -> seatQueryService.findSeatAvailabilities(missingScheduleId))
        .isInstanceOf(ScheduleNotFoundException.class);
  }
}
