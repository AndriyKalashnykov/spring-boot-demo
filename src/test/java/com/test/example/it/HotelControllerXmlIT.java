package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.test.example.domain.Hotel;
import java.net.URI;
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

/**
 * Verifies XML content negotiation against the Hotel CRUD endpoints. The Spring Boot 4 migration
 * replaced XStream with Jackson XML; without this IT a regression in the XML serdes path could
 * silently break clients that prefer XML.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class HotelControllerXmlIT {

  private static final String HOTELS_PATH = "/example/v1/hotels";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void postAndGetXmlRoundTrip() {
    Hotel payload = new Hotel();
    payload.setName("xml-name");
    payload.setDescription("xml-description");
    payload.setCity("xml-city");
    payload.setRating(4);

    HttpHeaders postHeaders = new HttpHeaders();
    postHeaders.setContentType(MediaType.APPLICATION_XML);
    postHeaders.setAccept(List.of(MediaType.APPLICATION_XML));
    ResponseEntity<Void> createResponse =
        restTemplate.exchange(
            HOTELS_PATH, HttpMethod.POST, new HttpEntity<>(payload, postHeaders), Void.class);

    assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
    URI location = createResponse.getHeaders().getLocation();
    assertNotNull(location);

    HttpHeaders getHeaders = new HttpHeaders();
    getHeaders.setAccept(List.of(MediaType.APPLICATION_XML));
    ResponseEntity<String> getResponse =
        restTemplate.exchange(location, HttpMethod.GET, new HttpEntity<>(getHeaders), String.class);

    assertEquals(HttpStatus.OK, getResponse.getStatusCode());
    assertTrue(
        getResponse.getHeaders().getContentType().includes(MediaType.APPLICATION_XML),
        "response must be XML");
    String body = getResponse.getBody();
    assertNotNull(body);
    assertTrue(body.contains("<name>xml-name</name>"), "XML must include serialised name element");
    assertTrue(body.contains("<city>xml-city</city>"), "XML must include serialised city element");
  }
}
