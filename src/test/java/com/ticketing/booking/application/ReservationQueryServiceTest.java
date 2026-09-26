package com.ticketing.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import com.ticketing.booking.domain.Payment;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationNotFoundException;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** 예매 조회. 실제 DB에 선점·결제를 남긴 뒤, 세 표에서 읽어 묶은 결과를 확인한다. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class ReservationQueryServiceTest {

  private static final Long USER_ID = 7L;

  @Autowired ReservationQueryService reservationQueryService;
  @Autowired ReservationService reservationService;
  @Autowired PaymentService paymentService;
  @Autowired BookingFixture fixture;

  @Test
  void 선점만_한_예약은_결제와_티켓_없이_조회된다() {
    Stage stage = fixture.createStage(1);
    Reservation held = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);

    ReservationDetail detail = reservationQueryService.find(held.getId());

    assertThat(detail.reservation().getId()).isEqualTo(held.getId());
    assertThat(detail.reservation().getStatus()).isEqualTo(ReservationStatus.HELD);
    assertThat(detail.payment()).isEmpty();
    assertThat(detail.ticket()).isEmpty();
  }

  /** 결제가 남긴 것이 결제 응답뿐 아니라 조회에서도 다시 보인다. */
  @Test
  void 결제한_예약은_결제와_티켓과_함께_조회된다() {
    Stage stage = fixture.createStage(1);
    Reservation held = reservationService.hold(stage.scheduleId(), stage.seatId(0), USER_ID);
    PaymentResult paid = paymentService.pay(held.getId(), USER_ID);

    ReservationDetail detail = reservationQueryService.find(held.getId());

    assertThat(detail.reservation().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    assertThat(detail.payment()).get().extracting(Payment::getId).isEqualTo(paid.payment().getId());
    assertThat(detail.ticket())
        .get()
        .extracting(Ticket::getCode)
        .isEqualTo(paid.ticket().getCode());
  }

  @Test
  void 없는_예약은_조회할_수_없다() {
    // id는 IDENTITY로 1부터 매겨지므로 0번 예약은 항상 없다.
    assertThatThrownBy(() -> reservationQueryService.find(0L))
        .isInstanceOf(ReservationNotFoundException.class);
  }

  @Test
  void 내_예매_목록은_최근에_만든_예약부터_나온다() {
    Long userId = BookingFixture.newUserId();
    Stage stage = fixture.createStage(2);
    Reservation first = reservationService.hold(stage.scheduleId(), stage.seatId(0), userId);
    Reservation second = reservationService.hold(stage.scheduleId(), stage.seatId(1), userId);

    assertThat(reservationQueryService.findAllByUser(userId))
        .extracting(Reservation::getId)
        .containsExactly(second.getId(), first.getId());
  }

  @Test
  void 내_예매_목록에_다른_사용자의_예약은_나오지_않는다() {
    Long userId = BookingFixture.newUserId();
    Long otherUserId = BookingFixture.newUserId();
    Stage stage = fixture.createStage(2);
    Reservation mine = reservationService.hold(stage.scheduleId(), stage.seatId(0), userId);
    reservationService.hold(stage.scheduleId(), stage.seatId(1), otherUserId);

    assertThat(reservationQueryService.findAllByUser(userId))
        .extracting(Reservation::getId)
        .containsExactly(mine.getId());
  }

  /** 예약이 없는 사용자는 거절이 아니라 빈 목록이다. 사용자 표가 아직 없어 "없는 사용자"를 가릴 수도 없다. */
  @Test
  void 예약이_없는_사용자의_목록은_비어_있다() {
    assertThat(reservationQueryService.findAllByUser(BookingFixture.newUserId())).isEmpty();
  }
}
