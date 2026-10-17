package com.hospital.hospital_management.config;

import com.hospital.hospital_management.model.Role;
import com.hospital.hospital_management.model.User;
import com.hospital.hospital_management.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the initial system administrator account safely using environment variables.
 * Never prints passwords to logs or console output.
 */
@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_INITIAL_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_INITIAL_PASSWORD:}")
    private String adminPassword;

    @Autowired
    public SecurityDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String cleanUsername = adminUsername != null ? adminUsername.trim().toLowerCase() : "admin";

        if (!userRepository.existsByUsername(cleanUsername)) {
            if (adminPassword == null || adminPassword.trim().isEmpty()) {
                logger.warn("Initial administrator account '{}' was not created: ADMIN_INITIAL_PASSWORD is not configured in the environment.",
                        cleanUsername);
                return;
            }

            User admin = new User();
            admin.setUsername(cleanUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword.trim()));
            admin.setFullName("System Administrator");
            admin.setEmail("admin@hospital.local");
            admin.setRole(Role.ROLE_ADMIN);
            admin.setEnabled(true);

            userRepository.save(admin);
            logger.info("Initial administrator account '{}' provisioned successfully.", cleanUsername);
        }
    }
}
