package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class OpenApiIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void apiDocsAdvertiseHotelResource() {
    // springdoc-openapi exposes the OpenAPI 3 JSON at /v3/api-docs.
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/v3/api-docs", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String body = response.getBody();
    assertNotNull(body);
    assertTrue(body.contains("/example/v1/hotels"), "OpenAPI doc should list /example/v1/hotels");
  }

  @Test
  void swaggerUiIsServed() {
    // springdoc-openapi-starter-webmvc-ui redirects /swagger-ui.html ->
    // /swagger-ui/index.html. Follow the redirect.
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.TEXT_HTML, MediaType.ALL));
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/swagger-ui/index.html", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }
}
