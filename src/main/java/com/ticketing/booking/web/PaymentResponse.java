package com.ticketing.booking.web;

import com.ticketing.booking.application.PaymentResult;
import com.ticketing.booking.domain.ReservationStatus;

/**
 * 결제 결과. 예:
 * {"reservationId":1,"status":"CONFIRMED","paymentId":3,"amount":150000,"ticketId":5,"ticketCode":"3f2a9c1e-..."}
 *
 * 결제가 바꾼 세 가지를 한 번에 보여준다. 예약이 확정되었는지(status), 얼마를 냈는지(amount), 무엇으로 입장하는지
 * (ticketCode). 클라이언트가 결제 직후 예약·티켓을 다시 조회하지 않아도 되게 한다.
 *
 * 엔티티를 그대로 내보내지 않는 이유는 SeatResponse와 같다.
 *
 * @param reservationId 결제한 예약 ID
 * @param status 예약 상태. 결제에 성공했으므로 항상 CONFIRMED다
 * @param paymentId 결제 기록 ID
 * @param amount 결제 금액(원)
 * @param ticketId 발급된 티켓 ID
 * @param ticketCode 입장할 때 보여주는 코드
 */
public record PaymentResponse(
    Long reservationId,
    ReservationStatus status,
    Long paymentId,
    long amount,
    Long ticketId,
    String ticketCode) {

  public static PaymentResponse from(PaymentResult result) {
    return new PaymentResponse(
        result.reservation().getId(),
        result.reservation().getStatus(),
        result.payment().getId(),
        result.payment().getAmount(),
        result.ticket().getId(),
        result.ticket().getCode());
  }
}
