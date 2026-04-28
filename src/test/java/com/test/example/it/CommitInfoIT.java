package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
class CommitInfoIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  @SuppressWarnings("unchecked")
  void getCommitIdReturnsPopulatedJson() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    ResponseEntity<List> response =
        restTemplate.exchange("/commitid", HttpMethod.GET, new HttpEntity<>(headers), List.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    List<Map<String, String>> commits = response.getBody();
    assertNotNull(commits, "body must be present");
    assertFalse(commits.isEmpty(), "body must contain at least one commit object");

    Map<String, String> first = commits.get(0);
    // git-commit-id-plugin populates these from .git during `mvn package`.
    // The exception path in CommitInfoController#load swallows IOException
    // when git.properties is absent — fields default to "" rather than null.
    assertTrue(first.containsKey("id"), "commit object must have 'id' field");
    assertTrue(first.containsKey("message"), "commit object must have 'message' field");
    assertTrue(first.containsKey("branch"), "commit object must have 'branch' field");
    assertTrue(first.containsKey("time"), "commit object must have 'time' field");
  }
}
