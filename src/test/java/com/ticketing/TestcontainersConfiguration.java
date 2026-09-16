package com.ticketing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  MySQLContainer mysqlContainer() {
    // latest가 아니라 고정한다 — 이미지가 바뀌면 어제 통과한 테스트가 오늘 깨진다.
    return new MySQLContainer(DockerImageName.parse("mysql:8.0"));
  }
}
