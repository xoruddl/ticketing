package com.ticketing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 식별자를 붙여 로그 한 줄이 어느 요청에서 나왔는지 가를 수 있게 한다.
 *
 * 동시 요청 N건이 같은 좌석을 두드리면 로그가 뒤섞인다. 스레드 이름만으로는 부족하다 — 톰캣은 스레드를 재사용하므로
 * 같은 스레드 이름이 여러 요청에 걸쳐 나온다. 요청 ID를 MDC에 넣어 두면 로그 패턴이 줄마다 찍어 준다.
 *
 * 특정 업무 모듈의 것이 아니라 모든 요청에 걸리는 기술 장치라, 조립 역할인 루트 패키지에 둔다.
 */
@Component
// 다른 필터가 남기는 로그에도 ID가 찍히도록 가장 먼저 돈다.
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  /** MDC 키. 로그 패턴에서 {@code %X{requestId}}로 꺼낸다. */
  static final String MDC_KEY = "requestId";

  /** 응답 헤더. 클라이언트가 받은 ID로 서버 로그를 찾을 수 있다. */
  static final String HEADER = "X-Request-Id";

  /** 요청 ID 길이. UUID 36자는 로그 한 줄을 너무 차지한다. 예: {@code 3f9a1c2e} */
  private static final int ID_LENGTH = 8;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String requestId = newRequestId();
    MDC.put(MDC_KEY, requestId);
    response.setHeader(HEADER, requestId);
    try {
      chain.doFilter(request, response);
    } finally {
      // 스레드가 풀로 돌아가 다음 요청을 받을 때 앞 요청의 ID가 남아 있으면 안 된다.
      MDC.remove(MDC_KEY);
    }
  }

  /**
   * 클라이언트가 보낸 헤더를 쓰지 않고 서버가 매번 새로 만든다. 받은 값을 그대로 쓰면 같은 ID를 여러 요청에 보내
   * 로그를 섞을 수 있다.
   *
   * 8자(32비트)는 전역으로 유일하지 않다. 한 테스트·한 시간대의 로그 안에서 요청을 가르는 용도로는 충분하다고 보았다.
   */
  private String newRequestId() {
    return UUID.randomUUID().toString().substring(0, ID_LENGTH);
  }
}
