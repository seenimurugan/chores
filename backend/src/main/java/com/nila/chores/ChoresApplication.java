package com.nila.chores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class ChoresApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChoresApplication.class, args);
    }
}
