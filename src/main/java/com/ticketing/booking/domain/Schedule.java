package com.ticketing.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 공연의 날짜·시간별 편성. 같은 좌석이 회차마다 따로 팔린다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long performanceId;

  private LocalDateTime startAt;

  /** 어느 공연의 언제 회차인지는 반드시 있어야 한다. 과거 시각인지는 여기서 따지지 않는다. */
  public Schedule(Long performanceId, LocalDateTime startAt) {
    if (performanceId == null) {
      throw new IllegalArgumentException("회차의 공연 ID는 비어 있을 수 없다");
    }
    if (startAt == null) {
      throw new IllegalArgumentException("회차 시작 시각은 비어 있을 수 없다");
    }
    this.performanceId = performanceId;
    this.startAt = startAt;
  }
}
