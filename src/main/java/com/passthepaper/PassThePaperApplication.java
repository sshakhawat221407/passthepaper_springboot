package com.passthepaper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PassThePaperApplication {
    public static void main(String[] args) {
        SpringApplication.run(PassThePaperApplication.class, args);
    }
}
