package com.test.example.api.rest.hotels;

import com.test.example.api.rest.AbstractRestHandler;
import com.test.example.domain.Hotel;
import com.test.example.exception.DataFormatException;
import com.test.example.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Hotel REST endpoints. */
@RestController
@RequestMapping(value = "/example/v1/hotels")
@Tag(name = "hotels")
public class HotelController extends AbstractRestHandler {

  private static final Logger log = LoggerFactory.getLogger(HotelController.class);

  @Autowired private HotelService hotelService;

  @RequestMapping(
      value = "",
      method = RequestMethod.POST,
      consumes = {"application/json", "application/xml"},
      produces = {"application/json", "application/xml"})
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a hotel resource.",
      description = "Returns the URL of the new resource in the Location header.")
  public void createHotel(
      @RequestBody Hotel hotel, HttpServletRequest request, HttpServletResponse response) {
    Hotel createdHotel = this.hotelService.createHotel(hotel);
    response.setHeader(
        "Location", request.getRequestURL().append("/").append(createdHotel.getId()).toString());
    log.debug("createHotel()");
  }

  @RequestMapping(
      value = "",
      method = RequestMethod.GET,
      produces = {"application/json", "application/xml"})
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Get a paginated list of all hotels.",
      description =
          "The list is paginated. You can provide a page number (default 0) and a page size"
              + " (default 100).")
  public @ResponseBody Page<Hotel> getAllHotels(
      @Parameter(description = "The page number (zero-based)", required = true)
          @RequestParam(value = "page", required = true, defaultValue = DEFAULT_PAGE_NUM)
          Integer page,
      @Parameter(description = "The page size", required = true)
          @RequestParam(value = "size", required = true, defaultValue = DEFAULT_PAGE_SIZE)
          Integer size,
      HttpServletRequest request,
      HttpServletResponse response) {
    log.debug("getAllHotels()");
    return this.hotelService.getAllHotels(page, size);
  }

  @RequestMapping(
      value = "/{id}",
      method = RequestMethod.GET,
      produces = {"application/json", "application/xml"})
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get a single hotel.", description = "You have to provide a valid hotel ID.")
  public @ResponseBody Hotel getHotel(
      @Parameter(description = "The ID of the hotel.", required = true) @PathVariable("id") Long id,
      HttpServletRequest request,
      HttpServletResponse response)
      throws Exception {
    Hotel hotel = this.hotelService.getHotel(id);
    log.debug("checkResourceFound: " + hotel);
    checkResourceFound(hotel);
    return hotel;
  }

  @RequestMapping(
      value = "/{id}",
      method = RequestMethod.PUT,
      consumes = {"application/json", "application/xml"},
      produces = {"application/json", "application/xml"})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Update a hotel resource.",
      description =
          "You have to provide a valid hotel ID in the URL and in the payload. The ID attribute can"
              + " not be updated.")
  public void updateHotel(
      @Parameter(description = "The ID of the existing hotel resource.", required = true)
          @PathVariable("id")
          Long id,
      @RequestBody Hotel hotel,
      HttpServletRequest request,
      HttpServletResponse response) {
    checkResourceFound(this.hotelService.getHotel(id));
    if (!Objects.equals(id, hotel.getId())) {
      throw new DataFormatException("ID doesn't match!");
    }
    this.hotelService.updateHotel(hotel);
  }

  @RequestMapping(
      value = "/{id}",
      method = RequestMethod.DELETE,
      produces = {"application/json", "application/xml"})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Delete a hotel resource.",
      description =
          "You have to provide a valid hotel ID in the URL. Once deleted the resource can not be"
              + " recovered.")
  public void deleteHotel(
      @Parameter(description = "The ID of the existing hotel resource.", required = true)
          @PathVariable("id")
          Long id,
      HttpServletRequest request,
      HttpServletResponse response) {
    checkResourceFound(this.hotelService.getHotel(id));
    this.hotelService.deleteHotel(id);
  }
}
