package com.chatapp.server.config;

import com.chatapp.server.service.FileStorageService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqliteConfig {

    @Bean
    ApplicationRunner initializeStorage(FileStorageService fileStorageService) {
        return args -> fileStorageService.initialize();
    }
}
