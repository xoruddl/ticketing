package com.ticketing.booking.application;

import com.ticketing.booking.domain.Payment;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.Ticket;
import java.util.Optional;

/**
 * 예약 단건 조회 결과. 예약 하나와, 그 예약에 딸린 결제·티켓.
 *
 * 결제와 티켓은 예약 표가 아니라 각자의 표에 있고 예약 ID로만 이어진다(V3 주석 참고). 그래서 예약만 읽어서는 답할
 * 수 없고, 세 표를 따로 읽어 여기서 한 쌍으로 묶는다.
 *
 * {@link PaymentResult}와 달리 결제·티켓이 없을 수 있다. 선점 중이거나 만료된 예약에는 둘 다 없다.
 *
 * @param reservation 조회한 예약
 * @param payment 이 예약의 결제. 결제 전이면 비어 있다
 * @param ticket 이 예약의 티켓. 확정 전이면 비어 있다
 */
public record ReservationDetail(
    Reservation reservation, Optional<Payment> payment, Optional<Ticket> ticket) {}
