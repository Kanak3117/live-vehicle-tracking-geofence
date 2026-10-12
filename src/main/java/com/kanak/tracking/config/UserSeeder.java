package com.kanak.tracking.config;

import com.kanak.tracking.domain.model.Role;
import com.kanak.tracking.domain.model.UserAccount;
import com.kanak.tracking.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tracking.seed.enabled", havingValue = "true")
public class UserSeeder implements CommandLineRunner {

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;

    public UserSeeder(UserAccountRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        create("admin", "admin123", Role.ADMIN);
        create("employee", "employee123", Role.EMPLOYEE);
        create("device", "device123", Role.DEVICE);
    }

    private void create(String username, String password, Role role) {
        UserAccount u = new UserAccount();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(password));          // BCrypt, never stored in plain text
        u.setRole(role);
        users.save(u);
    }
}