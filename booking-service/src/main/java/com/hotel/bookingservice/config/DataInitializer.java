package com.hotel.bookingservice.config;

import com.hotel.bookingservice.entity.Role;
import com.hotel.bookingservice.entity.User;
import com.hotel.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Initializing user data...");

        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Admin user created: username=admin, password=admin123");
        }

        if (!userRepository.existsByUsername("user1")) {
            User user = User.builder()
                    .username("user1")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
            log.info("Test user created: username=user1, password=password123");
        }

        if (!userRepository.existsByUsername("user2")) {
            User user = User.builder()
                    .username("user2")
                    .password(passwordEncoder.encode("password123"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
            log.info("Test user created: username=user2, password=password123");
        }

        log.info("Data initialization completed.");
    }
}