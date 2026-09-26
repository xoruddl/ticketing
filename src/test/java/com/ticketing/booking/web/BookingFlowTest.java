package com.ticketing.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * 예매 흐름 전체. 좌석 조회 → 선점 → 결제 → 확정·발권을 API만으로 이어서 거친다.
 *
 * API별 테스트(SeatQueryTest, SeatHoldTest, ReservationPaymentTest, ReservationQueryTest)는 각자 필요한 데이터를
 * 서비스로 미리 만든다. 이 테스트는 그러지 않고, 클라이언트처럼 앞 응답에서 받은 ID만으로 다음 요청을 만든다. 각 API의
 * 응답이 다음 단계에 필요한 것을 실제로 넘겨주는지는 여기서만 보인다.
 *
 * 공연·회차·좌석은 등록 API가 없어(FEATURES.md 범위 밖) 픽스처로 넣는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class BookingFlowTest {

  @Autowired MockMvcTester mvc;
  @Autowired BookingFixture fixture;

  @Test
  void 좌석을_조회하고_선점하고_결제하면_예약이_확정되고_티켓이_발급된다() throws Exception {
    Stage stage = fixture.createStage(1);
    Long userId = BookingFixture.newUserId();

    // 1. 좌석 조회: 예매 가능한 좌석을 고른다.
    MvcTestResult seats =
        mvc.get().uri("/schedules/{scheduleId}/seats", stage.scheduleId()).exchange();
    assertThat(seats).hasStatusOk();
    assertThat(read(seats, "$[0].available", Boolean.class)).isTrue();
    long seatId = read(seats, "$[0].seatId", Long.class);

    // 2. 선점: 고른 좌석을 잡는다. 응답의 예약 ID로 이후 요청을 만든다.
    MvcTestResult held =
        mvc.post()
            .uri("/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"scheduleId":%d,"seatId":%d,"userId":%d}
                """
                    .formatted(stage.scheduleId(), seatId, userId))
            .exchange();
    assertThat(held).hasStatus(HttpStatus.CREATED);
    long reservationId = read(held, "$.reservationId", Long.class);

    // 선점한 좌석은 다른 사용자에게 예매 불가로 보인다.
    assertThat(mvc.get().uri("/schedules/{scheduleId}/seats", stage.scheduleId()))
        .bodyJson()
        .extractingPath("$[0].available")
        .isEqualTo(false);

    // 3. 결제: 예약이 확정되고 티켓이 나온다.
    MvcTestResult paid =
        mvc.post()
            .uri("/reservations/{reservationId}/payment", reservationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"userId":%d}
                """
                    .formatted(userId))
            .exchange();
    assertThat(paid).hasStatusOk();
    assertThat(read(paid, "$.status", String.class)).isEqualTo("CONFIRMED");
    String ticketCode = read(paid, "$.ticketCode", String.class);

    // 4. 예약 조회: 결제 응답에서 받은 것이 다시 조회해도 그대로 남아 있다(확정이 커밋되었다).
    assertThat(mvc.get().uri("/reservations/{reservationId}", reservationId))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json).extractingPath("$.status").isEqualTo("CONFIRMED");
              assertThat(json)
                  .extractingPath("$.payment.amount")
                  .isEqualTo((int) BookingFixture.SEAT_PRICE);
              assertThat(json).extractingPath("$.ticket.code").isEqualTo(ticketCode);
            });

    // 5. 내 예매 목록: 확정된 예약 하나가 보인다.
    assertThat(mvc.get().uri("/reservations").param("userId", userId.toString()))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json).extractingPath("$.length()").isEqualTo(1);
              assertThat(json).extractingPath("$[0].reservationId").isEqualTo((int) reservationId);
              assertThat(json).extractingPath("$[0].status").isEqualTo("CONFIRMED");
            });
  }

  /**
   * 응답 본문에서 JSON 경로의 값을 꺼낸다. 다음 요청에 넘길 ID처럼, 검증이 아니라 값 자체가 필요할 때 쓴다.
   *
   * 한글이 섞인 응답도 깨지지 않게 UTF-8로 읽는다.
   */
  private static <T> T read(MvcTestResult result, String path, Class<T> type)
      throws UnsupportedEncodingException {
    String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    return JsonPath.parse(body).read(path, type);
  }
}
