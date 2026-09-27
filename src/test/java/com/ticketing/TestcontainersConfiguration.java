package com.ticketing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트용 MySQL 컨테이너. 쓰는 테스트에서 @Import(TestcontainersConfiguration.class)로 가져온다.
 *
 * 모듈별 테스트(com.ticketing.booking 등)는 하위 패키지라 별개 패키지이므로, 가져다 쓸 수 있게 public으로 연다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  MySQLContainer mysqlContainer() {
    // latest가 아니라 고정한다 — 이미지가 바뀌면 어제 통과한 테스트가 오늘 깨진다.
    return new MySQLContainer(DockerImageName.parse("mysql:8.0"));
  }
}
