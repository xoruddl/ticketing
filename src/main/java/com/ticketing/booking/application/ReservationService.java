package com.ticketing.booking.application;

import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.SeatAlreadyTakenException;
import com.ticketing.booking.domain.SeatNotFoundException;
import com.ticketing.booking.domain.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석을 선점한다.
 *
 * 좌석이 팔렸는지는 좌석이 아니라 예약으로 판단하므로, 선점은 새 예약을 하나 만드는 일이다. 결제·확정은 아직 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;
  private final HoldPolicy holdPolicy;

  /**
   * 좌석 하나를 선점한다. 유효 시간 안에 결제하지 않으면 만료된다.
   *
   * 거절하는 경우
   * - 좌석이 없다: SeatNotFoundException
   * - 이미 선점·확정된 좌석이다: SeatAlreadyTakenException
   *
   * 공연은 받지 않는다. 좌석이 공연에 속하므로 좌석 하나가 한 번 팔린다 (DECISIONS.md "도메인: 회차(Schedule)를 뺀다").
   *
   * 인증이 아직 없어 userId는 요청이 주는 값을 그대로 믿는다.
   */
  public Reservation hold(Long seatId, Long userId) {
    if (!seatRepository.existsById(seatId)) {
      throw new SeatNotFoundException(seatId);
    }
    // 동시 요청은 막지 않는다. 확인과 저장 사이에 다른 요청이 끼어들면 둘 다 통과한다.
    // 어떻게 되는지는 DoubleHoldReproductionTest가 보여주고, Step 2에서 막는다.
    if (reservationRepository.existsBySeatIdAndStatusIn(seatId, ReservationStatus.occupying())) {
      throw new SeatAlreadyTakenException(seatId);
    }
    return reservationRepository.save(Reservation.hold(seatId, userId, holdPolicy.expiresAt()));
  }
}
