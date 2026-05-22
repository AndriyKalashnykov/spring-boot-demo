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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
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

  @Test
  void putUpdatesHotelAndReturns204() {
    URI location = create(newHotel("put-happy"));
    long createdId = extractId(location);

    Hotel updated = newHotel("put-happy-updated");
    updated.setId(createdId);

    ResponseEntity<Void> putResponse =
        restTemplate.exchange(location, HttpMethod.PUT, jsonRequest(updated), Void.class);
    assertEquals(HttpStatus.NO_CONTENT, putResponse.getStatusCode());

    ResponseEntity<Hotel> readBack =
        restTemplate.exchange(location, HttpMethod.GET, jsonRequest(null), Hotel.class);
    assertEquals(HttpStatus.OK, readBack.getStatusCode());
    Hotel fetched = readBack.getBody();
    assertNotNull(fetched);
    assertEquals("put-happy-updated-name", fetched.getName(), "PUT must persist the new name");
    assertEquals("put-happy-updated-city", fetched.getCity(), "PUT must persist the new city");
  }

  @Test
  void paginationSizeOneReturnsAtMostOneItem() {
    create(newHotel("page-edge-a"));
    create(newHotel("page-edge-b"));
    create(newHotel("page-edge-c"));

    ResponseEntity<Map> response =
        restTemplate.exchange(
            HOTELS_PATH + "?page=0&size=1", HttpMethod.GET, jsonRequest(null), Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    List<?> content = (List<?>) body.get("content");
    assertNotNull(content);
    assertTrue(content.size() <= 1, "size=1 must yield at most one item, got " + content.size());
  }

  @Test
  void paginationFarPastEndReturnsEmptyContent() {
    create(newHotel("page-far"));

    ResponseEntity<Map> response =
        restTemplate.exchange(
            HOTELS_PATH + "?page=999&size=10", HttpMethod.GET, jsonRequest(null), Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    List<?> content = (List<?>) body.get("content");
    assertNotNull(content);
    assertTrue(content.isEmpty(), "page far past totalPages must return empty content");
  }

  @Test
  void paginationDefaultParamsReturn200() {
    create(newHotel("page-default"));

    ResponseEntity<Map> response =
        restTemplate.exchange(HOTELS_PATH, HttpMethod.GET, jsonRequest(null), Map.class);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("content"));
  }

  // -- Bad-input contract: validation must yield 400, never 500. --
  // Without @Valid + @NotBlank on Hotel.name, a null name propagated to the DB
  // layer and surfaced as a 500. Without @Validated + @Min on the page/size
  // params, PageRequest.of's IllegalArgumentException also became a 500.

  @Test
  void createWithNullNameReturns400() {
    // Hand-built JSON: Map.of doesn't allow null values.
    String body = "{\"name\":null,\"description\":\"d\",\"city\":\"c\",\"rating\":3}";
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.exchange(
            HOTELS_PATH, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    assertEquals(
        HttpStatus.BAD_REQUEST,
        response.getStatusCode(),
        "POST with null name must be rejected with 400, not propagate as 500");
  }

  @Test
  void createWithBlankNameReturns400() {
    String body = "{\"name\":\"   \",\"description\":\"d\",\"city\":\"c\",\"rating\":3}";
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> response =
        restTemplate.exchange(
            HOTELS_PATH, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    assertEquals(
        HttpStatus.BAD_REQUEST,
        response.getStatusCode(),
        "POST with whitespace-only name must be rejected with 400 (@NotBlank)");
  }

  @Test
  void getHotelsNegativePageReturns400() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            HOTELS_PATH + "?page=-1&size=10", HttpMethod.GET, jsonRequest(null), String.class);
    assertEquals(
        HttpStatus.BAD_REQUEST,
        response.getStatusCode(),
        "GET hotels?page=-1 must be rejected with 400, not propagate as 500");
  }

  @Test
  void getHotelsZeroSizeReturns400() {
    ResponseEntity<String> response =
        restTemplate.exchange(
            HOTELS_PATH + "?page=0&size=0", HttpMethod.GET, jsonRequest(null), String.class);
    assertEquals(
        HttpStatus.BAD_REQUEST,
        response.getStatusCode(),
        "GET hotels?size=0 must be rejected with 400 (PageRequest requires size >= 1)");
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
