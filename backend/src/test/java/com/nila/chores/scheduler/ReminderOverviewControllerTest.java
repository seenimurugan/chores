package com.nila.chores.scheduler;

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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD: Integration test for GET /api/admin/kids/{id}/reminder-overview.
 * ReminderOverviewService is mocked — controller wiring, auth, response shape only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReminderOverviewControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ReminderOverviewService overviewService;

    private RequestPostProcessor adminUser() {
        AuthUser principal = new AuthUser(1L, "admin", "Admin", Role.ADMIN);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return authentication(token);
    }

    private RequestPostProcessor kidUser() {
        AuthUser principal = new AuthUser(5L, "kid1", "Kid", Role.KID);
        var token = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_KID")));
        return authentication(token);
    }

    @Test
    void getOverview_adminUser_defaultPeriod_returnsRows() throws Exception {
        OffsetDateTime lastReminder = OffsetDateTime.parse("2024-01-07T09:30:00+00:00");
        ReminderOverviewService.ChoreOverviewRow row = new ReminderOverviewService.ChoreOverviewRow(
                10L, "Science video", "🔬", 2, 1, "AT_RISK", 1, 3, lastReminder);

        when(overviewService.getOverviewForKid(42L, "this-week")).thenReturn(List.of(row));

        mvc.perform(get("/api/admin/kids/42/reminder-overview").with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].choreId").value(10))
                .andExpect(jsonPath("$[0].choreTitle").value("Science video"))
                .andExpect(jsonPath("$[0].icon").value("🔬"))
                .andExpect(jsonPath("$[0].weeklyTarget").value(2))
                .andExpect(jsonPath("$[0].doneThisWeek").value(1))
                .andExpect(jsonPath("$[0].status").value("AT_RISK"))
                .andExpect(jsonPath("$[0].completionsInPeriod").value(1))
                .andExpect(jsonPath("$[0].remindersSentInPeriod").value(3))
                .andExpect(jsonPath("$[0].lastReminderInPeriod").isNotEmpty());
    }

    @Test
    void getOverview_withPeriodParam_passedToService() throws Exception {
        ReminderOverviewService.ChoreOverviewRow row = new ReminderOverviewService.ChoreOverviewRow(
                10L, "Reading", "📖", 3, 0, "ON_TRACK", 12, 0, null);

        when(overviewService.getOverviewForKid(42L, "last-month")).thenReturn(List.of(row));

        mvc.perform(get("/api/admin/kids/42/reminder-overview?period=last-month").with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].choreId").value(10))
                .andExpect(jsonPath("$[0].completionsInPeriod").value(12))
                .andExpect(jsonPath("$[0].remindersSentInPeriod").value(0))
                .andExpect(jsonPath("$[0].lastReminderInPeriod").isEmpty());
    }

    @Test
    void getOverview_kidUser_returns403() throws Exception {
        mvc.perform(get("/api/admin/kids/42/reminder-overview").with(kidUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOverview_emptyList_returnsEmptyArray() throws Exception {
        when(overviewService.getOverviewForKid(eq(99L), eq("this-week"))).thenReturn(List.of());

        mvc.perform(get("/api/admin/kids/99/reminder-overview").with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
