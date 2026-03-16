package com.klasio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KlasioApplication {

    public static void main(String[] args) {
        SpringApplication.run(KlasioApplication.class, args);
    }
}
