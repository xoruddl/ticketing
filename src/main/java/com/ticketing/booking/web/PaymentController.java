package com.ticketing.booking.web;

import com.ticketing.booking.application.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API. 사용자는 선점한 예약을 유효 시간 안에 여기서 결제하고, 확정된 예약과 티켓을 받는다.
 *
 * 경로는 예약 아래(/reservations/{id}/payment)지만 ReservationController와 나눈다. 결제는 Step 4에서 payment
 * 모듈로 갈라질 자리라, 서비스(PaymentService)와 함께 옮겨갈 수 있게 떼어 둔다.
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  /**
   * 예약을 결제한다. 예: POST /reservations/1/payment {"userId":7}
   *
   * 201이 아니라 200으로 답한다. 결제 기록과 티켓이 새로 생기지만, 이 요청이 하는 일은 "예약을 확정하라"는 동작이고
   * 응답의 주인공도 확정된 예약이다.
   *
   * 거절은 예외로 나가고 BookingExceptionHandler가 상태 코드를 정한다. 없는 예약은 404, 남의 예약은 403, 만료되었거나
   * 이미 결제한 예약은 409다.
   */
  @PostMapping("/reservations/{reservationId}/payment")
  public PaymentResponse pay(
      @PathVariable Long reservationId, @Valid @RequestBody PaymentRequest request) {
    return PaymentResponse.from(paymentService.pay(reservationId, request.userId()));
  }
}
