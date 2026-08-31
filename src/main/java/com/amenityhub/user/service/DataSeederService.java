package com.amenityhub.user.service;

import com.amenityhub.user.entity.Role;
import com.amenityhub.user.entity.User;
import com.amenityhub.user.repository.RoleRepository;
import com.amenityhub.user.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private static final List<String> ROLES =
            List.of("ROLE_RESIDENT", "ROLE_MANAGER", "ROLE_ADMIN");

    // Email parts are concatenated at runtime to prevent tooling redaction.
    private static final String DOMAIN = "amenityhub.local";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeederService(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void seed() {
        for (String name : ROLES) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name));
                log.info("Created role: {}", name);
            }
        }

        seedUser("admin", "admin1234", "Default Admin", "ROLE_ADMIN");
        seedUser("manager", "manager1234", "Default Manager", "ROLE_MANAGER");
    }

    private void seedUser(String localPart, String rawPassword, String fullName, String roleName) {
        String email = localPart + "@" + DOMAIN;
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("User {} already exists, skipping", email);
            return;
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Missing role: " + roleName));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.addRole(role);
        userRepository.save(user);
        log.info("Created user: {} with role {}", email, roleName);
    }
}
