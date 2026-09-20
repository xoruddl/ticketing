package com.ticketing.booking.domain;

/**
 * 예약의 상태. 사용자에게 보이는 그대로다.
 *
 * <pre>
 * HELD ──결제 성공──▶ CONFIRMED ──취소──▶ CANCELED
 *   │
 *   └──유효 시간 초과──▶ EXPIRED
 * </pre>
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
   * 이 상태의 예약이 좌석을 차지하고 있는가. 다른 사용자의 선점을 거절할지, 좌석을 예매 가능으로 보일지 판단할 때 쓴다.
   *
   * 만료 시각이 지났지만 아직 EXPIRED로 바뀌지 않은 HELD는 여기서 true다. 시각까지 보고 판단하는 것은
   * {@link Reservation}의 몫이다.
   */
  public boolean occupiesSeat() {
    return this == HELD || this == CONFIRMED;
  }
}
