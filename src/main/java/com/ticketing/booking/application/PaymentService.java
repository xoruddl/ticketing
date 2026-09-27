package com.ticketing.booking.application;

import com.ticketing.booking.domain.Payment;
import com.ticketing.booking.domain.PaymentRepository;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationNotFoundException;
import com.ticketing.booking.domain.ReservationNotOwnedException;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatNotFoundException;
import com.ticketing.booking.domain.SeatRepository;
import com.ticketing.booking.domain.Ticket;
import com.ticketing.booking.domain.TicketRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선점한 좌석을 결제해 예약을 확정하고 티켓을 발급한다.
 *
 * 결제는 항상 성공하는 내부 처리다. 외부 PG와 결과를 모르는 경우는 Step 7에서 다룬다.
 *
 * 선점({@link ReservationService})과 서비스를 나눈 이유: Step 4에서 결제는 payment 모듈로 갈라진다. 그때 이
 * 클래스가 옮겨갈 자리를 미리 떼어 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

  private final ReservationRepository reservationRepository;
  private final SeatRepository seatRepository;
  private final PaymentRepository paymentRepository;
  private final TicketRepository ticketRepository;
  private final Clock clock;

  /**
   * 예약을 결제한다. 결제 → 확정 → 발권이 한 트랜잭션에서 일어난다.
   *
   * 거절하는 경우
   * - 예약이 없다: ReservationNotFoundException
   * - 다른 사용자의 예약이다: ReservationNotOwnedException
   * - 선점 상태가 아니다(이미 확정 등): ReservationNotHeldException
   * - 선점 유효 시간이 지났다: ReservationExpiredException
   *
   * 셋을 한 트랜잭션에 묶었으므로 중간에 거절되면 아무것도 남지 않는다. 이것이 Step 0의 "일부러 허술한" 부분이다.
   * Step 4에서 예약·결제·티켓이 다른 모듈이 되면 이 묶음을 유지할 수 없고, 그때 무엇이 깨지는지 Step 6·8에서 본다.
   *
   * 인증이 아직 없어 userId는 요청이 주는 값을 그대로 믿는다.
   */
  public PaymentResult pay(Long reservationId, Long userId) {
    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    if (!reservation.isOwnedBy(userId)) {
      throw new ReservationNotOwnedException(reservationId, userId);
    }

    // 결제·확정·발권이 같은 시각을 쓰도록 한 번만 읽는다.
    LocalDateTime now = LocalDateTime.now(clock);
    // 결제해도 되는 예약인지(상태, 만료)는 예약이 판단한다. 기록을 남기기 전에 먼저 거절한다.
    reservation.confirm(now);

    // 금액은 요청으로 받지 않고 좌석 가격에서 가져온다. 사용자가 금액을 정하게 두면 안 된다.
    Long seatId = reservation.getSeat().seatId();
    Seat seat =
        seatRepository.findById(seatId).orElseThrow(() -> new SeatNotFoundException(seatId));
    Payment payment =
        paymentRepository.save(Payment.pay(reservationId, userId, seat.getPrice(), now));
    Ticket ticket = ticketRepository.save(Ticket.issue(reservationId, now));

    // 예약의 상태 변경은 save 없이 트랜잭션이 끝날 때 변경 감지로 반영된다.
    return new PaymentResult(reservation, payment, ticket);
  }
}
