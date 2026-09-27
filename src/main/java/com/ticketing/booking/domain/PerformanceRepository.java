package com.ticketing.booking.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** 공연 저장소. Step 0에는 공연 등록 API가 없어, 지금은 테스트가 공연을 준비할 때만 쓴다. */
public interface PerformanceRepository extends JpaRepository<Performance, Long> {}
