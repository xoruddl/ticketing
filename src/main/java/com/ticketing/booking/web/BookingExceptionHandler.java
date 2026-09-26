package com.ticketing.booking.web;

import com.ticketing.booking.domain.ReservationExpiredException;
import com.ticketing.booking.domain.ReservationNotFoundException;
import com.ticketing.booking.domain.ReservationNotHeldException;
import com.ticketing.booking.domain.ReservationNotOwnedException;
import com.ticketing.booking.domain.ScheduleNotFoundException;
import com.ticketing.booking.domain.SeatAlreadyTakenException;
import com.ticketing.booking.domain.SeatNotFoundException;
import java.util.List;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * booking 도메인 예외를 HTTP 응답으로 바꾼다. 도메인 예외는 HTTP를 모르므로, 예외 → BookingErrorCode 매핑을 여기서 한다.
 *
 * 응답은 RFC 9457 ProblemDetail 형식이다. 예: GET /schedules/999/seats
 *
 * 404 application/problem+json
 * {"detail":"회차를 찾을 수 없다: 999","instance":"/schedules/999/seats","status":404,"title":"Not Found","code":"SCHEDULE_NOT_FOUND"}
 *
 * 핸들러가 채우는 것은 status·detail·code뿐이다. title은 상태 코드의 기본 문구로, instance는 Spring이 요청 경로로
 * 채운다. type은 설정하지 않아 응답에서 빠진다.
 */
@RestControllerAdvice
public class BookingExceptionHandler {

  /**
   * 요청 값 검증(@Valid) 실패. 이 핸들러가 없으면 Spring 기본 처리로 본문 없는 400이 나가, 받는 쪽이 code로 구분할 수 없다.
   *
   * 응답에는 비어 있던 필드 이름을 담는다. 여럿이면 순서가 요청마다 달라지지 않게 정렬한다. 예: "요청 값이 올바르지 않다:
   * [seatId, userId]"
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handle(MethodArgumentNotValidException e) {
    List<String> fields =
        e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getField)
            .distinct()
            .sorted()
            .toList();
    return problem(BookingErrorCode.INVALID_REQUEST, fields);
  }

  @ExceptionHandler(ScheduleNotFoundException.class)
  public ProblemDetail handle(ScheduleNotFoundException e) {
    return problem(BookingErrorCode.SCHEDULE_NOT_FOUND, e.getScheduleId());
  }

  @ExceptionHandler(SeatNotFoundException.class)
  public ProblemDetail handle(SeatNotFoundException e) {
    return problem(BookingErrorCode.SEAT_NOT_FOUND, e.getSeatId());
  }

  /**
   * 회차는 요청 본문에 있어 보낸 쪽이 알고 있으므로, 응답에는 좌석 ID만 담는다. 회차까지 함께 남기는 것은 예외
   * 메시지(로그)의 몫이다.
   */
  @ExceptionHandler(SeatAlreadyTakenException.class)
  public ProblemDetail handle(SeatAlreadyTakenException e) {
    return problem(BookingErrorCode.SEAT_ALREADY_TAKEN, e.getSeat().seatId());
  }

  @ExceptionHandler(ReservationNotFoundException.class)
  public ProblemDetail handle(ReservationNotFoundException e) {
    return problem(BookingErrorCode.RESERVATION_NOT_FOUND, e.getReservationId());
  }

  /** 응답에는 예약 ID만 담는다. 예약의 주인이 누구인지는 남의 정보라 내보내지 않는다. */
  @ExceptionHandler(ReservationNotOwnedException.class)
  public ProblemDetail handle(ReservationNotOwnedException e) {
    return problem(BookingErrorCode.RESERVATION_NOT_OWNED, e.getReservationId());
  }

  @ExceptionHandler(ReservationExpiredException.class)
  public ProblemDetail handle(ReservationExpiredException e) {
    return problem(BookingErrorCode.RESERVATION_EXPIRED, e.getReservationId());
  }

  @ExceptionHandler(ReservationNotHeldException.class)
  public ProblemDetail handle(ReservationNotHeldException e) {
    return problem(BookingErrorCode.RESERVATION_NOT_HELD, e.getReservationId());
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
