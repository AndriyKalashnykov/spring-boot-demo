package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenApiIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void apiDocsAdvertiseHotelResource() {
    // Springfox 3.0.0 ships both /v2/api-docs (Swagger 2) and /v3/api-docs
    // (OpenAPI 3). The v3 mapper has a known NPE bug for some configs — use
    // v2 as the stable endpoint.
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/v2/api-docs", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String body = response.getBody();
    assertNotNull(body);
    assertTrue(
        body.contains("/example/v1/hotels"),
        "API doc should list /example/v1/hotels — guards against Springfox regressions");
  }

  @Test
  void swaggerUiIsServed() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.ALL));
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/swagger-ui/index.html", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
