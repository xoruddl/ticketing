package com.ticketing.booking.web;

import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.ScheduledSeat;
import java.time.LocalDateTime;

/**
 * 예약 요약. 예: {"reservationId":1,"scheduleId":10,"seatId":100,"status":"HELD","expiresAt":"2026-10-01T19:05:00"}
 *
 * 좌석 선점 결과와 내 예매 목록의 한 줄로 쓴다. 결제·티켓까지 보려면 예약 단건 조회({@link ReservationDetailResponse})를
 * 쓴다.
 *
 * 엔티티를 그대로 내보내지 않는 이유는 SeatResponse와 같다. 회차·좌석은 ScheduledSeat 값 객체를 풀어서 담는다.
 *
 * expiresAt은 타임존 없이 나간다. 앱이 서울 기준 시계를 쓰므로(ClockConfiguration) 값 자체는 서울 시각이지만,
 * 받는 쪽이 그것을 알 방법은 아직 없다. 예매 조회의 시각(결제·발권 시각)도 같은 형식으로 나가고, 타임존 표기는 아직
 * 정하지 않았다.
 *
 * @param reservationId 예약 ID. 결제·단건 조회에 쓴다
 * @param scheduleId 회차 ID
 * @param seatId 좌석 ID
 * @param status 예약 상태. 선점 직후에는 항상 HELD다
 * @param expiresAt 이 시각까지 결제하지 않으면 만료된다
 */
public record ReservationResponse(
    Long reservationId,
    Long scheduleId,
    Long seatId,
    ReservationStatus status,
    LocalDateTime expiresAt) {

  public static ReservationResponse from(Reservation reservation) {
    ScheduledSeat seat = reservation.getSeat();
    return new ReservationResponse(
        reservation.getId(),
        seat.scheduleId(),
        seat.seatId(),
        reservation.getStatus(),
        reservation.getExpiresAt());
  }
}
