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
class HotelControllerIT {

  private static final String HOTELS_PATH = "/example/v1/hotels";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void postCreateRetrieveDeleteRoundTrip() {
    URI location = create(newHotel("roundtrip"));

    ResponseEntity<Hotel> getResponse =
        restTemplate.exchange(location, HttpMethod.GET, jsonRequest(null), Hotel.class);
    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    Hotel fetched = getResponse.getBody();
    assertNotNull(fetched);
    assertEquals("roundtrip-name", fetched.getName());
    assertEquals("roundtrip-city", fetched.getCity());

    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(location, HttpMethod.DELETE, null, Void.class);
    assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

    ResponseEntity<String> afterDelete =
        restTemplate.exchange(location, HttpMethod.GET, jsonRequest(null), String.class);
    assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
  }

  @Test
  void getByUnknownIdReturns404() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            HOTELS_PATH + "/" + Long.MAX_VALUE, HttpMethod.GET, jsonRequest(null), String.class);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void putWithMismatchedIdReturns400() {
    URI location = create(newHotel("mismatch"));
    long createdId = extractId(location);

    Hotel updated = newHotel("mismatch-update");
    updated.setId(createdId + 1);

    ResponseEntity<String> response =
        restTemplate.exchange(location, HttpMethod.PUT, jsonRequest(updated), String.class);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void listReturnsPagedResults() {
    create(newHotel("page-a"));
    create(newHotel("page-b"));

    ResponseEntity<Map> response =
        restTemplate.exchange(
            HOTELS_PATH + "?page=0&size=10", HttpMethod.GET, jsonRequest(null), Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("content"), "paged response must include 'content'");
  }

  private URI create(Hotel payload) {
    ResponseEntity<Void> createResponse =
        restTemplate.exchange(HOTELS_PATH, HttpMethod.POST, jsonRequest(payload), Void.class);
    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode(), "create must return 201");
    URI location = createResponse.getHeaders().getLocation();
    assertNotNull(location);
    return location;
  }

  private static HttpEntity<Object> jsonRequest(Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    if (body != null) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }
    return new HttpEntity<>(body, headers);
  }

  private static Hotel newHotel(String prefix) {
    Hotel h = new Hotel();
    h.setName(prefix + "-name");
    h.setDescription(prefix + "-description");
    h.setCity(prefix + "-city");
    h.setRating(3);
    return h;
  }

  private static long extractId(URI location) {
    String[] parts = location.getPath().split("/");
    return Long.parseLong(parts[parts.length - 1]);
  }
}
