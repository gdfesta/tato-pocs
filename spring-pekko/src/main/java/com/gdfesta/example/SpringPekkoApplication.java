package com.gdfesta.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.gdfesta.example", "com.gdfesta.springboot.pekko"})
public class SpringPekkoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringPekkoApplication.class, args);
    }
}
