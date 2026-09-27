package com.ticketing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * API 문서(springdoc). 컨트롤러를 읽어 만든 OpenAPI 문서와 Swagger UI가 뜨는지 본다.
 *
 * 문서 내용을 하나하나 검사하지 않는다. 컨트롤러가 문서에 잡히는지만 대표 API 하나로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocsTest {

  @Autowired MockMvcTester mvc;

  @Test
  void OpenAPI_문서에_선점_API가_있다() {
    assertThat(mvc.get().uri("/v3/api-docs"))
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.paths['/reservations'].post")
        .isNotNull();
  }

  /** /swagger-ui.html은 실제 화면(/swagger-ui/index.html)으로 넘겨주는 주소다. */
  @Test
  void Swagger_UI_화면이_뜬다() {
    assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatus(HttpStatus.OK);
  }
}
