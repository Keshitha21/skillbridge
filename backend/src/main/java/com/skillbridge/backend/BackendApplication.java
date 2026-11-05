package com.skillbridge.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SkillBridge - Student Skill Exchange and Project Collaboration Platform.
 * REST API for the React frontend, backed by local MySQL.
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
