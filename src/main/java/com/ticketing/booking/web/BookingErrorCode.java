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
  /** 요청한 회차가 없다. 대응 예외: ScheduleNotFoundException */
  SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "회차를 찾을 수 없다");

  /** 응답 상태 코드. */
  private final HttpStatus status;

  /** 응답 본문에 담을 설명. 어떤 ID였는지 같은 요청별 값은 핸들러가 덧붙인다. */
  private final String message;
}
