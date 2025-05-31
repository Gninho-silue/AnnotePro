package com.example.annotationPlatform.intData;

import com.example.annotationPlatform.model.Role;
import com.example.annotationPlatform.model.User;
import com.example.annotationPlatform.repository.RoleRepository;
import com.example.annotationPlatform.repository.UserRepository;
import com.example.annotationPlatform.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Initializes the database with default roles and admin user.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initRoles();
        initAdmin();
    }

    /**
     * Initializes default roles if none exist.
     */
    private void initRoles() {
        if (roleRepository.count() == 0) {
            List<Role> roles = new ArrayList<>();

            Role adminRole = new Role();
            adminRole.setName(Constants.ROLE_ADMIN);
            roles.add(adminRole);

            Role annotatorRole = new Role();
            annotatorRole.setName(Constants.ROLE_ANNOTATOR);
            roles.add(annotatorRole);

            roleRepository.saveAll(roles);
            System.out.println("Roles initialized successfully");
        }
    }

    /**
     * Creates a default admin user if none exist.
     */
    private void initAdmin() {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName(Constants.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123")); // Change in production
            admin.setFirstName("Admin");
            admin.setLastName("User");
            admin.setEmail("admin@example.com");
            admin.setEnable(true);
            admin.setAccountLocked(false);
            admin.setCreateDate(LocalDateTime.now());
            admin.setRoles(Collections.singletonList(adminRole));

            userRepository.save(admin);
            System.out.println("Admin user created successfully");
        }
    }
}