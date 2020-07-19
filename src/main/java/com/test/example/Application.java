package com.test.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.xstream.XStreamMarshaller;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;


/*
 * This is the main Spring Boot application class. It configures Spring Boot, JPA, Swagger
 */

@Configuration
@SpringBootApplication
@EnableJpaRepositories("com.test.example.dao.jpa") // To segregate MongoDB and JPA repositories. Otherwise not needed.
public class Application  {
    private static final Class<Application> applicationClass = Application.class;
    private static final Logger log = LoggerFactory.getLogger(applicationClass);

    public static void main(String[] args) {
        SpringApplication.run(applicationClass, args);
    }

    @Autowired
    public Docket swaggerApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public HttpMessageConverters customConverters() {
        MarshallingHttpMessageConverter xmlConverter =new MarshallingHttpMessageConverter();
        XStreamMarshaller xstream = new XStreamMarshaller();
        xmlConverter.setMarshaller(xstream);
        xmlConverter.setUnmarshaller(xstream);

        return new HttpMessageConverters(xmlConverter);
    }

}
