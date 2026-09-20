package com.ticketing.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.ScheduleNotFoundException;
import com.ticketing.booking.domain.SeatAlreadyTakenException;
import com.ticketing.booking.domain.SeatNotFoundException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** 좌석 선점. 실제 DB에 저장하고 거절 규칙을 확인한다. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class ReservationServiceTest {

  private static final Long USER_ID = 7L;
  private static final Long OTHER_USER_ID = 8L;

  @Autowired ReservationService reservationService;
  @Autowired BookingFixture fixture;

  @Test
  void 좌석을_선점하면_선점_상태로_저장된다() {
    Stage stage = fixture.createStage(1);

    Reservation reservation = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    assertThat(reservation.getId()).isNotNull();
    assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
    assertThat(reservation.getUserId()).isEqualTo(USER_ID);
    assertThat(reservation.getSeat().scheduleId()).isEqualTo(stage.scheduleId());
    assertThat(reservation.getSeat().seatId()).isEqualTo(stage.seatId(0));
  }

  /** 만료 시각이 몇 분 뒤인지는 HoldPolicyTest가 본다. 여기서는 선점에 기한이 붙는지만 확인한다. */
  @Test
  void 선점에는_만료_시각이_붙는다() {
    Stage stage = fixture.createStage(1);

    Reservation reservation = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    assertThat(reservation.getExpiresAt()).isAfter(LocalDateTime.now());
    assertThat(reservation.isExpired(LocalDateTime.now())).isFalse();
  }

  @Test
  void 이미_선점된_좌석은_다른_사용자가_선점할_수_없다() {
    Stage stage = fixture.createStage(1);
    reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    assertThatThrownBy(
            () -> reservationService.hold(stage.scheduleId(), stage.seatId(0), OTHER_USER_ID))
        .isInstanceOf(SeatAlreadyTakenException.class);
  }

  /** 같은 사용자라도 막는다. 한 사람이 같은 좌석을 두 번 잡을 이유가 없다. */
  @Test
  void 이미_선점한_좌석은_같은_사용자도_다시_선점할_수_없다() {
    Stage stage = fixture.createStage(1);
    reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    assertThatThrownBy(() -> reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID))
        .isInstanceOf(SeatAlreadyTakenException.class);
  }

  @Test
  void 옆_좌석은_따로_선점된다() {
    Stage stage = fixture.createStage(2);
    reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    Reservation reservation =
        reservationService.hold(stage.scheduleId(), stage.seatId(1), OTHER_USER_ID);

    assertThat(reservation.getSeat().seatId()).isEqualTo(stage.seatId(1));
  }

  @Test
  void 없는_회차는_선점할_수_없다() {
    Stage stage = fixture.createStage(1);
    long missingScheduleId = 0L;

    assertThatThrownBy(() -> reservationService.hold(missingScheduleId, stage.seatId(0), USER_ID))
        .isInstanceOf(ScheduleNotFoundException.class);
  }

  @Test
  void 없는_좌석은_선점할_수_없다() {
    Stage stage = fixture.createStage(1);
    long missingSeatId = 0L;

    assertThatThrownBy(() -> reservationService.hold(stage.scheduleId(), missingSeatId, USER_ID))
        .isInstanceOf(SeatNotFoundException.class);
  }

  /** 좌석은 공연에 붙어 있다. 다른 공연의 좌석 ID로는 이 회차를 예매할 수 없다. */
  @Test
  void 다른_공연의_좌석은_선점할_수_없다() {
    Stage stage = fixture.createStage(1);
    Stage otherStage = fixture.createStage(1);

    assertThatThrownBy(
            () -> reservationService.hold(stage.scheduleId(), otherStage.seatId(0), USER_ID))
        .isInstanceOf(SeatNotFoundException.class);
  }
}
