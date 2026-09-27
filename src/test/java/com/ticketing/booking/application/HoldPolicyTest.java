package com.ticketing.booking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class HoldPolicyTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 19, 0);

  /** 시계를 고정하면 "지금"이 언제인지 테스트가 정한다. 실제 시간을 기다릴 필요가 없다. */
  private static final Clock FIXED = Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);

  @Test
  void 선점은_유효_시간_뒤에_만료된다() {
    HoldPolicy policy = new HoldPolicy(FIXED, Duration.ofMinutes(5));

    assertThat(policy.expiresAt()).isEqualTo(NOW.plusMinutes(5));
  }

  @Test
  void 유효_시간을_바꾸면_만료_시각도_따라_바뀐다() {
    HoldPolicy policy = new HoldPolicy(FIXED, Duration.ofMinutes(10));

    assertThat(policy.expiresAt()).isEqualTo(NOW.plusMinutes(10));
  }

  /** 0이면 선점하자마자 만료된다. 설정 실수를 기동할 때 잡는다. */
  @Test
  void 유효_시간이_0이면_만들_수_없다() {
    assertThatThrownBy(() -> new HoldPolicy(FIXED, Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 유효_시간이_음수면_만들_수_없다() {
    assertThatThrownBy(() -> new HoldPolicy(FIXED, Duration.ofMinutes(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
