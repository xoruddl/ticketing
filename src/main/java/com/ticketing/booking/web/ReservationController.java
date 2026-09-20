package com.ticketing.booking.web;

import com.ticketing.booking.application.ReservationService;
import com.ticketing.booking.domain.Reservation;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 예약 API. 사용자는 좌석 조회에서 고른 좌석을 여기서 선점하고, 유효 시간 안에 결제해 확정한다. */
@RestController
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  /**
   * 좌석을 선점한다. 예: POST /reservations {"scheduleId":10,"seatId":100,"userId":7}
   *
   * 새 예약을 만들었으므로 200이 아니라 201로 답하고, 어디서 찾는지 Location 헤더로 알려준다. 조회 API는 아직
   * 없지만 경로는 예매 조회가 생길 자리(GET /reservations/{id})로 미리 맞춘다.
   *
   * 거절은 예외로 나가고 BookingExceptionHandler가 상태 코드를 정한다. 없는 회차·좌석은 404, 이미 팔린 좌석은
   * 409다.
   */
  @PostMapping("/reservations")
  public ResponseEntity<ReservationResponse> hold(@Valid @RequestBody ReservationRequest request) {
    Reservation reservation =
        reservationService.hold(request.scheduleId(), request.seatId(), request.userId());
    ReservationResponse response = ReservationResponse.from(reservation);
    return ResponseEntity.created(URI.create("/reservations/" + response.reservationId()))
        .body(response);
  }
}
