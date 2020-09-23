package com.test.example.api.rest.docs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import javax.servlet.ServletContext;


@Component
@Configuration
public class SwaggerConfig  {

    @Autowired(required=false)
    private ServletContext servletContext_;


    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.test.example.api.rest.hotels"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }


    private ApiInfo apiInfo() {
        String description = "REST example";
        return new ApiInfoBuilder()
                .title("REST example")
                .description(description)
                .termsOfServiceUrl("github")
                .license("Apache 2.0")
                .licenseUrl("")
                .version("1.0")
                .build();
    }

//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("swagger-ui.html")
//                .addResourceLocations("classpath:/META-INF/resources/");
//
//        registry.addResourceHandler("/webjars/**")
//                .addResourceLocations("classpath:/META-INF/resources/webjars/");
//    }

    @Bean
    public DispatcherServlet dispatcherServlet()
    {
        System.out.println("dispatcherServlet started");
        return new DispatcherServlet();
    }

}
