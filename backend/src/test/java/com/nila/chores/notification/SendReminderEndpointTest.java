package com.nila.chores.notification;

import com.nila.chores.security.AuthUser;
import com.nila.chores.user.User.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD: Integration test for POST /api/admin/tasks/{id}/send-reminder.
 * NotificationService is mocked — this test covers the controller wiring only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SendReminderEndpointTest {

    @Autowired MockMvc mvc;
    @MockBean NotificationService notificationService;

    private RequestPostProcessor adminUser() {
        AuthUser principal = new AuthUser(1L, "admin", "Admin", Role.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return authentication(token);
    }

    private RequestPostProcessor kidUser() {
        AuthUser principal = new AuthUser(5L, "kid1", "Kid One", Role.KID);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_KID")));
        return authentication(token);
    }

    @Test
    void sendReminder_adminUser_returnsResultList() throws Exception {
        when(notificationService.sendReminderForTask(42L)).thenReturn(List.of(
                new NotificationService.ReminderResult(10L, "Ammu", "EMAIL", true, false, null),
                new NotificationService.ReminderResult(10L, "Ammu", "TELEGRAM", true, false, null)
        ));

        mvc.perform(post("/api/admin/tasks/42/send-reminder").with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kidName").value("Ammu"))
                .andExpect(jsonPath("$[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$[0].sent").value(true))
                .andExpect(jsonPath("$[1].channel").value("TELEGRAM"));
    }

    @Test
    void sendReminder_kidUser_returns403() throws Exception {
        mvc.perform(post("/api/admin/tasks/42/send-reminder").with(kidUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendReminder_taskNotFound_returns404() throws Exception {
        when(notificationService.sendReminderForTask(999L))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Task not found"));

        mvc.perform(post("/api/admin/tasks/999/send-reminder").with(adminUser()))
                .andExpect(status().isNotFound());
    }
}
