package com.test.example.dao.jpa;

import com.test.example.domain.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Repository can be used to delegate CRUD operations.
 *
 * <p>Spring Data 3 split {@link PagingAndSortingRepository} from {@link CrudRepository}; we extend
 * both so that {@code save}, {@code findById}, {@code delete}, and paging are all available.
 */
public interface HotelRepository
    extends CrudRepository<Hotel, Long>, PagingAndSortingRepository<Hotel, Long> {

  Hotel findHotelByCity(String city);

  Page<Hotel> findAll(Pageable pageable);
}
