package com.test.example.api.rest;

import com.test.example.domain.RestErrorInfo;
import com.test.example.exception.DataFormatException;
import com.test.example.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * This class is meant to be extended by all REST resource "controllers". It contains exception
 * mapping and other common REST API functionality
 */
// @ControllerAdvice?
public abstract class AbstractRestHandler implements ApplicationEventPublisherAware {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());
  protected ApplicationEventPublisher eventPublisher;

  protected static final String DEFAULT_PAGE_SIZE = "100";
  protected static final String DEFAULT_PAGE_NUM = "0";

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(DataFormatException.class)
  public @ResponseBody RestErrorInfo handleDataStoreException(
      DataFormatException ex, WebRequest request, HttpServletResponse response) {
    log.info("Converting Data Store exception to RestResponse : " + ex.getMessage());

    return new RestErrorInfo(ex, "handleDataStoreException");
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public @ResponseBody RestErrorInfo handleResourceNotFoundException(
      ResourceNotFoundException ex, WebRequest request, HttpServletResponse response) {
    log.info("ResourceNotFoundException handler: " + ex.getMessage());

    return new RestErrorInfo(ex, "handleResourceNotFoundException");
  }

  /**
   * Maps Bean Validation method-parameter violations to 400. Triggered by {@code @Validated} on the
   * controller class plus {@code @Min}/{@code @Max}/etc on {@code @RequestParam} or
   * {@code @PathVariable} arguments. Spring's {@code DefaultHandlerExceptionResolver} does not map
   * {@link ConstraintViolationException} on its own — without this handler the violation propagates
   * as a 500.
   */
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(ConstraintViolationException.class)
  public @ResponseBody RestErrorInfo handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request, HttpServletResponse response) {
    log.info("ConstraintViolationException handler: " + ex.getMessage());

    return new RestErrorInfo(ex, "handleConstraintViolation");
  }

  /**
   * Defense-in-depth for bad-input {@link IllegalArgumentException}s that aren't caught upstream by
   * {@code @Validated}. {@code PageRequest.of} throws IAE on negative page or zero/negative size;
   * at the controller boundary IAE is a client-side bug, not a server error.
   */
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(IllegalArgumentException.class)
  public @ResponseBody RestErrorInfo handleIllegalArgument(
      IllegalArgumentException ex, WebRequest request, HttpServletResponse response) {
    log.info("IllegalArgumentException handler: " + ex.getMessage());

    return new RestErrorInfo(ex, "handleIllegalArgument");
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.eventPublisher = applicationEventPublisher;
  }

  // todo: replace with exception mapping
  public static <T> T checkResourceFound(final T resource) {
    if (resource == null) {
      throw new ResourceNotFoundException("resource not found");
    }
    return resource;
  }
}
