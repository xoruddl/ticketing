package com.ticketing.booking.application;

import com.ticketing.booking.domain.Payment;
import com.ticketing.booking.domain.Reservation;
import com.ticketing.booking.domain.Ticket;

/**
 * 결제 한 번이 남긴 것 셋. 확정된 예약, 결제 기록, 발급된 티켓.
 *
 * 결제는 세 엔티티를 한꺼번에 바꾸는데, 응답은 셋을 다 보여줘야 한다(예약 상태, 결제 금액, 티켓 코드). 셋을
 * 따로 다시 조회하지 않게 한 쌍으로 묶어 돌려준다.
 *
 * {@link SeatAvailability}처럼 저장되는 값이 아니라 결과의 모양이라 application에 둔다.
 *
 * @param reservation 확정된 예약. 상태는 CONFIRMED다
 * @param payment 이번 결제 기록
 * @param ticket 이번에 발급된 티켓
 */
public record PaymentResult(Reservation reservation, Payment payment, Ticket ticket) {}
