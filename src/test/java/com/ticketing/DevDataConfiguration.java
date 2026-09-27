package com.ticketing;

import com.ticketing.booking.domain.Performance;
import com.ticketing.booking.domain.PerformanceRepository;
import com.ticketing.booking.domain.Schedule;
import com.ticketing.booking.domain.ScheduleRepository;
import com.ticketing.booking.domain.Seat;
import com.ticketing.booking.domain.SeatGrade;
import com.ticketing.booking.domain.SeatPosition;
import com.ticketing.booking.domain.SeatRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 개발 실행기(TestTicketingApplication)로 띄울 때 넣는 샘플 공연·회차·좌석.
 *
 * 공연·회차·좌석 등록 API는 만들지 않는다(FEATURES.md). 그래서 앱을 띄운 뒤 Swagger UI로 선점·결제를 해 보려면
 * 누군가 먼저 데이터를 넣어 줘야 한다. 운영 마이그레이션에 넣으면 운영 DB에도 가짜 공연이 생기므로, 개발 실행기에만
 * 얹는 test 소스의 설정으로 둔다. 통합 테스트(@SpringBootTest)는 이 설정을 가져오지 않는다.
 *
 * 개발 실행기는 띄울 때마다 새 MySQL 컨테이너를 쓴다. 빈 DB에 들어가므로 ID가 매번 같다.
 * - 공연 1: 레미제라블 @ 블루스퀘어
 * - 회차 1: 2026-12-24 19:00
 * - 좌석 1~5: A구역 1열 1~5번, VIP 150,000원
 * - 좌석 6~10: A구역 2열 1~5번, R 120,000원
 */
@TestConfiguration(proxyBeanMethods = false)
public class DevDataConfiguration {

  private static final Logger log = LoggerFactory.getLogger(DevDataConfiguration.class);

  /** 한 열의 좌석 수. */
  private static final int SEATS_PER_ROW = 5;

  @Bean
  ApplicationRunner devData(
      PerformanceRepository performanceRepository,
      ScheduleRepository scheduleRepository,
      SeatRepository seatRepository) {
    return args -> {
      // 컨테이너를 재사용하도록 바꾸면 기존 데이터가 남는다. 그때 같은 공연이 겹겹이 쌓이지 않게 한다.
      if (performanceRepository.count() > 0) {
        return;
      }
      Performance performance = performanceRepository.save(new Performance("레미제라블", "블루스퀘어"));
      Schedule schedule =
          scheduleRepository.save(
              new Schedule(performance.getId(), LocalDateTime.of(2026, 12, 24, 19, 0)));
      List<Seat> seats = new ArrayList<>();
      seats.addAll(row(performance, "1", SeatGrade.VIP, 150_000L));
      seats.addAll(row(performance, "2", SeatGrade.R, 120_000L));
      seatRepository.saveAll(seats);

      log.info("샘플 데이터: 회차 {} (좌석 조회 GET /schedules/{}/seats)", schedule.getId(), schedule.getId());
    };
  }

  /** A구역의 한 열. 번호는 1부터 붙인다. */
  private static List<Seat> row(
      Performance performance, String rowName, SeatGrade grade, long price) {
    List<Seat> seats = new ArrayList<>();
    for (int number = 1; number <= SEATS_PER_ROW; number++) {
      seats.add(
          new Seat(performance.getId(), new SeatPosition("A", rowName, number), grade, price));
    }
    return seats;
  }
}
