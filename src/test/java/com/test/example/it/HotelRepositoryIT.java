package com.test.example.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.test.example.dao.jpa.HotelRepository;
import com.test.example.domain.Hotel;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class HotelRepositoryIT {

  @Autowired private HotelRepository repository;

  @Test
  void saveAndFindById() {
    Hotel saved = repository.save(newHotel("save"));

    Optional<Hotel> found = repository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("save-city", found.get().getCity());
  }

  @Test
  void findHotelByCity() {
    repository.save(newHotel("byCity"));
    Hotel result = repository.findHotelByCity("byCity-city");
    assertNotNull(result);
    assertEquals("byCity-name", result.getName());
  }

  @Test
  void pagedFindAllRespectsPageRequest() {
    for (int i = 0; i < 5; i++) {
      repository.save(newHotel("paged-" + i));
    }
    Page<?> page = repository.findAll(PageRequest.of(0, 3));
    assertEquals(3, page.getNumberOfElements());
    assertTrue(page.getTotalElements() >= 5);
  }

  @Test
  void deleteRemovesEntity() {
    Hotel saved = repository.save(newHotel("delete"));
    Long id = saved.getId();
    repository.delete(saved);
    assertFalse(repository.findById(id).isPresent());
  }

  private static Hotel newHotel(String prefix) {
    Hotel h = new Hotel();
    h.setName(prefix + "-name");
    h.setDescription(prefix + "-description");
    h.setCity(prefix + "-city");
    h.setRating(3);
    return h;
  }
}
