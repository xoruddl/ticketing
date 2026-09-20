package com.ticketing.booking.web;

import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.ScheduledSeat;
import java.time.LocalDateTime;

/**
 * 좌석 선점 결과. 예: {"reservationId":1,"scheduleId":10,"seatId":100,"status":"HELD","expiresAt":"2026-10-01T19:05:00"}
 *
 * 엔티티를 그대로 내보내지 않는 이유는 SeatResponse와 같다. 회차·좌석은 ScheduledSeat 값 객체를 풀어서 담는다.
 *
 * expiresAt은 타임존 없이 나간다. 앱이 서울 기준 시계를 쓰므로(ClockConfiguration) 값 자체는 서울 시각이지만,
 * 받는 쪽이 그것을 알 방법은 아직 없다. 시각을 내보내는 API가 예매 조회까지 늘어나면 그때 형식을 함께 정한다.
 *
 * @param reservationId 결제할 때 쓰는 예약 ID
 * @param scheduleId 선점한 회차 ID
 * @param seatId 선점한 좌석 ID
 * @param status 예약 상태. 선점 직후라 항상 HELD다
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
