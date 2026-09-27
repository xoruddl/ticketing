package com.ticketing.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** 회차 저장소. 좌석 조회·선점은 모두 회차 ID로 시작하므로, 회차가 있는지부터 여기서 찾는다. */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {}
