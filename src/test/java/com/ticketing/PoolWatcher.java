package com.ticketing;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import javax.sql.DataSource;

/**
 * 요청이 몰리는 동안 커넥션 풀을 지켜보고, 사용 중·대기 수의 최댓값과 커넥션을 쥐고·기다린 시간을 남긴다.
 *
 * 동시 요청은 수십 ms에 끝나서, 끝난 뒤 한 번 읽으면 이미 0이다. 그래서 요청이 도는 동안 백그라운드 스레드가
 * 짧은 간격으로 계속 읽어 최댓값을 잡는다. 순간값은 캐시가 없는 HikariPoolMXBean으로 읽는다. Micrometer 게이지는
 * 1초 캐시라 이 용도로 못 쓴다 (ConnectionPoolMetricsTest 참고).
 *
 * 커넥션을 쥔 시간·기다린 시간은 샘플링으로 잴 수 없다. 반납·획득할 때마다 기록되는 Micrometer 타이머를 지켜보기
 * 전후로 읽어 그 사이의 몫만 뗀다.
 *
 * 쓰는 법:
 *
 *   PoolWatcher watcher = PoolWatcher.start(dataSource, registry);
 *   ... 요청을 보낸다 ...
 *   PoolReport report = watcher.stop();
 */
public final class PoolWatcher {

  /** 읽는 간격. 요청 하나가 몇 ms 걸리므로 그보다 훨씬 촘촘하게 읽는다. */
  private static final long INTERVAL_NANOS = TimeUnit.MICROSECONDS.toNanos(100);

  private final HikariPoolMXBean pool;
  private final int poolSize;
  private final Timer usage;
  private final Timer acquire;
  private final TimerSnapshot usageBefore;
  private final TimerSnapshot acquireBefore;
  private final Thread sampler;

  private volatile boolean running = true;

  // 샘플링 스레드만 쓰고, stop()은 그 스레드가 끝난 뒤(join) 읽으므로 volatile이 필요 없다.
  private int maxActive;
  private int maxPending;
  private long samples;

  private PoolWatcher(HikariDataSource hikari, MeterRegistry registry) {
    this.pool = hikari.getHikariPoolMXBean();
    this.poolSize = hikari.getMaximumPoolSize();
    this.usage = registry.get("hikaricp.connections.usage").timer();
    this.acquire = registry.get("hikaricp.connections.acquire").timer();
    this.usageBefore = TimerSnapshot.of(usage);
    this.acquireBefore = TimerSnapshot.of(acquire);
    this.sampler = Thread.ofPlatform().name("pool-watcher").daemon().start(this::sample);
  }

  /** 지금부터 지켜본다. */
  public static PoolWatcher start(DataSource dataSource, MeterRegistry registry)
      throws SQLException {
    return new PoolWatcher(dataSource.unwrap(HikariDataSource.class), registry);
  }

  /** 지켜보기를 멈추고, 시작부터 지금까지 본 것을 돌려준다. */
  public PoolReport stop() throws InterruptedException {
    running = false;
    sampler.join();
    TimerSnapshot usageDelta = TimerSnapshot.of(usage).minus(usageBefore);
    TimerSnapshot acquireDelta = TimerSnapshot.of(acquire).minus(acquireBefore);
    return new PoolReport(
        poolSize,
        maxActive,
        maxPending,
        samples,
        acquireDelta.count(),
        usageDelta.meanMs(),
        usage.max(TimeUnit.MILLISECONDS),
        acquireDelta.meanMs(),
        acquire.max(TimeUnit.MILLISECONDS));
  }

  private void sample() {
    while (running) {
      maxActive = Math.max(maxActive, pool.getActiveConnections());
      maxPending = Math.max(maxPending, pool.getThreadsAwaitingConnection());
      samples++;
      LockSupport.parkNanos(INTERVAL_NANOS);
    }
  }

  /**
   * 지켜본 결과.
   *
   * @param poolSize 풀 최대 크기. maxActive가 이 값에 닿으면 풀이 바닥난 것이다
   * @param maxActive 동시에 빌려 간 커넥션 수의 최댓값
   * @param maxPending 커넥션을 기다린 스레드 수의 최댓값. 0보다 크면 풀이 모자랐다
   * @param samples 읽은 횟수. 너무 적으면 최댓값을 놓쳤을 수 있다
   * @param borrowed 지켜보는 동안 커넥션을 빌린 횟수
   * @param meanUsageMs 커넥션을 빌려서 반납할 때까지 쥔 평균 시간
   * @param maxUsageMs 쥔 시간의 최댓값. Micrometer 타이머의 최근 몇 분 최댓값이라 지켜보기 전 기록이 섞일 수 있다
   * @param meanAcquireMs 커넥션을 달라고 해서 받을 때까지 기다린 평균 시간
   * @param maxAcquireMs 기다린 시간의 최댓값. maxUsageMs와 같은 이유로 지켜보기 전 기록이 섞일 수 있다
   */
  public record PoolReport(
      int poolSize,
      int maxActive,
      int maxPending,
      long samples,
      long borrowed,
      double meanUsageMs,
      double maxUsageMs,
      double meanAcquireMs,
      double maxAcquireMs) {

    @Override
    public String toString() {
      return String.format(
          "풀 %d개 중 최대 사용 %d, 최대 대기 %d (샘플 %d회) | 빌림 %d회"
              + " | 쥔 시간 평균 %.1fms 최대 %.1fms | 기다린 시간 평균 %.1fms 최대 %.1fms",
          poolSize,
          maxActive,
          maxPending,
          samples,
          borrowed,
          meanUsageMs,
          maxUsageMs,
          meanAcquireMs,
          maxAcquireMs);
    }
  }

  /** 타이머의 누적값. 두 시점의 차이로 그 사이의 몫만 뗀다. */
  private record TimerSnapshot(long count, double totalMs) {

    static TimerSnapshot of(Timer timer) {
      return new TimerSnapshot(timer.count(), timer.totalTime(TimeUnit.MILLISECONDS));
    }

    TimerSnapshot minus(TimerSnapshot before) {
      return new TimerSnapshot(count - before.count, totalMs - before.totalMs);
    }

    double meanMs() {
      return count == 0 ? 0 : totalMs / count;
    }
  }
}
