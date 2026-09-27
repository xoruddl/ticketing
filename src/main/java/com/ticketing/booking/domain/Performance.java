package com.ticketing.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  /** 공연장 이름. 예: 블루스퀘어 */
  private String venue;

  /** 제목·공연장은 비어 있을 수 없다. DB의 not null 오류가 아니라 만드는 시점에 이유를 드러낸다. */
  public Performance(String title, String venue) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("공연 제목은 비어 있을 수 없다");
    }
    if (venue == null || venue.isBlank()) {
      throw new IllegalArgumentException("공연장은 비어 있을 수 없다");
    }
    this.title = title;
    this.venue = venue;
  }
}
