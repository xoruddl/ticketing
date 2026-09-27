package com.ticketing.booking.domain;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 예약의 상태. 사용자에게 보이는 그대로다.
 *
 * HELD ──결제 성공──▶ CONFIRMED ──취소──▶ CANCELED
 *   │
 *   └──유효 시간 초과──▶ EXPIRED
 *
 * DB에는 이름으로 저장한다 (V2 마이그레이션의 status 컬럼).
 */
public enum ReservationStatus {
  /** 선점. {@link Reservation}의 expiresAt까지 결제하지 않으면 만료된다. */
  HELD,
  /** 확정. 결제가 끝났다. */
  CONFIRMED,
  /** 만료. 유효 시간 안에 결제되지 않았다. */
  EXPIRED,
  /** 취소됨. */
  CANCELED;

  /**
   * 좌석을 차지하는 상태 전부. 상수로 적지 않고 {@link #occupiesSeat()}에서 뽑아내, 상태가 늘어도 규칙이 한
   * 곳에만 있게 한다.
   */
  private static final Set<ReservationStatus> OCCUPYING =
      Arrays.stream(values())
          .filter(ReservationStatus::occupiesSeat)
          .collect(Collectors.toUnmodifiableSet());

  /**
   * 좌석을 차지하는 상태들. 이 상태의 예약이 하나라도 있으면 그 회차·좌석은 팔린 것이다.
   *
   * 상태를 하나씩 묻는 {@link #occupiesSeat()}와 달리, 여러 상태를 한 번에 넘겨야 하는 저장소 조회 조건으로 쓴다
   * ({@link ReservationRepository#existsBySeatAndStatusIn}).
   */
  public static Set<ReservationStatus> occupying() {
    return OCCUPYING;
  }

  /**
   * 이 상태의 예약이 좌석을 차지하고 있는가. 다른 사용자의 선점을 거절할지, 좌석을 예매 가능으로 보일지 판단할 때 쓴다.
   *
   * 만료 시각이 지났지만 아직 EXPIRED로 바뀌지 않은 HELD는 여기서 true다. 시각까지 보고 판단하는 것은
   * {@link Reservation}의 몫이다.
   */
  public boolean occupiesSeat() {
    return this == HELD || this == CONFIRMED;
  }
}
