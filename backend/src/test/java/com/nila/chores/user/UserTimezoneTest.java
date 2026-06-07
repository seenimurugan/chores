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
 * TDD: Verifies per-kid timezone field — persists, round-trips on create/contacts-update,
 * defaults to Europe/London, and rejects invalid IANA zones with 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserTimezoneTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;

    private RequestPostProcessor adminUser() {
        AuthUser principal = new AuthUser(1L, "admin", "Admin", Role.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return authentication(token);
    }

    @Test
    void createKid_withTimezone_persistsAndRoundTrips() throws Exception {
        var body = Map.of(
                "username", "tztestcreate01",
                "password", "pass1234",
                "displayName", "TZ Create Kid",
                "timezone", "Asia/Kolkata"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));

        User saved = userRepo.findByUsername("tztestcreate01").orElseThrow();
        assertThat(saved.getTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void createKid_withoutTimezone_defaultsToEuropeLondon() throws Exception {
        var body = Map.of(
                "username", "tztestdefault01",
                "password", "pass1234",
                "displayName", "TZ Default Kid"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Europe/London"));

        User saved = userRepo.findByUsername("tztestdefault01").orElseThrow();
        assertThat(saved.getTimezone()).isEqualTo("Europe/London");
    }

    @Test
    void createKid_withInvalidTimezone_returns400() throws Exception {
        var body = Map.of(
                "username", "tztestinvalid01",
                "password", "pass1234",
                "displayName", "Bad TZ Kid",
                "timezone", "Mars/Phobos"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateContacts_withTimezone_persistsAndRoundTrips() throws Exception {
        // Create first without timezone (gets default)
        var body = Map.of(
                "username", "tztestupdate01",
                "password", "pass1234",
                "displayName", "TZ Update Kid"
        );
        var createResult = mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        var responseMap = mapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        // Update contacts including new timezone
        var patch = Map.of(
                "email", "tzupdate@example.com",
                "timezone", "America/New_York"
        );
        mvc.perform(patch("/api/admin/users/{id}/contacts", id)
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/New_York"));

        User saved = userRepo.findById((long) id).orElseThrow();
        assertThat(saved.getTimezone()).isEqualTo("America/New_York");
    }

    @Test
    void updateContacts_withInvalidTimezone_returns400() throws Exception {
        // Create a kid first
        var body = Map.of(
                "username", "tztestbadpatch01",
                "password", "pass1234",
                "displayName", "TZ Bad Patch Kid"
        );
        var createResult = mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        var responseMap = mapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        var patch = Map.of("timezone", "Not/AZone");
        mvc.perform(patch("/api/admin/users/{id}/contacts", id)
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isBadRequest());
    }
}
