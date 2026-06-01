package com.nila.chores.config;

import com.nila.chores.user.User;
import com.nila.chores.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    @Bean
    public ApplicationRunner ensureAdmin(UserRepository users,
                                         PasswordEncoder encoder,
                                         @Value("${chores.admin.username}") String username,
                                         @Value("${chores.admin.password}") String password,
                                         @Value("${chores.admin.display-name}") String displayName) {
        return args -> {
            boolean hasAdmin = users.findAll().stream().anyMatch(u -> u.getRole() == User.Role.ADMIN);
            if (hasAdmin) {
                log.info("event=bootstrap.admin outcome=already-present");
                return;
            }
            User admin = new User();
            admin.setUsername(username);
            admin.setPasswordHash(encoder.encode(password));
            admin.setDisplayName(displayName);
            admin.setRole(User.Role.ADMIN);
            admin.setAvatarColor("#4263eb");
            users.save(admin);
            log.info("event=bootstrap.admin outcome=created username={} message='Change the password immediately if using the default.'",
                    username);
        };
    }
}
