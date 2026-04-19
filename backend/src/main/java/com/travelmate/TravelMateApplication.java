package com.travelmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TravelMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelMateApplication.class, args);
        System.out.println("====== TravelMate Backend Started ======");
    }

}