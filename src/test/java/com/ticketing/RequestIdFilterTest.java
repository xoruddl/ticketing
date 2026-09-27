package com.ticketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 요청마다 식별자를 붙이는 필터. 서블릿 목 객체로 필터 하나만 돌려 본다 (Docker 없이 돈다). */
class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void 요청을_처리하는_동안_MDC에_요청_ID가_있다() throws Exception {
    AtomicReference<String> seen = new AtomicReference<>();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        new MockHttpServletRequest(),
        response,
        (req, res) -> seen.set(MDC.get(RequestIdFilter.MDC_KEY)));

    assertThat(seen.get()).isNotBlank();
    // 클라이언트가 받은 응답의 ID로 서버 로그를 찾을 수 있어야 한다.
    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seen.get());
  }

  /** 동시 요청 N건의 로그를 가르는 것이 목적이라, ID가 겹치면 쓸모가 없다. */
  @Test
  void 요청마다_다른_ID를_붙인다() throws Exception {
    assertThat(requestIdOfOneRequest()).isNotEqualTo(requestIdOfOneRequest());
  }

  /** 톰캣은 스레드를 재사용한다. 지우지 않으면 다음 요청의 로그에 앞 요청의 ID가 찍힌다. */
  @Test
  void 요청이_끝나면_MDC에서_지운다() throws Exception {
    requestIdOfOneRequest();

    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void 처리_중_예외가_나도_MDC에서_지운다() {
    FilterChain failing =
        (req, res) -> {
          throw new IllegalStateException("처리 실패");
        };

    assertThatThrownBy(
            () ->
                filter.doFilter(
                    new MockHttpServletRequest(), new MockHttpServletResponse(), failing))
        .isInstanceOf(IllegalStateException.class);
    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  private String requestIdOfOneRequest() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {});
    return response.getHeader(RequestIdFilter.HEADER);
  }
}
