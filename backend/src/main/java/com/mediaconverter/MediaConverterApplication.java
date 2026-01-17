package com.mediaconverter;

import com.mediaconverter.config.CustomBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MediaConverterApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MediaConverterApplication.class);
        app.setBanner(new CustomBanner());
        app.run(args);
    }
}