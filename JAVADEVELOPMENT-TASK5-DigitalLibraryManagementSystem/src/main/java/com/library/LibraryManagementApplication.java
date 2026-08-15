package com.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Digital Library Management System
 * Admin module: manage books, issued records, members, fines
 * User module: browse/search catalogue, issue/return books, reservations, contact form
 */
@SpringBootApplication
@EnableScheduling
public class LibraryManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
