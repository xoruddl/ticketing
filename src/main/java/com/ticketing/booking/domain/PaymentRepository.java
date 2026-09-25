package com.ticketing.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결제 저장소. 지금은 결제를 저장하는 일만 한다.
 *
 * 예약으로 결제를 찾는 조회는 예약 단건 조회(GET /reservations/{id})를 만들 때 더한다. 인덱스는 V3의
 * idx_payment_reservation이 미리 있다.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {}
