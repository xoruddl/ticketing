package com.ticketing.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** 좌석 선점 API. 요청부터 DB 저장까지 실제로 거쳐 응답을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class SeatHoldTest {

  private static final long USER_ID = 7L;
  private static final long OTHER_USER_ID = 8L;

  @Autowired MockMvcTester mvc;
  @Autowired BookingFixture fixture;

  @Test
  void 좌석을_선점하면_201과_예약을_응답한다() {
    Stage stage = fixture.createStage(1);

    // 201 application/json, Location: /reservations/1
    // {
    //   "reservationId":1,
    //   "scheduleId":10,
    //   "seatId":100,
    //   "status":"HELD",
    //   "expiresAt":"2026-10-01T19:05:00"
    // }
    assertThat(post(stage.scheduleId(), stage.seatId(0), USER_ID))
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .satisfies(
            json -> {
              // ID는 DB가 매기므로 값이 아니라 채워졌는지만 본다.
              assertThat(json).extractingPath("$.reservationId").isNotNull();
              assertThat(json)
                  .extractingPath("$.scheduleId")
                  .isEqualTo(stage.scheduleId().intValue());
              assertThat(json).extractingPath("$.seatId").isEqualTo(stage.seatId(0).intValue());
              // 선점 직후라 항상 HELD다.
              assertThat(json).extractingPath("$.status").isEqualTo("HELD");
              // 만료 시각이 없으면 클라이언트가 남은 시간을 보여줄 수 없다.
              assertThat(json).extractingPath("$.expiresAt").isNotNull();
            });
  }

  /** 새 자원을 만들었으므로 어디서 찾는지 알려준다. 조회 API는 아직 없다. */
  @Test
  void 선점_응답은_예약_위치를_알려준다() {
    Stage stage = fixture.createStage(1);

    assertThat(post(stage.scheduleId(), stage.seatId(0), USER_ID))
        .hasStatus(HttpStatus.CREATED)
        .headers()
        .satisfies(
            headers -> assertThat(headers.getLocation()).asString().startsWith("/reservations/"));
  }

  @Test
  void 이미_선점된_좌석은_409를_응답한다() {
    Stage stage = fixture.createStage(1);
    post(stage.scheduleId(), stage.seatId(0), USER_ID);

    // 409 application/problem+json
    // {"detail":"이미 팔린 좌석이다:
    // 100","instance":"/reservations","status":409,"title":"Conflict","code":"SEAT_ALREADY_TAKEN"}
    assertThat(post(stage.scheduleId(), stage.seatId(0), OTHER_USER_ID))
        .hasStatus(HttpStatus.CONFLICT)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .satisfies(
            json -> {
              assertThat(json).extractingPath("$.code").isEqualTo("SEAT_ALREADY_TAKEN");
              assertThat(json)
                  .extractingPath("$.detail")
                  .isEqualTo("이미 팔린 좌석이다: " + stage.seatId(0));
            });
  }

  @Test
  void 없는_회차를_선점하면_404를_응답한다() {
    Stage stage = fixture.createStage(1);
    long missingScheduleId = 0L;

    assertThat(post(missingScheduleId, stage.seatId(0), USER_ID))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .satisfies(
            json -> assertThat(json).extractingPath("$.code").isEqualTo("SCHEDULE_NOT_FOUND"));
  }

  @Test
  void 없는_좌석을_선점하면_404를_응답한다() {
    Stage stage = fixture.createStage(1);
    long missingSeatId = 0L;

    assertThat(post(stage.scheduleId(), missingSeatId, USER_ID))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .satisfies(json -> assertThat(json).extractingPath("$.code").isEqualTo("SEAT_NOT_FOUND"));
  }

  /**
   * 값이 비었는지는 서비스까지 가지 않고 요청 검증이 막는다.
   *
   * 이 400에는 본문이 없다. 다른 거절은 ProblemDetail에 code를 실어 보내는데 검증 실패만 빈 응답이라, 받는 쪽이
   * 무엇이 잘못됐는지 알 수 없다. 상태 코드만 확인해 두고 응답 형식은 따로 맞춘다.
   */
  @Test
  void 좌석_없이_선점하면_400을_응답한다() {
    Stage stage = fixture.createStage(1);
    String body =
        """
        {"scheduleId":%d,"userId":%d}
        """
            .formatted(stage.scheduleId(), USER_ID);

    assertThat(
            mvc.post().uri("/reservations").contentType(MediaType.APPLICATION_JSON).content(body))
        .hasStatus(HttpStatus.BAD_REQUEST);
  }

  /**
   * 선점 요청 하나를 보낸다. 본문 모양이 테스트마다 같아 여기로 모은다.
   *
   * exchange()로 요청을 바로 실행한다. 빌더만 돌려주면, 결과를 검증하지 않고 버리는 호출(예: 409를 보기 전에
   * 좌석을 미리 선점해두는 요청)이 실제로 전송되지 않는다.
   */
  private MvcTestResult post(long scheduleId, long seatId, long userId) {
    String body =
        """
        {"scheduleId":%d,"seatId":%d,"userId":%d}
        """
            .formatted(scheduleId, seatId, userId);
    return mvc.post()
        .uri("/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }
}
