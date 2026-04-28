package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
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

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "management.server.port=")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class ActuatorIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void healthReportsUp() {
    ResponseEntity<Map> response = json("/actuator/health", Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals("UP", body.get("status"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void healthIncludesCustomHotelServiceComponent() {
    // application.yml sets show-details=always + show-components=always so the
    // /actuator/health body exposes the components map. The custom HotelServiceHealth
    // contributor registers under a Spring-derived bean-name key — match by substring
    // instead of guessing whether Spring trims "Health" from "HotelServiceHealth".
    ResponseEntity<Map> response = json("/actuator/health", Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    Map<String, Object> components = (Map<String, Object>) body.get("components");
    assertNotNull(components, "/actuator/health must expose 'components' (show-components=always)");
    String hotelKey =
        components.keySet().stream()
            .filter(k -> k.toLowerCase().contains("hotel"))
            .findFirst()
            .orElse(null);
    assertNotNull(
        hotelKey,
        "components must include a custom hotel-service indicator (got keys: "
            + components.keySet()
            + ")");
    Map<String, Object> hotelService = (Map<String, Object>) components.get(hotelKey);
    assertEquals("UP", hotelService.get("status"));
  }

  @Test
  void infoEndpointResponds() {
    ResponseEntity<Map> response = json("/actuator/info", Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void metricsEndpointResponds() {
    ResponseEntity<Map> response = json("/actuator/metrics", Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void prometheusScrapeEndpointResponds() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(java.util.List.of(MediaType.TEXT_PLAIN, MediaType.ALL));
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/actuator/prometheus", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
  }

  private <T> ResponseEntity<T> json(String path, Class<T> type) {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), type);
  }
}
