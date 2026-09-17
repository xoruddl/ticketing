package com.ticketing.booking.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공연의 좌석 하나. 예: 레미제라블 A구역 3열 12번, VIP, 150,000원
 *
 * 회차마다 따로 만들지 않고 모든 회차가 이 행을 함께 쓴다. 그래서 "예매 가능 여부"는 이 엔티티가 아니라 회차별 예약이 있는지로 판단한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 이 좌석이 속한 공연. 연관관계 대신 ID로 참조한다. */
  private Long performanceId;

  /** 구역·열·번호. 테이블에는 section, row_name, seat_number 컬럼으로 풀려 저장된다. */
  @Embedded private SeatPosition position;

  /** 좌석 등급. DB에는 이름(VIP, R, S)으로 저장해 enum 순서가 바뀌어도 값이 어긋나지 않게 한다. */
  @Enumerated(EnumType.STRING)
  private SeatGrade grade;

  /** 가격(원). 결제 금액이 된다. */
  private long price;

  // 인자가 4개라 CLEAN_CODE.md 8번(3개 이하)을 어긴다. 등급·가격을 묶을지는 따로 정한다.
  public Seat(Long performanceId, SeatPosition position, SeatGrade grade, long price) {
    this.performanceId = performanceId;
    this.position = position;
    this.grade = grade;
    this.price = price;
  }

  /** 이 좌석이 주어진 회차의 공연 좌석인지. 다른 공연의 좌석으로 선점하는 것을 막을 때 쓴다. */
  public boolean isIn(Schedule schedule) {
    return performanceId.equals(schedule.getPerformanceId());
  }
}
