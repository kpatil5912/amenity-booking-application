package com.amenityhub.user.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runs the {@link DataSeederService} on startup. The actual seeding lives in a
 * separate {@code @Service} so its {@code @Transactional} boundary is applied
 * via the Spring proxy (a {@code CommandLineRunner.run()} is invoked directly
 * by the framework, so {@code @Transactional} on it would have no effect).
 */
@Component
public class RoleSeeder implements CommandLineRunner {

    private final DataSeederService dataSeederService;

    public RoleSeeder(DataSeederService dataSeederService) {
        this.dataSeederService = dataSeederService;
    }

    @Override
    public void run(String... args) {
        dataSeederService.seed();
    }
}
