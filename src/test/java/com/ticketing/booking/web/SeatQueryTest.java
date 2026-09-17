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
}
