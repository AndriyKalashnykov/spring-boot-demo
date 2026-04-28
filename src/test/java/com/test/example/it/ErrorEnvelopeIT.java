package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.test.example.domain.Hotel;
import java.net.URI;
import java.util.List;
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

/**
 * Verifies the {@code RestErrorInfo} body shape returned by {@code AbstractRestHandler}'s
 * {@code @ExceptionHandler} methods on {@link com.test.example.exception.ResourceNotFoundException}
 * (404) and {@link com.test.example.exception.DataFormatException} (400). Without this IT the
 * handlers could silently regress to Spring's default error body without changing the status code.
 * Bodies are deserialised as {@link Map} because {@code RestErrorInfo} has only a non-default
 * constructor and {@code public final} fields — Jackson cannot bind to it without extra
 * annotations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class ErrorEnvelopeIT {

  private static final String HOTELS_PATH = "/example/v1/hotels";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @SuppressWarnings("unchecked")
  void notFoundReturnsRestErrorInfoEnvelope() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    ResponseEntity<Map> response =
        restTemplate.exchange(
            HOTELS_PATH + "/" + Long.MAX_VALUE,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body, "404 response must have a body");
    assertEquals(
        "handleResourceNotFoundException",
        body.get("detail"),
        "detail must identify the handler that mapped the exception");
    Object message = body.get("message");
    assertNotNull(message, "message must be present");
    assertTrue(message.toString().length() > 0, "message must not be empty");
  }

  @Test
  @SuppressWarnings("unchecked")
  void putWithMismatchedIdReturnsRestErrorInfoEnvelope() {
    URI location = createHotel("err-envelope");
    long createdId = extractId(location);

    Hotel mismatched = new Hotel();
    mismatched.setId(createdId + 1);
    mismatched.setName("mismatch");
    mismatched.setDescription("mismatch");
    mismatched.setCity("mismatch");
    mismatched.setRating(1);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    ResponseEntity<Map> response =
        restTemplate.exchange(
            location, HttpMethod.PUT, new HttpEntity<>(mismatched, headers), Map.class);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body, "400 response must have a body");
    assertEquals(
        "handleDataStoreException",
        body.get("detail"),
        "detail must identify the DataFormatException handler");
    assertNotNull(body.get("message"), "message must be present");
  }

  private URI createHotel(String prefix) {
    Hotel h = new Hotel();
    h.setName(prefix + "-name");
    h.setDescription(prefix + "-description");
    h.setCity(prefix + "-city");
    h.setRating(3);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    ResponseEntity<Void> resp =
        restTemplate.exchange(
            HOTELS_PATH, HttpMethod.POST, new HttpEntity<>(h, headers), Void.class);
    URI location = resp.getHeaders().getLocation();
    assertNotNull(location);
    return location;
  }

  private static long extractId(URI location) {
    String[] parts = location.getPath().split("/");
    return Long.parseLong(parts[parts.length - 1]);
  }
}
