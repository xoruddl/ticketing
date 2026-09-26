package com.ticketing.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import com.ticketing.booking.application.PaymentResult;
import com.ticketing.booking.application.PaymentService;
import com.ticketing.booking.application.ReservationService;
import com.ticketing.booking.domain.Reservation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** 예매 조회 API. 요청부터 DB 조회까지 실제로 거쳐 응답 JSON을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class ReservationQueryTest {

  private static final long USER_ID = 7L;

  @Autowired MockMvcTester mvc;
  @Autowired BookingFixture fixture;

  /** 조회할 예약은 서비스로 바로 만든다. 선점·결제 API 자체는 SeatHoldTest·ReservationPaymentTest가 본다. */
  @Autowired ReservationService reservationService;

  @Autowired PaymentService paymentService;

  @Test
  void 선점한_예약을_조회하면_결제와_티켓이_null로_나온다() {
    Reservation held = hold(USER_ID);

    // 200 application/json
    // {"reservationId":1,"scheduleId":10,"seatId":100,"status":"HELD",
    //  "expiresAt":"2026-10-01T19:05:00","payment":null,"ticket":null}
    assertThat(mvc.get().uri("/reservations/{reservationId}", held.getId()))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json).extractingPath("$.reservationId").isEqualTo(held.getId().intValue());
              assertThat(json)
                  .extractingPath("$.seatId")
                  .isEqualTo(held.getSeat().seatId().intValue());
              assertThat(json).extractingPath("$.status").isEqualTo("HELD");
              assertThat(json).extractingPath("$.expiresAt").isNotNull();
              // 필드가 빠진 것이 아니라 null로 나온다.
              assertThat(json).hasPath("$.payment");
              assertThat(json).extractingPath("$.payment").isNull();
              assertThat(json).hasPath("$.ticket");
              assertThat(json).extractingPath("$.ticket").isNull();
            });
  }

  @Test
  void 결제한_예약을_조회하면_결제와_티켓이_함께_나온다() {
    Reservation held = hold(USER_ID);
    PaymentResult paid = paymentService.pay(held.getId(), USER_ID);

    assertThat(mvc.get().uri("/reservations/{reservationId}", held.getId()))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json).extractingPath("$.status").isEqualTo("CONFIRMED");
              assertThat(json)
                  .extractingPath("$.payment.paymentId")
                  .isEqualTo(paid.payment().getId().intValue());
              assertThat(json)
                  .extractingPath("$.payment.amount")
                  .isEqualTo((int) BookingFixture.SEAT_PRICE);
              assertThat(json).extractingPath("$.payment.paidAt").isNotNull();
              assertThat(json)
                  .extractingPath("$.ticket.ticketId")
                  .isEqualTo(paid.ticket().getId().intValue());
              assertThat(json).extractingPath("$.ticket.code").isEqualTo(paid.ticket().getCode());
              assertThat(json).extractingPath("$.ticket.issuedAt").isNotNull();
            });
  }

  @Test
  void 없는_예약을_조회하면_404를_응답한다() {
    long missingReservationId = 0L;

    // 404 application/problem+json
    // {"detail":"예약을 찾을 수 없다: 0","instance":"/reservations/0","status":404,
    //  "title":"Not Found","code":"RESERVATION_NOT_FOUND"}
    assertThat(mvc.get().uri("/reservations/{reservationId}", missingReservationId))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .satisfies(
            json -> assertThat(json).extractingPath("$.code").isEqualTo("RESERVATION_NOT_FOUND"));
  }

  @Test
  void 내_예매_목록을_조회하면_최근_예약부터_요약으로_나온다() {
    Long userId = BookingFixture.newUserId();
    Reservation first = hold(userId);
    Reservation second = hold(userId);
    paymentService.pay(first.getId(), userId);

    // 200 application/json
    // [{"reservationId":2,"scheduleId":11,"seatId":101,"status":"HELD","expiresAt":"..."},
    //  {"reservationId":1,"scheduleId":10,"seatId":100,"status":"CONFIRMED","expiresAt":"..."}]
    assertThat(mvc.get().uri("/reservations").param("userId", userId.toString()))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json)
                  .extractingPath("$[*].reservationId")
                  .asArray()
                  .containsExactly(second.getId().intValue(), first.getId().intValue());
              assertThat(json)
                  .extractingPath("$[*].status")
                  .asArray()
                  .containsExactly("HELD", "CONFIRMED");
              // 목록은 요약이다. 결제·티켓은 단건 조회에서 본다.
              assertThat(json).doesNotHavePath("$[0].payment");
            });
  }

  @Test
  void 예약이_없는_사용자의_목록은_빈_배열이다() {
    assertThat(
            mvc.get().uri("/reservations").param("userId", BookingFixture.newUserId().toString()))
        .hasStatusOk()
        .bodyJson()
        .isEqualTo("[]");
  }

  /** 새 공연의 좌석 하나를 이 사용자로 선점한다. 테스트마다 새 공연이라 서로 좌석이 겹치지 않는다. */
  private Reservation hold(long userId) {
    Stage stage = fixture.createStage(1);
    return reservationService.hold(stage.scheduleId(), stage.seatId(0), userId);
  }
}
