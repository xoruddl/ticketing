package com.ticketing.booking.web;

import jakarta.validation.constraints.NotNull;

/**
 * 결제 요청. 예: POST /reservations/1/payment {"userId":7}
 *
 * 어떤 예약을 결제하는지는 경로가 말하고, 본문에는 누가 결제하는지만 담는다. 금액은 받지 않는다. 좌석 가격에서
 * 서비스가 정한다(사용자가 금액을 정하게 두면 안 된다).
 *
 * @param userId 결제하는 사용자 ID. 인증이 아직 없어 요청이 주는 값을 그대로 믿는다. 선점한 사용자와 다르면 거절된다
 */
public record PaymentRequest(@NotNull Long userId) {}
