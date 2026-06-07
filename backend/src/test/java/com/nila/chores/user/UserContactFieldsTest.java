package com.nila.chores.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nila.chores.security.AuthUser;
import com.nila.chores.user.User.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD: Verifies that email + telegramChatId fields are accepted on kid create,
 * persisted to the DB, and returned in list/create responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserContactFieldsTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;

    /** Injects an AuthUser principal with ADMIN role — matching the production JWT filter setup. */
    private RequestPostProcessor adminUser() {
        AuthUser principal = new AuthUser(1L, "admin", "Admin", Role.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return authentication(token);
    }

    @Test
    void createKid_withEmailAndTelegramChatId_persistsAndRoundTrips() throws Exception {
        var body = Map.of(
                "username", "testcontact01",
                "password", "pass1234",
                "displayName", "Contact Kid",
                "avatarColor", "#4263eb",
                "email", "kid@example.com",
                "telegramChatId", 123456789L
        );

        var result = mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("kid@example.com"))
                .andExpect(jsonPath("$.telegramChatId").value(123456789))
                .andReturn();

        // Verify persistence via repo
        User saved = userRepo.findByUsername("testcontact01").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("kid@example.com");
        assertThat(saved.getTelegramChatId()).isEqualTo(123456789L);
    }

    @Test
    void createKid_withoutContactFields_defaultsToNull() throws Exception {
        var body = Map.of(
                "username", "testcontact02",
                "password", "pass1234",
                "displayName", "Minimal Kid"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telegramChatId").doesNotExist());

        User saved = userRepo.findByUsername("testcontact02").orElseThrow();
        assertThat(saved.getEmail()).isNull();
        assertThat(saved.getTelegramChatId()).isNull();
    }

    @Test
    void createKid_withInvalidEmail_returns400() throws Exception {
        var body = Map.of(
                "username", "testcontact03",
                "password", "pass1234",
                "displayName", "Bad Email Kid",
                "email", "not-an-email"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateKidContacts_persistsAndRoundTrips() throws Exception {
        // Create first
        var body = Map.of(
                "username", "testcontact04",
                "password", "pass1234",
                "displayName", "Update Kid"
        );
        var createResult = mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        var responseMap = mapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        // Now update contacts
        var patch = Map.of(
                "email", "updated@example.com",
                "telegramChatId", 999888777L
        );
        mvc.perform(patch("/api/admin/users/{id}/contacts", id)
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.telegramChatId").value(999888777));

        User saved = userRepo.findById((long) id).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("updated@example.com");
        assertThat(saved.getTelegramChatId()).isEqualTo(999888777L);
    }
}
