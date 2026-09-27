package com.ticketing.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationExpiredException;
import com.ticketing.booking.domain.ReservationNotFoundException;
import com.ticketing.booking.domain.ReservationNotHeldException;
import com.ticketing.booking.domain.ReservationNotOwnedException;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** 결제 → 확정 → 발권. 실제 DB에 저장하고 거절 규칙을 확인한다. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class PaymentServiceTest {

  private static final Long USER_ID = 7L;
  private static final Long OTHER_USER_ID = 8L;

  @Autowired PaymentService paymentService;
  @Autowired ReservationService reservationService;
  @Autowired ReservationRepository reservationRepository;
  @Autowired BookingFixture fixture;

  @Test
  void 결제하면_예약이_확정되고_티켓이_발급된다() {
    Stage stage = fixture.createStage(1);
    Reservation held = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    PaymentResult result = paymentService.pay(held.getId(), USER_ID);

    assertThat(result.reservation().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    // 결제 금액은 요청이 아니라 좌석 가격에서 온다.
    assertThat(result.payment().getId()).isNotNull();
    assertThat(result.payment().getAmount()).isEqualTo(BookingFixture.SEAT_PRICE);
    assertThat(result.ticket().getId()).isNotNull();
    assertThat(result.ticket().getReservationId()).isEqualTo(held.getId());
  }

  /** 돌려받은 객체가 아니라 DB를 다시 읽어 확인한다. 확정이 커밋까지 되었는지는 여기서만 보인다. */
  @Test
  void 확정된_상태가_저장된다() {
    Stage stage = fixture.createStage(1);
    Reservation held = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    paymentService.pay(held.getId(), USER_ID);

    assertThat(reservationRepository.findById(held.getId()))
        .get()
        .extracting(Reservation::getStatus)
        .isEqualTo(ReservationStatus.CONFIRMED);
  }

  @Test
  void 없는_예약은_결제할_수_없다() {
    // id는 IDENTITY로 1부터 매겨지므로 0번 예약은 항상 없다.
    assertThatThrownBy(() -> paymentService.pay(0L, USER_ID))
        .isInstanceOf(ReservationNotFoundException.class);
  }

  @Test
  void 다른_사용자의_선점은_결제할_수_없다() {
    Stage stage = fixture.createStage(1);
    Reservation held = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    assertThatThrownBy(() -> paymentService.pay(held.getId(), OTHER_USER_ID))
        .isInstanceOf(ReservationNotOwnedException.class);
  }

  /** 거절되면 아무것도 남지 않는다. 예약은 선점 그대로다. */
  @Test
  void 만료된_선점은_결제할_수_없다() {
    Stage stage = fixture.createStage(1);
    Reservation expired = fixture.createExpiredHold(stage, 0, USER_ID);

    assertThatThrownBy(() -> paymentService.pay(expired.getId(), USER_ID))
        .isInstanceOf(ReservationExpiredException.class);
    assertThat(reservationRepository.findById(expired.getId()))
        .get()
        .extracting(Reservation::getStatus)
        .isEqualTo(ReservationStatus.HELD);
  }

  /** 같은 결제가 다시 와도 두 번 결제되지 않는다(단일 스레드에서만). 동시에 두 번 오는 경우는 Step 1에서 본다. */
  @Test
  void 이미_결제한_예약은_다시_결제할_수_없다() {
    Stage stage = fixture.createStage(1);
    Reservation held = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);
    paymentService.pay(held.getId(), USER_ID);

    assertThatThrownBy(() -> paymentService.pay(held.getId(), USER_ID))
        .isInstanceOf(ReservationNotHeldException.class);
  }
}
