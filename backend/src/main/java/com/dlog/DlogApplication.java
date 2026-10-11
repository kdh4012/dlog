package com.dlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(DlogApplication.class, args);
    }
}
