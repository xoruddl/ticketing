package com.ticketing.booking.web;

import com.ticketing.booking.application.SeatQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 좌석 조회 API. 사용자는 회차를 고른 뒤 이 목록을 보고 선점할 좌석을 고른다. */
@RestController
@RequiredArgsConstructor
public class SeatController {

  private final SeatQueryService seatQueryService;

  /** 회차의 좌석 목록과 좌석마다 예매 가능 여부. 예: GET /schedules/10/seats */
  @GetMapping("/schedules/{scheduleId}/seats")
  public List<SeatResponse> findSeats(@PathVariable Long scheduleId) {
    return seatQueryService.findSeatAvailabilities(scheduleId).stream()
        .map(SeatResponse::from)
        .toList();
  }
}
