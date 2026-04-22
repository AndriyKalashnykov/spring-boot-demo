package com.test.example.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.example.Application;
import com.test.example.domain.Hotel;
import java.util.Random;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
class HotelControllerTest {

  // Ant-style pattern (Spring MockMvc's redirectedUrlPattern uses Ant globs, not regex)
  private static final String RESOURCE_LOCATION_PATTERN = "http://localhost/example/v1/hotels/*";

  @Autowired private WebApplicationContext context;

  private MockMvc mvc;

  @BeforeEach
  void initTests() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @Test
  void shouldCreateRetrieveDelete() throws Exception {
    Hotel r1 = mockHotel("shouldCreateRetrieveDelete");
    byte[] r1Json = toJson(r1);

    MvcResult result =
        mvc.perform(
                post("/example/v1/hotels")
                    .content(r1Json)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrlPattern(RESOURCE_LOCATION_PATTERN))
            .andReturn();
    long id = getResourceIdFromUrl(result.getResponse().getRedirectedUrl());

    mvc.perform(get("/example/v1/hotels/" + id).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", Matchers.is((int) id)))
        .andExpect(jsonPath("$.name", Matchers.is(r1.getName())))
        .andExpect(jsonPath("$.city", Matchers.is(r1.getCity())))
        .andExpect(jsonPath("$.description", Matchers.is(r1.getDescription())))
        .andExpect(jsonPath("$.rating", Matchers.is(r1.getRating())));

    mvc.perform(delete("/example/v1/hotels/" + id)).andExpect(status().isNoContent());

    mvc.perform(get("/example/v1/hotels/" + Long.MAX_VALUE).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldCreateAndUpdateAndDelete() throws Exception {
    Hotel r1 = mockHotel("shouldCreateAndUpdate");
    byte[] r1Json = toJson(r1);

    MvcResult result =
        mvc.perform(
                post("/example/v1/hotels")
                    .content(r1Json)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrlPattern(RESOURCE_LOCATION_PATTERN))
            .andReturn();
    long id = getResourceIdFromUrl(result.getResponse().getRedirectedUrl());

    Hotel r2 = mockHotel("shouldCreateAndUpdate2");
    r2.setId(id);
    byte[] r2Json = toJson(r2);

    mvc.perform(
            put("/example/v1/hotels/" + id)
                .content(r2Json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    mvc.perform(get("/example/v1/hotels/" + id).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", Matchers.is((int) id)))
        .andExpect(jsonPath("$.name", Matchers.is(r2.getName())))
        .andExpect(jsonPath("$.city", Matchers.is(r2.getCity())))
        .andExpect(jsonPath("$.description", Matchers.is(r2.getDescription())))
        .andExpect(jsonPath("$.rating", Matchers.is(r2.getRating())));

    mvc.perform(delete("/example/v1/hotels/" + id)).andExpect(status().isNoContent());
  }

  private static long getResourceIdFromUrl(String locationUrl) {
    String[] parts = locationUrl.split("/");
    return Long.parseLong(parts[parts.length - 1]);
  }

  private static Hotel mockHotel(String prefix) {
    Hotel r = new Hotel();
    r.setCity(prefix + "_city");
    r.setDescription(prefix + "_description");
    r.setName(prefix + "_name");
    r.setRating(new Random().nextInt(6));
    return r;
  }

  private static byte[] toJson(Object r) throws Exception {
    return new ObjectMapper().writeValueAsString(r).getBytes();
  }
}
