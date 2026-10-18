package com.faisal.outreach;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RecruiterOutreachApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RecruiterOutreachApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}
