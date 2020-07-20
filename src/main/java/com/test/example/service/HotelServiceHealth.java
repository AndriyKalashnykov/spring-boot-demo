package com.test.example.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

/**
 * Application specific health check with Spring Boot health management endpoint.
 */
@Component
public class HotelServiceHealth implements HealthIndicator {

    @Autowired
    private ServiceProperties configuration;

    private int check() {
        int result = 0;

        return result;
    }

    // application-specific health check according to https://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready-health
    @Override
    public Health health() {

        int errorCode = check(); // perform some specific health check

        if (errorCode != 0) {
            return Health.down().withDetail("Error Code", errorCode).build();
        }

        return Health.up().withDetail("details", "{ 'internals' : 'all good', 'profile' : '" + this.configuration.getName() + "' }") .status(Status.UP).build();
    }
}
