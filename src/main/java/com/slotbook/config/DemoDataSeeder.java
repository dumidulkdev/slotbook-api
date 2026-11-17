package com.slotbook.config;

import com.slotbook.domain.Role;
import com.slotbook.domain.UserEntity;
import com.slotbook.domain.WorkingWindowEmbeddable;
import com.slotbook.domain.ProviderAvailabilityEntity;
import com.slotbook.repository.ProviderAvailabilityRepository;
import com.slotbook.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("local")
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoUsers(UserRepository userRepository,
                                    ProviderAvailabilityRepository availabilityRepository,
                                    PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.existsByEmail("dana.provider@slotbook.dev")) {
                return;
            }
            UserEntity provider = userRepository.save(new UserEntity(
                    "Dana Barber",
                    "dana.provider@slotbook.dev",
                    passwordEncoder.encode("Provider123!"),
                    Role.PROVIDER));
            userRepository.save(new UserEntity(
                    "Chris Client",
                    "chris.client@slotbook.dev",
                    passwordEncoder.encode("Client123!"),
                    Role.CLIENT));

            List<WorkingWindowEmbeddable> windows = List.of(
                    new WorkingWindowEmbeddable(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    new WorkingWindowEmbeddable(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    new WorkingWindowEmbeddable(DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    new WorkingWindowEmbeddable(DayOfWeek.THURSDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                    new WorkingWindowEmbeddable(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)));
            availabilityRepository.save(new ProviderAvailabilityEntity(provider, 30, windows));
        };
    }
}