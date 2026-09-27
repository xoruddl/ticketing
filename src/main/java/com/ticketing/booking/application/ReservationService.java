package com.ticketing.booking.application;

import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.ReservationStatus;
import com.ticketing.booking.domain.Schedule;
import com.ticketing.booking.domain.ScheduleNotFoundException;
import com.ticketing.booking.domain.ScheduleRepository;
import com.ticketing.booking.domain.ScheduledSeat;
import com.ticketing.booking.domain.Seat;
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

  private final ScheduleRepository scheduleRepository;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;
  private final HoldPolicy holdPolicy;

  /**
   * 회차의 좌석 하나를 선점한다. 유효 시간 안에 결제하지 않으면 만료된다.
   *
   * 거절하는 경우
   * - 회차가 없다: ScheduleNotFoundException
   * - 좌석이 없거나 이 회차의 공연 좌석이 아니다: SeatNotFoundException
   * - 이미 선점·확정된 좌석이다: SeatAlreadyTakenException
   *
   * 인증이 아직 없어 userId는 요청이 주는 값을 그대로 믿는다.
   */
  public Reservation hold(Long scheduleId, Long seatId, Long userId) {
    Schedule schedule =
        scheduleRepository
            .findById(scheduleId)
            .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    Seat seat =
        seatRepository.findById(seatId).orElseThrow(() -> new SeatNotFoundException(seatId));
    // 좌석은 회차가 아니라 공연에 붙어 있다. 다른 공연의 좌석으로 이 회차를 예매하지 못하게 막는다.
    if (!seat.isIn(schedule)) {
      throw new SeatNotFoundException(seatId);
    }

    ScheduledSeat scheduledSeat = new ScheduledSeat(scheduleId, seatId);
    // 이 확인과 아래 저장 사이에 다른 요청이 끼어들면 같은 좌석에 예약이 둘 생긴다.
    // Step 0은 막지 않는다. Step 1에서 재현하고 Step 2에서 고른 방법으로 막는다.
    if (reservationRepository.existsBySeatAndStatusIn(
        scheduledSeat, ReservationStatus.occupying())) {
      throw new SeatAlreadyTakenException(scheduledSeat);
    }
    return reservationRepository.save(
        Reservation.hold(scheduledSeat, userId, holdPolicy.expiresAt()));
  }
}
