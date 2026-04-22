package ru.example.otpcodes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OtpCodesApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtpCodesApplication.class, args);
    }
}
