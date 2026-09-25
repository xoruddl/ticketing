package com.ticketing.booking.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 회차의 좌석 하나를 잡은 것. 예: 7번 사용자가 10월 1일 회차의 A구역 3열 12번을 19시 5분까지 선점
 *
 * 이 회차의 이 좌석이 팔렸는지는 좌석이 아니라 이 예약이 있는지로 판단한다({@link Seat} 주석 참고).
 * 예약은 언제나 선점(HELD)으로 태어나고, 결제가 끝나면 확정된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 어느 회차의 어느 좌석인지. 테이블에는 schedule_id, seat_id 컬럼으로 풀려 저장된다. */
  @Embedded private ScheduledSeat seat;

  /** 이 좌석을 잡은 사용자. 인증은 아직 없어 요청이 주는 값을 그대로 믿는다. */
  private Long userId;

  /** DB에는 이름으로 저장해 enum 순서가 바뀌어도 값이 어긋나지 않게 한다. */
  @Enumerated(EnumType.STRING)
  private ReservationStatus status;

  /** 선점 유효 시간이 끝나는 시각. 선점할 때 박아두고 이후 바꾸지 않는다. */
  private LocalDateTime expiresAt;

  private Reservation(ScheduledSeat seat, Long userId, LocalDateTime expiresAt) {
    this.seat = seat;
    this.userId = userId;
    this.status = ReservationStatus.HELD;
    this.expiresAt = expiresAt;
  }

  /**
   * 좌석을 선점한다. 예약을 만드는 유일한 길이다.
   *
   * 만료 시각을 직접 받는다. 유효 시간이 몇 분인지, 지금이 몇 시인지는 도메인이 알 일이 아니라 부르는 쪽(서비스)의
   * 설정과 시계가 정한다.
   *
   * 다른 사람이 이미 잡은 좌석인지는 여기서 보지 않는다. 예약 하나만으로는 알 수 없고 그 회차·좌석의 다른 예약을
   * 봐야 하므로, 그 판단은 저장소를 가진 서비스가 한다.
   */
  public static Reservation hold(ScheduledSeat seat, Long userId, LocalDateTime expiresAt) {
    if (seat == null) {
      throw new IllegalArgumentException("예약의 회차·좌석은 비어 있을 수 없다");
    }
    if (userId == null) {
      throw new IllegalArgumentException("예약의 사용자 ID는 비어 있을 수 없다");
    }
    if (expiresAt == null) {
      throw new IllegalArgumentException("선점 만료 시각은 비어 있을 수 없다");
    }
    return new Reservation(seat, userId, expiresAt);
  }

  /**
   * 주어진 시각 기준으로 선점 유효 시간이 끝났는가. 만료 시각과 같은 순간부터 만료로 본다.
   *
   * 상태는 보지 않는다. HELD인 예약에만 의미가 있는 질문이다.
   *
   * 여기서 하는 일은 판정뿐이고, 상태를 EXPIRED로 바꾸거나 좌석을 다시 팔지는 않는다. Step 0은 만료된 선점을
   * 자동으로 풀어주지 않는다 — 누가 언제 풀지는 Step 3에서 정한다.
   */
  public boolean isExpired(LocalDateTime now) {
    return !now.isBefore(expiresAt);
  }

  /** 이 사용자가 선점한 예약인가. 결제처럼 본인만 할 수 있는 일을 막을 때 쓴다. */
  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }

  /**
   * 결제가 끝난 선점을 확정한다. HELD → CONFIRMED
   *
   * 거절하는 경우
   * - 선점 상태가 아니다(이미 확정·취소·만료): ReservationNotHeldException
   * - 선점 유효 시간이 지났다: ReservationExpiredException
   *
   * 상태를 먼저 본다. 이미 확정된 예약은 만료 시각이 지났더라도 "만료"가 아니라 "이미 확정"이 맞는 답이다.
   *
   * 만료되어 거절할 때 상태를 EXPIRED로 바꾸지 않는다. 거절은 예외로 끝나 트랜잭션이 롤백되므로 여기서 바꿔도 남지
   * 않고, 만료를 누가 언제 반영할지는 Step 3에서 정한다.
   */
  public void confirm(LocalDateTime now) {
    if (status != ReservationStatus.HELD) {
      throw new ReservationNotHeldException(id, status);
    }
    if (isExpired(now)) {
      throw new ReservationExpiredException(id, expiresAt);
    }
    this.status = ReservationStatus.CONFIRMED;
  }
}
