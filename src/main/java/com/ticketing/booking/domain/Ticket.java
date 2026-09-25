package com.ticketing.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 확정된 예약에 발급되는 입장권. 예: 1번 예약에 코드 "3f2a...-..."인 티켓을 19시 3분에 발급
 *
 * 예약을 객체가 아니라 ID로 가리킨다. Step 4에서 ticket이 따로 모듈이 되면 예약 엔티티를 볼 수 없게 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 이 티켓으로 입장하는 예약. */
  private Long reservationId;

  /**
   * 입장할 때 보여주는 코드. 예: 3f2a9c1e-8b7d-4e21-9f0a-6c5d4b3a2e1f
   *
   * id를 그대로 쓰지 않는 이유: auto_increment라 다음 번호를 추측할 수 있다. UUID는 추측할 수 없고, 중복은 V3의
   * uk_ticket_code 유니크 키가 막는다.
   */
  private String code;

  /** 발급한 시각. */
  private LocalDateTime issuedAt;

  private Ticket(Long reservationId, String code, LocalDateTime issuedAt) {
    this.reservationId = reservationId;
    this.code = code;
    this.issuedAt = issuedAt;
  }

  /**
   * 티켓을 발급한다. 티켓을 만드는 유일한 길이다.
   *
   * 확정된 예약인지는 여기서 보지 않는다. 확정과 발권은 같은 트랜잭션에서 이어지므로 부르는 쪽(서비스)이 순서로
   * 보장한다. 이 둘이 다른 모듈·트랜잭션으로 갈라지는 문제는 Step 4 이후에 다룬다.
   *
   * 코드는 부르는 쪽에서 받지 않고 여기서 만든다. 어떤 형식의 코드를 쓸지는 티켓이 정할 일이다.
   */
  public static Ticket issue(Long reservationId, LocalDateTime issuedAt) {
    if (reservationId == null) {
      throw new IllegalArgumentException("발권할 예약 ID는 비어 있을 수 없다");
    }
    if (issuedAt == null) {
      throw new IllegalArgumentException("발권 시각은 비어 있을 수 없다");
    }
    return new Ticket(reservationId, UUID.randomUUID().toString(), issuedAt);
  }
}
