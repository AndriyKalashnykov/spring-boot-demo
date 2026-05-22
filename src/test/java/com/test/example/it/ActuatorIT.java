package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  @SuppressWarnings("unchecked")
  void infoEndpointExposesBuildArtifact() {
    // info.build.* in application.yml uses Maven resource-filtering tokens
    // (@project.artifactId@); a regression that breaks filtering would leave
    // the literal token in the response — assert the substituted value.
    ResponseEntity<Map> response = json("/actuator/info", Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    Map<String, Object> build = (Map<String, Object>) body.get("build");
    assertNotNull(build, "/actuator/info must expose 'build' (configured in application.yml)");
    assertEquals(
        "spring-boot-demo",
        build.get("artifact"),
        "info.build.artifact must resolve the @project.artifactId@ token");
  }

  @Test
  void metricsEndpointResponds() {
    ResponseEntity<Map> response = json("/actuator/metrics", Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void prometheusScrapeReturnsExpositionFormat() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(java.util.List.of(MediaType.TEXT_PLAIN, MediaType.ALL));
    ResponseEntity<String> response =
        restTemplate.exchange(
            "/actuator/prometheus", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    String body = response.getBody();
    assertNotNull(body);
    // Prometheus exposition format always emits `# HELP` and `# TYPE` headers
    // per metric. Asserting both catches a regression that swaps the format
    // (e.g. accidentally returning JSON) while staying renderer-version-stable.
    assertTrue(
        body.contains("# HELP"),
        "Prometheus scrape body must contain '# HELP' lines (exposition format)");
    assertTrue(
        body.contains("# TYPE"),
        "Prometheus scrape body must contain '# TYPE' lines (exposition format)");
  }

  private <T> ResponseEntity<T> json(String path, Class<T> type) {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), type);
  }
}
