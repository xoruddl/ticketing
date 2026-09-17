package com.ticketing.booking.domain;

/** 좌석 등급. 가격은 등급이 아니라 좌석마다 따로 가진다 ({@link Seat}의 price). */
public enum SeatGrade {
  /** 가장 앞쪽·가운데 좌석. */
  VIP,
  /** 로열석. */
  R,
  /** 스탠더드석. */
  S
}
