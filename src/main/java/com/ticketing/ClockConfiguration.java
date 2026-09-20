package com.ticketing;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 현재 시각을 알려주는 시계.
 *
 * 도메인과 서비스가 {@code LocalDateTime.now()}를 직접 부르면 그 코드는 실제 시간에 묶인다. 선점 만료처럼 시각이
 * 판단 기준인 기능은 테스트에서 시간을 기다려야 하고, "만료 1초 전"을 재현할 수 없다. 시계를 빈으로 주입하면
 * 테스트가 {@link Clock#fixed}로 원하는 순간을 고정할 수 있다.
 *
 * 모듈이 아니라 조립 역할인 루트 패키지에 둔다. 어느 업무 모듈의 것도 아닌 기술 설정이다.
 */
@Configuration
public class ClockConfiguration {

  /** 공연과 예매가 일어나는 곳의 시간대. 서버가 어디에 있든 이 시간대로 읽는다. */
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  /**
   * 서울 기준 시계. 서버의 기본 타임존을 쓰지 않고 고정한다.
   *
   * 저장하는 시각이 {@code LocalDateTime}이라 타임존을 가지지 않으므로, 서버 설정을 따르면 개발자 노트북(KST)과
   * 배포 서버(흔히 UTC)가 만든 값이 9시간 어긋난 채 같은 컬럼에 섞인다. 인스턴스를 늘리는 Step 10에서는 인스턴스끼리
   * 어긋난다.
   *
   * "19시 30분 공연"이 서울의 19시 30분인 이상, 시간대는 환경 설정이 아니라 도메인이 정할 값이라고 보았다.
   */
  @Bean
  public Clock clock() {
    return Clock.system(SEOUL);
  }
}
