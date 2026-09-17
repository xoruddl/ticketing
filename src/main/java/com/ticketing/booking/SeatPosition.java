package com.ticketing.booking;

import jakarta.persistence.Embeddable;

/**
 * 공연장 안에서 좌석의 위치. 예: A구역 3열 12번
 *
 * 구역·열·번호는 항상 함께 다니므로 하나의 값으로 묶는다. {@link Seat} 안에 {@code @Embedded}로 저장되어, 테이블에는
 * section, row_name, seat_number 컬럼으로 풀려 들어간다. 위치에 관한 규칙(검증, 표시)은 여기에 모은다.
 *
 * @param section 구역. 예: A
 * @param rowName 열. 숫자가 아닌 열(예: AA)도 있어 문자열로 둔다
 * @param seatNumber 열 안에서의 좌석 번호. 1부터 시작한다. 예: 12
 */
@Embeddable
public record SeatPosition(String section, String rowName, int seatNumber) {

  /** 구역·열은 비어 있을 수 없고, 번호는 1 이상이어야 한다. JPA가 DB에서 읽어올 때도 이 검증을 거친다. */
  public SeatPosition {
    if (section == null || section.isBlank()) {
      throw new IllegalArgumentException("구역은 비어 있을 수 없다");
    }
    if (rowName == null || rowName.isBlank()) {
      throw new IllegalArgumentException("열은 비어 있을 수 없다");
    }
    if (seatNumber < 1) {
      throw new IllegalArgumentException("좌석 번호는 1 이상이어야 한다: " + seatNumber);
    }
  }

  /** 화면에 보여줄 위치. 예: A구역 3열 12번 */
  public String label() {
    return section + "구역 " + rowName + "열 " + seatNumber + "번";
  }
}
