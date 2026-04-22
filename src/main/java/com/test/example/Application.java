package com.test.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Spring Boot 4 entry point. */
@SpringBootApplication
@EnableJpaRepositories("com.test.example.dao.jpa")
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
