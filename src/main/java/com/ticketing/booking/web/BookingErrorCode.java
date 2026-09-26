package com.ticketing.booking.web;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * booking 모듈이 사용자에게 내보내는 에러. 도메인 예외 하나에 코드 하나가 대응한다.
 *
 * 예: SCHEDULE_NOT_FOUND → 404, "회차를 찾을 수 없다"
 *
 * 전역 에러 코드(sharedkernel)로 모으지 않고 모듈 안에 둔다. 한 파일에 모으면 Step 4에서 모듈을 나눌 때 모든 모듈이 그 파일에 의존하게
 * 된다. domain이 아니라 web에 두는 이유는 HTTP 상태를 들고 있어서다. 도메인이 HttpStatus를 알면 안 된다.
 */
@Getter
@RequiredArgsConstructor
public enum BookingErrorCode {
  /**
   * 요청 값이 비었다. 대응 예외: MethodArgumentNotValidException (본문 검증 실패),
   * MissingServletRequestParameterException (필수 쿼리 파라미터 누락)
   *
   * 도메인 예외가 아니라 Spring이 던지는 예외에 대응하는 유일한 코드다. 서비스까지 가기 전에 막히지만, 받는 쪽이 다른
   * 거절과 같은 방식으로 구분할 수 있게 코드를 준다. 예: 좌석 없이 선점 → "요청 값이 올바르지 않다: [seatId]",
   * 사용자 없이 목록 조회 → "요청 값이 올바르지 않다: [userId]"
   */
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않다"),

  /** 요청한 회차가 없다. 대응 예외: ScheduleNotFoundException */
  SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "회차를 찾을 수 없다"),

  /** 요청한 회차에 그 좌석이 없다. 대응 예외: SeatNotFoundException */
  SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "좌석을 찾을 수 없다"),

  /**
   * 이미 팔린 좌석을 선점하려 했다. 대응 예외: SeatAlreadyTakenException
   *
   * 400이 아니라 409다. 요청이 틀린 것이 아니라 좌석의 상태가 바뀐 것이고, 나중에는 같은 요청이 성공할 수도 있다.
   */
  SEAT_ALREADY_TAKEN(HttpStatus.CONFLICT, "이미 팔린 좌석이다"),

  /** 요청한 예약이 없다. 대응 예외: ReservationNotFoundException */
  RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없다"),

  /**
   * 다른 사용자의 예약에 결제하려 했다. 대응 예외: ReservationNotOwnedException
   *
   * 404로 숨기지 않고 403으로 답한다. 인증이 아직 없어 "그런 예약이 있다"는 사실을 숨겨도 막아주는 것이 없고,
   * 숨기면 클라이언트가 예약 ID를 잘못 보낸 것인지 사용자를 잘못 보낸 것인지 구분할 수 없다.
   */
  RESERVATION_NOT_OWNED(HttpStatus.FORBIDDEN, "다른 사용자의 예약이다"),

  /**
   * 선점 유효 시간이 지난 예약에 결제하려 했다. 대응 예외: ReservationExpiredException
   *
   * 409다. 요청 모양은 맞고 예약의 상태(시간)가 결제를 허락하지 않는 것이다. 사용자는 좌석을 다시 선점해야 한다.
   */
  RESERVATION_EXPIRED(HttpStatus.CONFLICT, "선점 유효 시간이 지났다"),

  /**
   * 선점 상태가 아닌 예약에 결제하려 했다. 대응 예외: ReservationNotHeldException
   *
   * 이미 결제한 예약에 한 번 더 보낸 경우가 대표적이다. RESERVATION_EXPIRED와 코드를 나눈 이유는 사용자에게 할
   * 말이 달라서다. 만료는 "다시 선점하라", 이쪽은 "이미 끝났다"이다.
   */
  RESERVATION_NOT_HELD(HttpStatus.CONFLICT, "선점 상태가 아닌 예약이다");

  /** 응답 상태 코드. */
  private final HttpStatus status;

  /** 응답 본문에 담을 설명. 어떤 ID였는지 같은 요청별 값은 핸들러가 덧붙인다. */
  private final String message;
}
