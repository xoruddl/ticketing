package com.ticketing.booking.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 티켓 저장소. 티켓을 저장하고, 예약 단건 조회(GET /reservations/{id})에서 예약의 티켓을 찾는다. */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

  /**
   * 이 예약에 발급된 티켓. 아직 확정되지 않은 예약이면 비어 있다.
   *
   * reservation_id 조건은 V3의 idx_ticket_reservation 인덱스를 탄다.
   *
   * 결제와 같은 이유로 Optional로 받는다({@link PaymentRepository#findByReservationId} 참고).
   */
  Optional<Ticket> findByReservationId(Long reservationId);
}
