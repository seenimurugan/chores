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

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD: Verifies per-kid reminderTime field — persists, round-trips on create +
 * contacts-update, defaults to 06:00, and rejects malformed times with 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserReminderTimeTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepo;

    private RequestPostProcessor adminUser() {
        AuthUser principal = new AuthUser(1L, "admin", "Admin", Role.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return authentication(token);
    }

    /** Create kid with an explicit reminderTime — persists and round-trips via GET. */
    @Test
    void createKid_withReminderTime_persistsAndRoundTrips() throws Exception {
        var body = Map.of(
                "username", "rtcreate01",
                "password", "pass1234",
                "displayName", "RT Create Kid",
                "reminderTime", "07:30"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderTime").value("07:30"));

        User saved = userRepo.findByUsername("rtcreate01").orElseThrow();
        assertThat(saved.getReminderTime()).isEqualTo(LocalTime.of(7, 30));
    }

    /** Create kid without reminderTime → defaults to 06:00. */
    @Test
    void createKid_withoutReminderTime_defaultsToSixAm() throws Exception {
        var body = Map.of(
                "username", "rtdefault01",
                "password", "pass1234",
                "displayName", "RT Default Kid"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderTime").value("06:00"));

        User saved = userRepo.findByUsername("rtdefault01").orElseThrow();
        assertThat(saved.getReminderTime()).isEqualTo(LocalTime.of(6, 0));
    }

    /** Invalid reminderTime format returns 400. */
    @Test
    void createKid_withInvalidReminderTime_returns400() throws Exception {
        var body = Map.of(
                "username", "rtbad01",
                "password", "pass1234",
                "displayName", "RT Bad Kid",
                "reminderTime", "25:99"
        );

        mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    /** PATCH contacts with a new reminderTime — persists and round-trips. */
    @Test
    void updateContacts_withReminderTime_persistsAndRoundTrips() throws Exception {
        // Create first
        var body = Map.of(
                "username", "rtupdate01",
                "password", "pass1234",
                "displayName", "RT Update Kid"
        );
        var createResult = mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        var responseMap = mapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        // Patch contacts with a new reminderTime
        var patch = Map.of(
                "email", "rtupdate@example.com",
                "reminderTime", "08:15"
        );
        mvc.perform(patch("/api/admin/users/{id}/contacts", id)
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderTime").value("08:15"));

        User saved = userRepo.findById((long) id).orElseThrow();
        assertThat(saved.getReminderTime()).isEqualTo(LocalTime.of(8, 15));
    }

    /** Omitting reminderTime from PATCH contacts keeps the kid's existing value. */
    @Test
    void updateContacts_withoutReminderTime_keepsExistingValue() throws Exception {
        // Create with a specific reminderTime
        var body = Map.of(
                "username", "rtkeep01",
                "password", "pass1234",
                "displayName", "RT Keep Kid",
                "reminderTime", "09:45"
        );
        var createResult = mvc.perform(post("/api/admin/users")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        var responseMap = mapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        // Patch contacts WITHOUT reminderTime — should keep 09:45
        var patch = Map.of("email", "rtkeep@example.com");
        mvc.perform(patch("/api/admin/users/{id}/contacts", id)
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderTime").value("09:45"));

        User saved = userRepo.findById((long) id).orElseThrow();
        assertThat(saved.getReminderTime()).isEqualTo(LocalTime.of(9, 45));
    }
}
