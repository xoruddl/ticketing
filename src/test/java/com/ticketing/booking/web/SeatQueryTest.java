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

/** 회차 좌석 조회 API. 요청부터 DB 조회까지 실제로 거쳐 응답 JSON을 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class SeatQueryTest {

  @Autowired MockMvcTester mvc;
  @Autowired BookingFixture fixture;

  @Test
  void 회차의_좌석_목록을_조회한다() {
    Stage stage = fixture.createStage(3);

    // 응답은 좌석 객체의 배열이다. 루트가 배열이라 경로가 $[0].seatId 모양이 된다.
    // seatId는 DB가 매기므로 실행마다 다르다. 그래서 숫자 대신 stage.seatId(n)과 비교한다.
    // 200 application/json
    // [
    //
    // {"seatId":101,"section":"A","rowName":"1","seatNumber":1,"grade":"VIP","price":150000,"available":true},
    //
    // {"seatId":102,"section":"A","rowName":"1","seatNumber":2,"grade":"VIP","price":150000,"available":true},
    //
    // {"seatId":103,"section":"A","rowName":"1","seatNumber":3,"grade":"VIP","price":150000,"available":true}
    // ]
    assertThat(mvc.get().uri("/schedules/{scheduleId}/seats", stage.scheduleId()))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              // 픽스처가 만든 좌석 3개가 모두, 저장된 순서(id 순)대로 나온다.
              assertThat(json).extractingPath("$.length()").isEqualTo(3);
              assertThat(json).extractingPath("$[0].seatId").isEqualTo(stage.seatId(0).intValue());
              assertThat(json).extractingPath("$[2].seatId").isEqualTo(stage.seatId(2).intValue());
              // 위치는 SeatPosition을 풀어서 section·rowName·seatNumber로 내려준다.
              assertThat(json).extractingPath("$[0].section").isEqualTo("A");
              assertThat(json).extractingPath("$[0].rowName").isEqualTo("1");
              assertThat(json).extractingPath("$[0].seatNumber").isEqualTo(1);
              assertThat(json).extractingPath("$[0].grade").isEqualTo("VIP");
              assertThat(json)
                  .extractingPath("$[0].price")
                  .isEqualTo((int) BookingFixture.SEAT_PRICE);
            });
  }

  /**
   * 선점된 좌석은 목록에 남아 있되 예매 불가로 표시된다.
   *
   * 좌석을 목록에서 빼지 않는 이유: 화면은 좌석 배치도를 그대로 그리고 팔린 자리만 회색으로 칠한다. 빠지면 배치도에 구멍이
   * 생긴다.
   */
  @Test
  void 선점된_좌석은_예매_불가로_응답한다() {
    Stage stage = fixture.createStage(2);
    // 저장소로 예약을 직접 넣지 않고 선점 API를 거친다. 사용자가 실제로 겪는 순서(선점 → 목록 새로고침) 그대로 본다.
    holdSeat(stage.scheduleId(), stage.seatId(0));

    // 200 application/json
    // [
    //
    // {"seatId":101,"section":"A","rowName":"1","seatNumber":1,"grade":"VIP","price":150000,"available":false},
    //
    // {"seatId":102,"section":"A","rowName":"1","seatNumber":2,"grade":"VIP","price":150000,"available":true}
    // ]
    assertThat(mvc.get().uri("/schedules/{scheduleId}/seats", stage.scheduleId()))
        .hasStatusOk()
        .bodyJson()
        .satisfies(
            json -> {
              // 선점된 좌석도 목록에서 빠지지 않는다.
              assertThat(json).extractingPath("$.length()").isEqualTo(2);
              assertThat(json).extractingPath("$[0].available").isEqualTo(false);
              // 선점하지 않은 옆 좌석은 그대로 예매할 수 있다.
              assertThat(json).extractingPath("$[1].available").isEqualTo(true);
            });
  }

  @Test
  void 없는_회차의_좌석을_조회하면_404를_응답한다() {
    // id는 IDENTITY로 1부터 매겨지므로 0번 회차는 항상 없다.
    long missingScheduleId = 0L;

    // 응답은 ProblemDetail 객체 하나다. 루트가 객체라 경로가 $.code 모양이 된다.
    // code는 ProblemDetail의 properties 맵에 들어 있지만, 직렬화할 때 최상위 필드로 풀려 나온다.
    // 필드 순서는 의미가 없다. JSON 객체는 이름으로 값을 찾는다.
    // 404 application/problem+json
    // {
    //   "detail":"회차를 찾을 수 없다: 0",
    //   "instance":"/schedules/0/seats",
    //   "status":404,
    //   "title":"Not Found",
    //   "code":"SCHEDULE_NOT_FOUND"
    // }
    assertThat(mvc.get().uri("/schedules/{scheduleId}/seats", missingScheduleId))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .satisfies(
            json -> {
              // 클라이언트는 문구가 아니라 code로 에러 종류를 구분한다.
              assertThat(json).extractingPath("$.code").isEqualTo("SCHEDULE_NOT_FOUND");
              assertThat(json).extractingPath("$.detail").isEqualTo("회차를 찾을 수 없다: 0");
              // instance는 핸들러가 채우지 않아도 Spring이 요청 경로로 채운다.
              assertThat(json).extractingPath("$.instance").isEqualTo("/schedules/0/seats");
            });
  }

  /**
   * 선점 API로 좌석 하나를 선점한다. 누가 선점했는지는 이 테스트의 관심이 아니라 사용자 ID는 고정한다.
   *
   * exchange()로 바로 실행하고, 201인지 확인해 둔다. 선점이 조용히 실패하면 아래 단언이 엉뚱한 이유로 깨지기 때문이다.
   */
  private void holdSeat(long scheduleId, long seatId) {
    String body =
        """
        {"scheduleId":%d,"seatId":%d,"userId":7}
        """
            .formatted(scheduleId, seatId);
    assertThat(
            mvc.post()
                .uri("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange())
        .hasStatus(HttpStatus.CREATED);
  }
}
