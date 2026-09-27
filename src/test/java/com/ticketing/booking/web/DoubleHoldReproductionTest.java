package com.ticketing.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketing.TestcontainersConfiguration;
import com.ticketing.booking.BookingFixture;
import com.ticketing.booking.BookingFixture.Stage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * 같은 회차의 같은 좌석에 선점 요청을 동시에 보낸다. 한 건만 성공해야 한다.
 *
 * MockMvc가 아니라 실제 포트로 띄운 서버에 HTTP로 보낸다. 요청마다 톰캣 스레드가 따로 붙고 RequestIdFilter를
 * 거치므로, 운영에서 요청이 몰릴 때와 같은 경로로 돈다.
 *
 * 이 테스트는 아직 막지 않은 문제를 드러내려는 것이라 실패가 정상이다. 기본 test에서 빠지고
 * {@code ./gradlew reproductionTest}로만 돈다. 막는 Step에서 태그를 뗀다.
 */
@Tag("reproduction")
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    // 요청마다 어떤 SQL이 어떤 순서로 나갔는지 보려고 켠다. 로그 줄마다 요청 ID가 붙는다.
    properties = "logging.level.org.hibernate.SQL=DEBUG")
@Import({TestcontainersConfiguration.class, BookingFixture.class})
class DoubleHoldReproductionTest {

  private static final Logger log = LoggerFactory.getLogger(DoubleHoldReproductionTest.class);

  /**
   * 동시에 보낼 요청 수.
   *
   * 커넥션 풀 기본 크기(10)를 넘기지 않는다. 넘기면 일부 요청은 커넥션을 기다리느라 늦게 출발해, 동시에 부딪히는
   * 요청이 줄어든다.
   */
  private static final int REQUESTS = 10;

  @LocalServerPort int port;
  @Autowired BookingFixture fixture;
  @Autowired JdbcTemplate jdbc;

  @Test
  void 같은_좌석에_동시에_선점하면_한_건만_성공한다() throws Exception {
    Stage stage = fixture.createStage(1);
    RestClient client = RestClient.create("http://localhost:" + port);

    List<Result> results = sendAtOnce(client, stage);
    List<Map<String, Object>> rows = reservationsOf(stage);
    report(results, rows);

    assertThat(results).filteredOn(Result::created).hasSize(1);
    assertThat(rows).hasSize(1);
  }

  /**
   * 스레드 {@value #REQUESTS}개를 출발선에 세워 두고 한꺼번에 출발시킨다.
   *
   * 스레드를 만드는 대로 바로 보내면 먼저 만든 스레드가 앞서 나가 요청이 조금씩 어긋난다. 모두 준비된 뒤 한 번에
   * 풀어야 요청이 실제로 겹친다.
   */
  private List<Result> sendAtOnce(RestClient client, Stage stage) throws Exception {
    CountDownLatch ready = new CountDownLatch(REQUESTS);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Result>> futures = new ArrayList<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(REQUESTS)) {
      for (int i = 0; i < REQUESTS; i++) {
        // 사용자마다 다른 ID로 보낸다. 서로 다른 사람이 같은 좌석을 노리는 상황이다.
        Long userId = BookingFixture.newUserId();
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return hold(client, stage, userId);
                }));
      }
      ready.await();
      start.countDown();

      List<Result> results = new ArrayList<>();
      for (Future<Result> future : futures) {
        results.add(future.get());
      }
      return results;
    }
  }

  /** 선점 요청 하나. 4xx·5xx도 예외로 던지지 않고 상태 코드로 돌려받는다. */
  private Result hold(RestClient client, Stage stage, Long userId) {
    return client
        .post()
        .uri("/reservations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("scheduleId", stage.scheduleId(), "seatId", stage.seatId(0), "userId", userId))
        .exchange(
            (request, response) ->
                new Result(
                    userId,
                    response.getStatusCode().value(),
                    response.getHeaders().getFirst("X-Request-Id")));
  }

  /** 이 좌석에 남은 예약 행. 엔티티에 없는 created_at까지 보려고 테이블을 직접 읽는다. */
  private List<Map<String, Object>> reservationsOf(Stage stage) {
    return jdbc.queryForList(
        "select id, user_id, status, created_at from reservation"
            + " where schedule_id = ? and seat_id = ? order by created_at",
        stage.scheduleId(),
        stage.seatId(0));
  }

  /** 응답과 DB에 남은 것을 로그로 남긴다. 요청 ID로 위쪽 SQL 로그와 맞춰 볼 수 있다. */
  private void report(List<Result> results, List<Map<String, Object>> rows) {
    log.info("=== 응답 {}건 ===", results.size());
    results.forEach(r -> log.info("요청 {} 사용자 {} -> {}", r.requestId(), r.userId(), r.status()));

    log.info("=== 남은 예약 행 {}건 ===", rows.size());
    rows.forEach(row -> log.info("{}", row));
    if (rows.size() > 1) {
      LocalDateTime first = (LocalDateTime) rows.getFirst().get("created_at");
      LocalDateTime last = (LocalDateTime) rows.getLast().get("created_at");
      log.info("첫 행과 마지막 행의 간격: {} µs", Duration.between(first, last).toNanos() / 1_000);
    }
  }

  /** 요청 하나의 결과. 201이면 선점에 성공한 것이다. */
  private record Result(Long userId, int status, String requestId) {
    boolean created() {
      return status == 201;
    }
  }
}
