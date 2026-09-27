package com.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * 커넥션 풀(HikariCP)을 무엇으로 읽을 수 있는지 확인한다. 동시 요청 테스트가 풀을 관찰하는 방법의 전제다.
 *
 * 읽는 곳은 셋이고, 정확도가 다르다.
 *
 * 1. HikariPoolMXBean — 사용 중·대기 수를 읽는 순간의 값으로 준다. 수십 ms에 끝나는 동시 요청의 순간 최댓값은
 *    이걸로 잰다.
 * 2. Micrometer 게이지(hikaricp.connections.active·pending) — 같은 수지만 HikariCP가 최대 1초 캐시한다.
 *    /actuator/metrics로 운행 중에 보는 값이 이것이다. 몇 초 이상 이어지는 부하를 보기엔 충분하다.
 * 3. Micrometer 타이머(hikaricp.connections.usage·acquire) — 커넥션을 쥔 시간과 얻기까지 기다린 시간.
 *    반납·획득할 때마다 기록돼 캐시가 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConnectionPoolMetricsTest {

  /** 지금 빌려 가서 쓰고 있는 커넥션 수. */
  private static final String ACTIVE = "hikaricp.connections.active";

  /** 커넥션을 빌린 뒤 반납할 때까지 쥐고 있던 시간. 반납할 때 기록된다. */
  private static final String USAGE = "hikaricp.connections.usage";

  /** 커넥션을 달라고 한 뒤 받을 때까지 걸린 시간. 풀이 바닥나면 길어진다. */
  private static final String ACQUIRE = "hikaricp.connections.acquire";

  @Autowired MeterRegistry registry;
  @Autowired DataSource dataSource;
  @Autowired MockMvcTester mvc;

  HikariDataSource hikari;
  HikariPoolMXBean pool;

  @BeforeEach
  void setUp() throws Exception {
    hikari = dataSource.unwrap(HikariDataSource.class);
    pool = hikari.getHikariPoolMXBean();
  }

  @Test
  void MXBean은_커넥션을_빌리고_반납하는_즉시_사용_중_수가_바뀐다() throws Exception {
    int before = pool.getActiveConnections();

    try (Connection held = dataSource.getConnection()) {
      assertThat(pool.getActiveConnections()).isEqualTo(before + 1);
    }

    assertThat(pool.getActiveConnections()).isEqualTo(before);
  }

  /**
   * 풀을 바닥낸 뒤 한 스레드를 더 보내면, 그 스레드는 커넥션이 반납될 때까지 대기 수에 잡힌다. 동시 요청이 풀 크기를
   * 넘으면 이 수가 늘어난다.
   */
  @Test
  void 풀이_바닥나면_MXBean_대기_수가_늘어난다() throws Exception {
    List<Connection> all = borrowAll();
    try {
      CompletableFuture<Connection> waiting = CompletableFuture.supplyAsync(this::borrow);
      awaitWaitingThreads(1);

      all.removeFirst().close();
      waiting.get(5, TimeUnit.SECONDS).close();
      assertThat(pool.getThreadsAwaitingConnection()).isZero();
    } finally {
      for (Connection connection : all) {
        connection.close();
      }
    }
  }

  /** 끝난 뒤가 아니라 지켜보는 도중의 최댓값을 남기는지 본다. 끝난 시점에는 사용 중·대기 수가 이미 돌아와 있다. */
  @Test
  void PoolWatcher는_지켜보는_동안의_사용_중_대기_최댓값을_남긴다() throws Exception {
    PoolWatcher watcher = PoolWatcher.start(dataSource, registry);
    List<Connection> all = borrowAll();
    try {
      CompletableFuture<Connection> waiting = CompletableFuture.supplyAsync(this::borrow);
      awaitWaitingThreads(1);
      // 대기가 생긴 걸 이 스레드가 먼저 봤을 수 있다. 샘플링 스레드도 읽을 틈을 준다.
      Thread.sleep(50);

      all.removeFirst().close();
      waiting.get(5, TimeUnit.SECONDS).close();
    } finally {
      for (Connection connection : all) {
        connection.close();
      }
    }
    // 다 반납한 뒤에도 몇 번 더 읽게 둔다. 마지막에 읽은 값이 아니라 최댓값을 남기는지 가른다.
    Thread.sleep(50);
    PoolWatcher.PoolReport report = watcher.stop();

    assertThat(report.maxActive()).isEqualTo(hikari.getMaximumPoolSize());
    assertThat(report.maxPending()).isEqualTo(1);
    assertThat(report.borrowed()).isEqualTo(hikari.getMaximumPoolSize() + 1);
    assertThat(pool.getActiveConnections()).isLessThan(report.maxActive());
  }

  /**
   * 게이지를 한 번 읽으면 HikariCP가 풀 상태를 1초 동안 캐시한다. 그 사이에 커넥션을 빌려도 게이지는 옛값을 준다.
   * 동시 요청 테스트에서 게이지로 순간값을 샘플링하지 않는 이유다.
   */
  @Test
  void Micrometer_게이지는_1초_안의_변화를_보여주지_않는다() throws Exception {
    double cached = activeGauge();

    try (Connection held = dataSource.getConnection()) {
      assertThat(pool.getActiveConnections()).isEqualTo((int) cached + 1);
      assertThat(activeGauge()).isEqualTo(cached);
    }
  }

  @Test
  void 커넥션을_쥔_시간이_반납할_때_타이머에_기록된다() throws Exception {
    Timer usage = registry.get(USAGE).timer();
    long countBefore = usage.count();

    try (Connection held = dataSource.getConnection()) {
      Thread.sleep(200);
    }

    assertThat(usage.count()).isEqualTo(countBefore + 1);
    assertThat(usage.max(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(200);
  }

  @Test
  void 커넥션을_얻을_때마다_획득_타이머에_기록된다() throws Exception {
    Timer acquire = registry.get(ACQUIRE).timer();
    long countBefore = acquire.count();

    try (Connection held = dataSource.getConnection()) {
      assertThat(acquire.count()).isEqualTo(countBefore + 1);
    }
  }

  @Test
  void 커넥션_풀_지표를_HTTP로_읽을_수_있다() {
    assertThat(mvc.get().uri("/actuator/metrics/{name}", ACTIVE))
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.name")
        .isEqualTo(ACTIVE);
  }

  /** 설정·환경 변수를 드러내는 엔드포인트는 열지 않았다. */
  @Test
  void 열지_않은_엔드포인트는_404다() {
    assertThat(mvc.get().uri("/actuator/env")).hasStatus(HttpStatus.NOT_FOUND);
  }

  private double activeGauge() {
    return registry.get(ACTIVE).gauge().value();
  }

  /** 풀 최대 크기만큼 커넥션을 빌려 쥔다. 빌린 커넥션은 호출한 쪽이 반납한다. */
  private List<Connection> borrowAll() throws Exception {
    List<Connection> connections = new ArrayList<>();
    for (int i = 0; i < hikari.getMaximumPoolSize(); i++) {
      connections.add(dataSource.getConnection());
    }
    return connections;
  }

  private Connection borrow() {
    try {
      return dataSource.getConnection();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /** 다른 스레드가 대기열에 들어설 때까지 잠깐씩 확인한다. 5초 안에 안 들어서면 실패한다. */
  private void awaitWaitingThreads(int expected) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (pool.getThreadsAwaitingConnection() < expected && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertThat(pool.getThreadsAwaitingConnection()).isEqualTo(expected);
  }
}
