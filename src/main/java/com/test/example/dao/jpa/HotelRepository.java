package com.test.example.dao.jpa;

import com.test.example.domain.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

/**
 * Repository can be used to delegate CRUD operations
 */
public interface HotelRepository extends PagingAndSortingRepository<Hotel, Long> {
    Hotel findHotelByCity(String city);

    Page findAll(Pageable pageable);

    void delete(Hotel byId);
}
