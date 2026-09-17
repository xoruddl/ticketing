package com.ticketing.booking.web;

import com.ticketing.booking.domain.ScheduleNotFoundException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * booking 도메인 예외를 HTTP 응답으로 바꾼다. 도메인 예외는 HTTP를 모르므로, 예외 → BookingErrorCode 매핑을 여기서 한다.
 *
 * 응답은 RFC 9457 ProblemDetail 형식이다. 예: GET /schedules/999/seats
 *
 * <pre>
 * 404 application/problem+json
 * {"detail":"회차를 찾을 수 없다: 999","instance":"/schedules/999/seats","status":404,"title":"Not Found","code":"SCHEDULE_NOT_FOUND"}
 * </pre>
 *
 * 핸들러가 채우는 것은 status·detail·code뿐이다. title은 상태 코드의 기본 문구로, instance는 Spring이 요청 경로로
 * 채운다. type은 설정하지 않아 응답에서 빠진다.
 */
@RestControllerAdvice
public class BookingExceptionHandler {

  @ExceptionHandler(ScheduleNotFoundException.class)
  public ProblemDetail handle(ScheduleNotFoundException e) {
    return problem(BookingErrorCode.SCHEDULE_NOT_FOUND, e.getScheduleId());
  }

  /**
   * 에러 코드의 공통 메시지 뒤에 요청별 값(예: 회차 ID)을 붙인다.
   *
   * code 속성은 클라이언트가 detail 문구를 파싱하지 않고 에러 종류를 구분할 수 있게 넣는다. 문구는 바뀔 수 있지만 코드 이름은 API 약속이다.
   */
  private ProblemDetail problem(BookingErrorCode code, Object value) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(code.getStatus(), code.getMessage() + ": " + value);
    problem.setProperty("code", code.name());
    return problem;
  }
}
