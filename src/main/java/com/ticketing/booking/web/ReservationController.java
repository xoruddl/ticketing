package com.ticketing.booking.web;

import com.ticketing.booking.application.ReservationQueryService;
import com.ticketing.booking.application.ReservationService;
import com.ticketing.booking.domain.Reservation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

  /**
   * 내 예매 목록을 최근 예약부터 조회한다. 예: GET /reservations?userId=7
   *
   * 한 줄은 예약 요약({@link ReservationResponse})이고 결제·티켓은 담지 않는다. 예약이 없으면 빈 배열이다.
   *
   * 사용자는 인증이 아직 없어 쿼리 파라미터로 받는다. 선점·결제가 본문으로 받는 것과 같은 임시방편이고, 누구의 목록이든
   * 볼 수 있다. 사용자를 app이 정하게 되면(Step 4) 이 파라미터는 사라진다.
   */
  @GetMapping("/reservations")
  public List<ReservationResponse> findMine(@RequestParam Long userId) {
    return reservationQueryService.findAllByUser(userId).stream()
        .map(ReservationResponse::from)
        .toList();
  }
}
