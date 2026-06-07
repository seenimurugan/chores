package com.nila.chores.task;

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
 * TDD: Verifies that weeklyTarget + remindLeadDays fields are accepted on task create/update,
 * persisted, and returned in responses with correct defaults.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TaskReminderFieldsTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TaskRepository taskRepo;

    /** Injects an AuthUser principal with ADMIN role — matching the production JWT filter setup. */
    private RequestPostProcessor adminUser() {
        AuthUser principal = new AuthUser(1L, "admin", "Admin", Role.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return authentication(token);
    }

    @Test
    void createTask_withWeeklyTargetAndRemindLeadDays_persistsAndRoundTrips() throws Exception {
        var body = Map.of(
                "title", "Weekly Exercise",
                "points", 2,
                "recurrence", "WEEKLY",
                "weeklyTarget", 3,
                "remindLeadDays", 1
        );

        var result = mvc.perform(post("/api/admin/tasks")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyTarget").value(3))
                .andExpect(jsonPath("$.remindLeadDays").value(1))
                .andReturn();

        var responseMap = mapper.readValue(result.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        Task saved = taskRepo.findById((long) id).orElseThrow();
        assertThat(saved.getWeeklyTarget()).isEqualTo(3);
        assertThat(saved.getRemindLeadDays()).isEqualTo(1);
    }

    @Test
    void createTask_withoutOptionalFields_defaultsApplied() throws Exception {
        var body = Map.of(
                "title", "Daily Chore",
                "points", 1
        );

        var result = mvc.perform(post("/api/admin/tasks")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                // weeklyTarget is null = not tracked
                .andExpect(jsonPath("$.weeklyTarget").doesNotExist())
                // remindLeadDays defaults to 0
                .andExpect(jsonPath("$.remindLeadDays").value(0))
                .andReturn();

        var responseMap = mapper.readValue(result.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        Task saved = taskRepo.findById((long) id).orElseThrow();
        assertThat(saved.getWeeklyTarget()).isNull();
        assertThat(saved.getRemindLeadDays()).isEqualTo(0);
    }

    @Test
    void updateTask_weeklyTargetAndRemindLeadDays_roundTrips() throws Exception {
        // Create bare task first
        var createBody = Map.of("title", "Bare Task", "points", 1);
        var createResult = mvc.perform(post("/api/admin/tasks")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(createBody)))
                .andExpect(status().isOk())
                .andReturn();

        var responseMap = mapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        int id = (int) responseMap.get("id");

        // Now update with reminder fields
        var updateBody = Map.of(
                "title", "Updated Bare Task",
                "weeklyTarget", 2,
                "remindLeadDays", 1
        );
        mvc.perform(put("/api/admin/tasks/{id}", id)
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyTarget").value(2))
                .andExpect(jsonPath("$.remindLeadDays").value(1));

        Task saved = taskRepo.findById((long) id).orElseThrow();
        assertThat(saved.getWeeklyTarget()).isEqualTo(2);
        assertThat(saved.getRemindLeadDays()).isEqualTo(1);
    }

    @Test
    void createTask_withZeroWeeklyTarget_returns400() throws Exception {
        var body = Map.of(
                "title", "Invalid Target Task",
                "points", 1,
                "weeklyTarget", 0
        );

        mvc.perform(post("/api/admin/tasks")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_withNegativeRemindLeadDays_returns400() throws Exception {
        var body = Map.of(
                "title", "Negative Remind Task",
                "points", 1,
                "remindLeadDays", -1
        );

        mvc.perform(post("/api/admin/tasks")
                        .with(adminUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
