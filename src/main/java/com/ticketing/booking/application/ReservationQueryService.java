package com.ticketing.booking.application;

import com.ticketing.booking.domain.PaymentRepository;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.ReservationNotFoundException;
import com.ticketing.booking.domain.ReservationRepository;
import com.ticketing.booking.domain.TicketRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예매를 조회한다. 예약 단건(결제·티켓 포함)과 내 예매 목록.
 *
 * 선점({@link ReservationService})·결제({@link PaymentService})와 서비스를 나눈 이유: 둘은 상태를 바꾸고 이쪽은
 * 읽기만 한다. 좌석 조회({@link SeatQueryService})처럼 읽기 전용 트랜잭션을 클래스에 한 번 건다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationQueryService {

  private final ReservationRepository reservationRepository;
  private final PaymentRepository paymentRepository;
  private final TicketRepository ticketRepository;

  /**
   * 예약 하나를 결제·티켓과 함께 돌려준다. 예약이 없으면 ReservationNotFoundException
   *
   * 예약·결제·티켓을 세 번 따로 읽는다. 결제·티켓은 예약을 ID로만 가리키고(연관관계 없음) 표도 따로라, 한 번에
   * 읽으려면 JOIN을 직접 써야 한다. 그러지 않는 이유는 Step 4에서 셋이 다른 모듈의 표가 되기 때문이다.
   *
   * 누가 조회하는지는 보지 않는다. 인증이 아직 없어 사용자 ID를 받아도 요청이 주는 값을 믿을 수밖에 없다. 본인
   * 확인은 사용자를 app이 정하게 되는 Step 4 이후의 일이다.
   */
  public ReservationDetail find(Long reservationId) {
    Reservation reservation =
        reservationRepository
            .findById(reservationId)
            .orElseThrow(() -> new ReservationNotFoundException(reservationId));
    return new ReservationDetail(
        reservation,
        paymentRepository.findByReservationId(reservationId),
        ticketRepository.findByReservationId(reservationId));
  }

  /**
   * 이 사용자의 예약 전부를 최근에 만든 것부터 돌려준다. 예약이 없으면 빈 목록이다.
   *
   * 목록에는 결제·티켓을 붙이지 않는다. 예약마다 붙이면 예약 N개에 조회가 2N번 더 나간다. 자세한 내용은 목록에서
   * 하나를 골라 {@link #find}로 본다.
   *
   * 만료 시각이 지났지만 아직 HELD인 예약도 HELD 그대로 나온다. Step 0은 만료를 상태에 반영하지 않는다(Step 3).
   */
  public List<Reservation> findAllByUser(Long userId) {
    return reservationRepository.findAllByUserIdOrderByIdDesc(userId);
  }
}
