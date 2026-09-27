package com.ticketing;

import org.springframework.boot.SpringApplication;

public class TestTicketingApplication {

  public static void main(String[] args) {
    SpringApplication.from(TicketingApplication::main)
        .with(TestcontainersConfiguration.class, DevDataConfiguration.class)
        .run(args);
  }
}
