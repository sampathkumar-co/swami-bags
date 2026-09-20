package com.swamibags.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SwamiBagsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwamiBagsApplication.class, args);
    }
}
