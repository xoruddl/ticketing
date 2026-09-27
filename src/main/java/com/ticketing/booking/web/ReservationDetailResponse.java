package com.ticketing.booking.web;

import com.ticketing.booking.application.ReservationDetail;
import com.ticketing.booking.domain.Payment;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.ScheduledSeat;
import com.ticketing.booking.domain.Ticket;
import java.time.LocalDateTime;

/**
 * 예약 단건 조회 결과. 예:
 *
 * {"reservationId":1,"scheduleId":10,"seatId":100,"status":"CONFIRMED","expiresAt":"2026-10-01T19:05:00",
 *  "payment":{"paymentId":3,"amount":150000,"paidAt":"2026-10-01T19:03:00"},
 *  "ticket":{"ticketId":5,"code":"3f2a9c1e-...","issuedAt":"2026-10-01T19:03:00"}}
 *
 * 예약 필드는 {@link ReservationResponse}와 같고, 결제·티켓을 묶음으로 덧붙인다. 결제 전이면 payment와 ticket이
 * null로 나간다. 필드를 빼지 않고 null로 두는 이유: 받는 쪽이 "아직 없음"과 "응답 형식이 다름"을 헷갈리지 않는다.
 *
 * 시각은 ReservationResponse와 같이 타임존 없이 나간다(그 주석 참고).
 *
 * @param reservationId 예약 ID
 * @param scheduleId 회차 ID
 * @param seatId 좌석 ID
 * @param status 예약 상태. 만료 시각이 지났어도 아직 HELD로 나올 수 있다(Step 3에서 다룬다)
 * @param expiresAt 선점 유효 시간이 끝나는 시각
 * @param payment 결제. 결제 전이면 null
 * @param ticket 티켓. 확정 전이면 null
 */
public record ReservationDetailResponse(
    Long reservationId,
    Long scheduleId,
    Long seatId,
    ReservationStatus status,
    LocalDateTime expiresAt,
    PaymentView payment,
    TicketView ticket) {

  public static ReservationDetailResponse from(ReservationDetail detail) {
    Reservation reservation = detail.reservation();
    ScheduledSeat seat = reservation.getSeat();
    return new ReservationDetailResponse(
        reservation.getId(),
        seat.scheduleId(),
        seat.seatId(),
        reservation.getStatus(),
        reservation.getExpiresAt(),
        detail.payment().map(PaymentView::from).orElse(null),
        detail.ticket().map(TicketView::from).orElse(null));
  }

  /**
   * 예약에 딸린 결제.
   *
   * @param paymentId 결제 기록 ID
   * @param amount 결제 금액(원)
   * @param paidAt 결제한 시각
   */
  public record PaymentView(Long paymentId, long amount, LocalDateTime paidAt) {

    static PaymentView from(Payment payment) {
      return new PaymentView(payment.getId(), payment.getAmount(), payment.getPaidAt());
    }
  }

  /**
   * 예약에 발급된 티켓.
   *
   * @param ticketId 티켓 ID
   * @param code 입장할 때 보여주는 코드
   * @param issuedAt 발급한 시각
   */
  public record TicketView(Long ticketId, String code, LocalDateTime issuedAt) {

    static TicketView from(Ticket ticket) {
      return new TicketView(ticket.getId(), ticket.getCode(), ticket.getIssuedAt());
    }
  }
}
