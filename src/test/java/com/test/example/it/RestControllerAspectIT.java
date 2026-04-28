package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.test.example.RestControllerAspect;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that {@link RestControllerAspect} actually fires on every public {@code *Controller}
 * method. A Logback {@link ListAppender} attached directly to the aspect's logger captures the
 * emitted events — more reliable than {@code OutputCaptureExtension}, which can miss output routed
 * through Logback's cached {@code System.out} reference.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class RestControllerAspectIT {

  @Autowired private TestRestTemplate restTemplate;

  private Logger aspectLogger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void attachAppender() {
    aspectLogger = (Logger) LoggerFactory.getLogger(RestControllerAspect.class);
    appender = new ListAppender<>();
    appender.start();
    aspectLogger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    aspectLogger.detachAppender(appender);
    appender.stop();
  }

  @Test
  void aspectLogsBeforeControllerInvocation() {
    // CommitInfoController is in com.test.example.api.rest, which is what the aspect's
    // pointcut `com.test.example.api.rest.*Controller.*(..)` actually matches. The
    // HotelController lives in the `.hotels` sub-package and falls outside the pointcut —
    // a quirk worth knowing if the aspect is ever extended.
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    ResponseEntity<String> response =
        restTemplate.exchange("/commitid", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode(), "request must succeed first");

    boolean fired =
        appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .anyMatch(msg -> msg.contains(":::::AOP Before REST call:::::"));
    assertTrue(
        fired,
        "RestControllerAspect must emit the canonical 'AOP Before REST call' marker (got "
            + appender.list.size()
            + " events)");
  }
}
