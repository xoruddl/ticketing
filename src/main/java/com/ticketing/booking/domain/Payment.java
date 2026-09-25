package com.ticketing.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 하나에 대해 돈을 받은 기록. 예: 1번 예약에 7번 사용자가 150,000원을 19시 3분에 결제
 *
 * Step 0의 결제는 항상 성공하는 내부 처리다. 그래서 상태 필드가 없고, 이 행이 있다는 것 자체가 "결제 성공"이다.
 * 실패·결과 모름은 외부 PG가 들어오는 Step 7에서 더한다.
 *
 * 예약을 객체가 아니라 ID로 가리킨다. Step 4에서 payment가 따로 모듈이 되면 예약 엔티티를 볼 수 없게 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 결제한 예약. */
  private Long reservationId;

  /** 결제한 사용자. */
  private Long userId;

  /** 결제 금액(원). 결제 시점의 좌석 가격을 복사해 둔다. 좌석 가격이 나중에 바뀌어도 이미 낸 금액은 그대로다. */
  private long amount;

  /** 결제한 시각. */
  private LocalDateTime paidAt;

  private Payment(Long reservationId, Long userId, long amount, LocalDateTime paidAt) {
    this.reservationId = reservationId;
    this.userId = userId;
    this.amount = amount;
    this.paidAt = paidAt;
  }

  /**
   * 결제한다. 결제 기록을 만드는 유일한 길이다.
   *
   * 결제해도 되는 예약인지(주인, 상태, 만료)는 여기서 보지 않는다. 그 판단은 예약이 한다({@link
   * Reservation#confirm}). 여기서는 기록으로서 값이 온전한지만 본다.
   */
  public static Payment pay(Long reservationId, Long userId, long amount, LocalDateTime paidAt) {
    if (reservationId == null) {
      throw new IllegalArgumentException("결제할 예약 ID는 비어 있을 수 없다");
    }
    if (userId == null) {
      throw new IllegalArgumentException("결제한 사용자 ID는 비어 있을 수 없다");
    }
    // 좌석 가격이 0원일 수 있어(Seat 참고) 0원은 허용하고 음수만 막는다.
    if (amount < 0) {
      throw new IllegalArgumentException("결제 금액은 음수일 수 없다: " + amount);
    }
    if (paidAt == null) {
      throw new IllegalArgumentException("결제 시각은 비어 있을 수 없다");
    }
    return new Payment(reservationId, userId, amount, paidAt);
  }
}
