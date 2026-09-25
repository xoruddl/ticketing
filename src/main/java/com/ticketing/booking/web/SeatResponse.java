package com.ticketing.booking.web;

import com.ticketing.booking.application.SeatAvailability;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatGrade;
import com.ticketing.booking.domain.SeatPosition;

/**
 * 좌석 조회 응답의 좌석 하나. 예:
 * {"seatId":100,"section":"A","rowName":"1","seatNumber":1,"grade":"VIP","price":150000,"available":true}
 *
 * 엔티티를 그대로 내보내지 않고 응답 전용 모양으로 옮긴다. 엔티티 필드가 바뀌어도 API 응답이 따라 바뀌지 않게 하기 위해서다.
 *
 * @param seatId 선점 요청에 그대로 쓰는 좌석 ID
 * @param section 구역. 예: A
 * @param rowName 열. 예: 1, AA
 * @param seatNumber 열 안의 번호. 예: 12
 * @param grade 등급. 예: VIP
 * @param price 가격(원)
 * @param available 조회한 회차에서 지금 선점할 수 있는가. false면 화면은 좌석을 회색으로 칠하고 누를 수 없게 한다
 */
public record SeatResponse(
    Long seatId,
    String section,
    String rowName,
    int seatNumber,
    SeatGrade grade,
    long price,
    boolean available) {

  /**
   * 위치 값 객체는 화면에서 쓰기 쉽게 구역·열·번호 필드로 풀어서 담는다.
   *
   * available은 서비스가 계산한 값을 그대로 옮긴다. 응답을 만드는 여기서 다시 판단하지 않는다.
   */
  public static SeatResponse from(SeatAvailability availability) {
    Seat seat = availability.seat();
    SeatPosition position = seat.getPosition();
    return new SeatResponse(
        seat.getId(),
        position.section(),
        position.rowName(),
        position.seatNumber(),
        seat.getGrade(),
        seat.getPrice(),
        availability.available());
  }
}
