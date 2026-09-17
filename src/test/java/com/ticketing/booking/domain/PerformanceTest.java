package com.ticketing.booking.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PerformanceTest {

  @Test
  void 제목이_비어_있으면_만들_수_없다() {
    assertThatThrownBy(() -> new Performance(null, "블루스퀘어"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Performance(" ", "블루스퀘어"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 공연장이_비어_있으면_만들_수_없다() {
    assertThatThrownBy(() -> new Performance("레미제라블", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Performance("레미제라블", ""))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
