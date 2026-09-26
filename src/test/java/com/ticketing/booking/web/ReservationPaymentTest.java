package com.ticketing.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
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
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** 결제 API. 요청부터 DB 저장까지 실제로 거쳐 응답을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class ReservationPaymentTest {

  private static final long USER_ID = 7L;
  private static final long OTHER_USER_ID = 8L;

  @Autowired MockMvcTester mvc;
  @Autowired BookingFixture fixture;

  /** 결제 전 선점은 서비스로 바로 만든다. 선점 API 자체는 SeatHoldTest가 본다. */
  @Autowired ReservationService reservationService;

  @Test
  void 결제하면_200과_확정된_예약과_티켓을_응답한다() {
    Reservation held = hold(USER_ID);

    // 200 application/json
    // {
    //   "reservationId":1,
    //   "status":"CONFIRMED",
    //   "paymentId":3,
    //   "amount":150000,
    //   "ticketId":5,
    //   "ticketCode":"3f2a9c1e-8b7d-4e21-9f0a-6c5d4b3a2e1f"
    // }
    assertThat(pay(held.getId(), USER_ID))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json).extractingPath("$.reservationId").isEqualTo(held.getId().intValue());
              assertThat(json).extractingPath("$.status").isEqualTo("CONFIRMED");
              // 금액은 요청에 없다. 좌석 가격이 그대로 결제 금액이 된다.
              assertThat(json)
                  .extractingPath("$.amount")
                  .isEqualTo((int) BookingFixture.SEAT_PRICE);
              // ID와 코드는 실행마다 다르므로 채워졌는지만 본다.
              assertThat(json).extractingPath("$.paymentId").isNotNull();
              assertThat(json).extractingPath("$.ticketId").isNotNull();
              assertThat(json).extractingPath("$.ticketCode").isNotNull();
            });
  }

  @Test
  void 없는_예약을_결제하면_404를_응답한다() {
    long missingReservationId = 0L;

    // 404 application/problem+json
    // {"detail":"예약을 찾을 수 없다: 0","instance":"/reservations/0/payment","status":404,
    //  "title":"Not Found","code":"RESERVATION_NOT_FOUND"}
    assertThat(pay(missingReservationId, USER_ID))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .satisfies(
            json -> assertThat(json).extractingPath("$.code").isEqualTo("RESERVATION_NOT_FOUND"));
  }

  @Test
  void 다른_사용자의_예약을_결제하면_403을_응답한다() {
    Reservation held = hold(USER_ID);

    assertThat(pay(held.getId(), OTHER_USER_ID))
        .hasStatus(HttpStatus.FORBIDDEN)
        .bodyJson()
        .satisfies(
            json -> assertThat(json).extractingPath("$.code").isEqualTo("RESERVATION_NOT_OWNED"));
  }

  @Test
  void 만료된_선점을_결제하면_409를_응답한다() {
    Stage stage = fixture.createStage(1);
    Reservation expired = fixture.createExpiredHold(stage, 0, USER_ID);

    assertThat(pay(expired.getId(), USER_ID))
        .hasStatus(HttpStatus.CONFLICT)
        .bodyJson()
        .satisfies(
            json -> assertThat(json).extractingPath("$.code").isEqualTo("RESERVATION_EXPIRED"));
  }

  @Test
  void 이미_결제한_예약을_다시_결제하면_409를_응답한다() {
    Reservation held = hold(USER_ID);
    pay(held.getId(), USER_ID);

    assertThat(pay(held.getId(), USER_ID))
        .hasStatus(HttpStatus.CONFLICT)
        .bodyJson()
        .satisfies(
            json -> assertThat(json).extractingPath("$.code").isEqualTo("RESERVATION_NOT_HELD"));
  }

  /** 누가 결제하는지 없으면 서비스까지 가지 않고 요청 검증이 막는다. 본문 형식은 SeatHoldTest의 400과 같다. */
  @Test
  void 사용자_없이_결제하면_400과_에러_코드를_응답한다() {
    Reservation held = hold(USER_ID);

    assertThat(
            mvc.post()
                .uri("/reservations/{reservationId}/payment", held.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json).extractingPath("$.code").isEqualTo("INVALID_REQUEST");
              assertThat(json).extractingPath("$.detail").asString().contains("userId");
            });
  }

  /** 새 공연의 좌석 하나를 이 사용자로 선점한다. 테스트마다 새 공연이라 서로 좌석이 겹치지 않는다. */
  private Reservation hold(long userId) {
    Stage stage = fixture.createStage(1);
    return reservationService.hold(stage.scheduleId(), stage.seatId(0), userId);
  }

  /**
   * 결제 요청 하나를 보낸다.
   *
   * exchange()로 바로 실행한다. 결과를 버리는 호출(예: 두 번째 결제를 보기 전의 첫 결제)도 실제로 전송되어야 한다.
   */
  private MvcTestResult pay(long reservationId, long userId) {
    return mvc.post()
        .uri("/reservations/{reservationId}/payment", reservationId)
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"userId":%d}
            """
                .formatted(userId))
        .exchange();
  }
}
