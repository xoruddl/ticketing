package com.ticketing.booking.application;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 선점을 언제까지 유효하게 볼 것인가. 시계와 설정값을 한곳에 모은다.
 *
 * 선점 유효 시간은 설정으로 두고(FEATURES.md F2), 지금 몇 시인지는 주입받은 시계가 정한다. 이 둘을 서비스가 직접
 * 들고 있으면 선점·결제·만료가 각자 시각을 계산하게 되고, Step 3에서 만료를 다룰 때 고칠 자리가 흩어진다.
 *
 * 도메인({@link com.ticketing.booking.domain.Reservation})이 아니라 application에 둔 것은 스프링의
 * {@code @Value}에 기대기 때문이다. 도메인은 설정도 프레임워크도 모르는 채로 둔다.
 */
@Component
public class HoldPolicy {

  private final Clock clock;

  /** 선점 유효 시간. 예: 5m */
  private final Duration duration;

  /** 유효 시간이 0 이하면 선점하자마자 만료된다. 설정 실수를 기동할 때 잡는다. */
  public HoldPolicy(Clock clock, @Value("${booking.hold-duration}") Duration duration) {
    if (duration.isZero() || duration.isNegative()) {
      throw new IllegalArgumentException("선점 유효 시간은 0보다 커야 한다: " + duration);
    }
    this.clock = clock;
    this.duration = duration;
  }

  /** 지금 선점한다면 언제 만료되는가. 이 값이 예약의 expiresAt 컬럼에 그대로 박힌다. */
  public LocalDateTime expiresAt() {
    return LocalDateTime.now(clock).plus(duration);
  }
}
