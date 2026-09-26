package com.ticketing.booking.web;

import com.ticketing.booking.application.ReservationQueryService;
import com.ticketing.booking.application.ReservationService;
import com.ticketing.booking.domain.Reservation;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 API. 사용자는 좌석 조회에서 고른 좌석을 여기서 선점하고, 유효 시간 안에 결제해 확정한 뒤, 예약을 다시 조회한다.
 */
@RestController
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;
  private final ReservationQueryService reservationQueryService;

  /**
   * 좌석을 선점한다. 예: POST /reservations {"scheduleId":10,"seatId":100,"userId":7}
   *
   * 새 예약을 만들었으므로 200이 아니라 201로 답하고, 어디서 찾는지 Location 헤더로 알려준다. 그 주소로 예약 단건
   * 조회(GET /reservations/{id})를 하면 된다.
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

  /**
   * 예약 하나를 결제·티켓과 함께 조회한다. 예: GET /reservations/1
   *
   * 없는 예약은 404다. 누가 조회하는지는 받지 않는다 — 이유는 {@link ReservationQueryService#find} 주석 참고.
   */
  @GetMapping("/reservations/{reservationId}")
  public ReservationDetailResponse find(@PathVariable Long reservationId) {
    return ReservationDetailResponse.from(reservationQueryService.find(reservationId));
  }
}
