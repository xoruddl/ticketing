package com.ticketing.booking.web;

import jakarta.validation.constraints.NotNull;

/**
 * 좌석 선점 요청. 예: POST /reservations {"scheduleId":10,"seatId":100,"userId":7}
 *
 * 좌석 하나만 선점한다(FEATURES.md F2). 여러 좌석을 한 번에 잡는 것은 범위 밖이다.
 *
 * 값이 비었는지는 여기서 막고, 그 회차·좌석이 실제로 있는지는 서비스가 본다. 셋 다 필수라 하나라도 없으면 400으로
 * 거절된다.
 *
 * @param scheduleId 선점할 회차 ID
 * @param seatId 선점할 좌석 ID. 좌석 조회 응답의 seatId를 그대로 보낸다
 * @param userId 선점하는 사용자 ID. 인증이 아직 없어 요청이 주는 값을 그대로 믿는다
 */
public record ReservationRequest(
    @NotNull Long scheduleId, @NotNull Long seatId, @NotNull Long userId) {}
