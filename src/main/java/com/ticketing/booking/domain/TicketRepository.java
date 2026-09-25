package com.ticketing.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 티켓 저장소. 지금은 티켓을 저장하는 일만 한다.
 *
 * 예약으로 티켓을 찾는 조회는 예약 단건 조회(GET /reservations/{id})를 만들 때 더한다. 인덱스는 V3의
 * idx_ticket_reservation이 미리 있다.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {}
