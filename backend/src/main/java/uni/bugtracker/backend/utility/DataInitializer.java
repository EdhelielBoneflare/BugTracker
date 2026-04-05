package uni.bugtracker.backend.utility;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.security.model.Role;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final DeveloperRepository developerRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.default-admin.username}")
  private String adminUsername;

  @Value("${app.default-admin.password}")
  private String adminPassword;

  @Override
  public void run(String... args) {

    boolean exists = developerRepository
        .findByUsername(adminUsername)
        .isPresent();

    if (exists) {
      return;
    }

    Developer admin = Developer.builder()
        .username(adminUsername)
        .password(passwordEncoder.encode(adminPassword))
        .role(Role.ADMIN)
        .build();

    developerRepository.save(admin);

    log.info("Default admin created: {} / {}", adminUsername, adminPassword);
  }
}
