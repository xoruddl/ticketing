package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * 회차 없이 좌석 ID만으로 예약을 저장하고 찾는다. 실제 MySQL에서 쿼리가 도는지 확인한다.
 *
 * 회차를 빼는 동안의 중간 단계다 (DECISIONS.md "도메인: 회차(Schedule)를 뺀다").
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class ReservationRepositoryTest {

  private static final Long USER_ID = 7L;

  /** 저장소 테스트는 만료를 보지 않는다. 언제 돌려도 미래가 되도록 먼 시각으로 둔다. */
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2099, 1, 1, 0, 0);

  @Autowired ReservationRepository reservationRepository;
  @Autowired BookingFixture fixture;

  /** V5에서 schedule_id를 nullable로 바꿨다. 다시 읽을 때 ScheduledSeat 검증도 통과해야 한다. */
  @Test
  void 회차_없이_선점한_예약도_저장하고_다시_읽을_수_있다() {
    Stage stage = fixture.createStage(1);
    Reservation saved = reservationRepository.save(hold(stage.seatId(0)));

    Reservation found = reservationRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getSeat().scheduleId()).isNull();
    assertThat(found.getSeat().seatId()).isEqualTo(stage.seatId(0));
  }

  @Test
  void 예약이_없는_좌석은_차지되지_않았다() {
    Stage stage = fixture.createStage(1);

    assertThat(
            reservationRepository.existsBySeatIdAndStatusIn(
                stage.seatId(0), ReservationStatus.occupying()))
        .isFalse();
  }

  @Test
  void 선점된_좌석은_차지되었다() {
    Stage stage = fixture.createStage(1);
    reservationRepository.save(hold(stage.seatId(0)));

    assertThat(
            reservationRepository.existsBySeatIdAndStatusIn(
                stage.seatId(0), ReservationStatus.occupying()))
        .isTrue();
  }

  /** 받은 좌석 중에서만 찾는다. 다른 공연에서 선점된 좌석은 섞이지 않는다. */
  @Test
  void 주어진_좌석_중_차지된_좌석만_찾는다() {
    Stage stage = fixture.createStage(3);
    Stage other = fixture.createStage(1);
    reservationRepository.save(hold(stage.seatId(1)));
    reservationRepository.save(hold(other.seatId(0)));

    List<Long> seatIds = stage.seats().stream().map(Seat::getId).toList();

    assertThat(
            reservationRepository.findOccupiedSeatIdsAmong(seatIds, ReservationStatus.occupying()))
        .containsExactly(stage.seatId(1));
  }

  /** 좌석이 없는 공연도 있을 수 있다. 빈 목록으로 IN 절을 만들어도 쿼리가 깨지지 않아야 한다. */
  @Test
  void 좌석_목록이_비어_있으면_빈_결과다() {
    assertThat(
            reservationRepository.findOccupiedSeatIdsAmong(
                List.of(), ReservationStatus.occupying()))
        .isEmpty();
  }

  private Reservation hold(Long seatId) {
    return Reservation.hold(seatId, USER_ID, EXPIRES_AT);
  }
}
