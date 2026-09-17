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

  public Schedule(Long performanceId, LocalDateTime startAt) {
    this.performanceId = performanceId;
    this.startAt = startAt;
  }
}
