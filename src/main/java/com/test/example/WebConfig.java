package com.test.example;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Content-negotiation customisation. Spring Boot auto-configures WebMvc, so this class only
 * contributes the {@code ?mediaType=xml|json} query-parameter strategy on top of the default
 * Accept-header negotiation.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
    configurer
        .favorParameter(true)
        .parameterName("mediaType")
        .ignoreAcceptHeader(false)
        .defaultContentType(MediaType.APPLICATION_JSON)
        .mediaType("xml", MediaType.APPLICATION_XML)
        .mediaType("json", MediaType.APPLICATION_JSON);
  }
}
