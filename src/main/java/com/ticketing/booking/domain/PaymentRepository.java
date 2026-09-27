package com.ticketing.booking.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 결제 저장소. 결제를 저장하고, 예약 단건 조회(GET /reservations/{id})에서 예약의 결제를 찾는다. */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  /**
   * 이 예약의 결제. 아직 결제하지 않은 예약이면 비어 있다.
   *
   * reservation_id 조건은 V3의 idx_payment_reservation 인덱스를 탄다.
   *
   * 예약 하나에 결제는 하나라고 보고 Optional로 받는다. 같은 예약에 결제 행이 둘 있으면 이 조회는 예외로 끝난다.
   * 그런 행이 생기지 않게 막는 장치(유니크 제약 등)는 아직 없다 — V3 주석 참고.
   */
  Optional<Payment> findByReservationId(Long reservationId);
}
