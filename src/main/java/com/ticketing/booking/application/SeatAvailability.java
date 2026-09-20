package com.ticketing.booking.application;

import com.ticketing.booking.domain.Seat;

/**
 * 좌석 하나와, 그 좌석을 지금 예매할 수 있는지.
 *
 * 좌석 조회는 좌석 정보만으로 끝나지 않는다. 같은 좌석이라도 회차에 따라 팔렸을 수도 있고 아닐 수도 있어
 * ({@link Seat} 주석 참고), 조회한 회차 기준의 판단을 좌석에 붙여 돌려줘야 한다. 이 레코드가 그 한 쌍이다.
 *
 * 도메인이 아니라 여기(application)에 두는 이유: 저장되는 값이 아니라 조회 결과의 모양이다. available은 어느
 * 표의 컬럼도 아니고, 예약이 있는지를 보고 그때그때 계산한 값이다.
 *
 * 판단 자체는 여기서 하지 않는다. 한 좌석만 봐서는 알 수 없고 그 회차의 예약들을 봐야 하므로, 저장소를 가진
 * {@link SeatQueryService}가 계산해 담는다.
 *
 * @param seat 좌석 정보. 구역·열·번호·등급·가격
 * @param available 이 회차에서 지금 선점할 수 있는가
 */
public record SeatAvailability(Seat seat, boolean available) {}
