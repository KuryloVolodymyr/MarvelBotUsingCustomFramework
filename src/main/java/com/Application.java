package com;

import com.service.ApiCaller;
import com.service.TemplateBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    @Bean
    public TemplateBuilder templateCreator(){
        return new TemplateBuilder();
    }

    @Bean
    public ApiCaller apiCaller(){
        return new ApiCaller();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
