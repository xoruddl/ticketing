package com.ticketing.booking.domain;

import lombok.Getter;

/**
 * 다른 사람의 예약에 손을 댔다. 예: 7번 사용자가 선점한 예약에 8번 사용자가 결제를 보냈을 때
 *
 * 인증이 아직 없어 "누구인가"는 요청 본문의 userId를 그대로 믿는다. 그래도 이 검사를 지금 두는 이유는, 인증이
 * 붙은 뒤에도 "이 예약이 이 사용자의 것인가"라는 질문 자체는 그대로 남기 때문이다. 바뀌는 것은 userId를 어디서
 * 받느냐뿐이다.
 *
 * 도메인 예외는 HTTP를 모른다({@link ScheduleNotFoundException} 주석 참고).
 */
@Getter
public class ReservationNotOwnedException extends RuntimeException {

  /** 요청한 예약 ID. */
  private final Long reservationId;

  /** 요청한 사용자 ID. 예약의 주인이 누구인지는 담지 않는다. 응답으로 새면 남의 예약 정보가 된다. */
  private final Long userId;

  public ReservationNotOwnedException(Long reservationId, Long userId) {
    super("다른 사용자의 예약이다: 예약 " + reservationId + ", 요청 사용자 " + userId);
    this.reservationId = reservationId;
    this.userId = userId;
  }
}
