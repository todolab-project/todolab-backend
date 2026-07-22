package com.todolab.task.controller;

import com.jayway.jsonpath.JsonPath;
import com.todolab.auth.service.JwtTokenService;
import com.todolab.mail.MailService;
import com.todolab.task.dto.TaskRequest;
import com.todolab.user.domain.User;
import com.todolab.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskV1IntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtTokenService jwtTokenService;

    @MockitoBean
    MailService mailService;

    @Test
    @DisplayName("v1 Task 생성 응답은 날짜 없는 Task 기본값과 nullable 필드를 반환한다")
    void create_unscheduledTask_responseDefaults() throws Exception {
        String accessToken = accessToken("task-inbox@example.com");
        TaskRequest request = new TaskRequest("인박스 정리", null, null, null, null, false);

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(notNullValue()))
                .andExpect(jsonPath("$.data.type").value("TODO"))
                .andExpect(jsonPath("$.data.title").value("인박스 정리"))
                .andExpect(jsonPath("$.data.description").isEmpty())
                .andExpect(jsonPath("$.data.startAt").isEmpty())
                .andExpect(jsonPath("$.data.endAt").isEmpty())
                .andExpect(jsonPath("$.data.allDay").value(false))
                .andExpect(jsonPath("$.data.unscheduled").value(true))
                .andExpect(jsonPath("$.data.category").isEmpty())
                .andExpect(jsonPath("$.data.status").value("INBOX"))
                .andExpect(jsonPath("$.data.plannedDate").isEmpty())
                .andExpect(jsonPath("$.data.targetDate").isEmpty())
                .andExpect(jsonPath("$.data.todayOrder").isEmpty())
                .andExpect(jsonPath("$.data.completedAt").isEmpty())
                .andExpect(jsonPath("$.data.carryOverCount").value(0))
                .andExpect(jsonPath("$.data.staleCarryOver").value(false))
                .andExpect(jsonPath("$.data.deferReason").isEmpty())
                .andExpect(jsonPath("$.data.deferReasonLabel").isEmpty())
                .andExpect(jsonPath("$.data.ddayGoalId").isEmpty())
                .andExpect(jsonPath("$.data.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.updatedAt").isEmpty());
    }

    @Test
    @DisplayName("v1 Task 생성 응답은 날짜 있는 Task를 Today로 저장하고 날짜 규칙을 반환한다")
    void create_scheduledTask_responseDateRules() throws Exception {
        String accessToken = accessToken("task-schedule@example.com");
        TaskRequest request = new TaskRequest(
                "출시 회의",
                "릴리스 범위 확인",
                LocalDateTime.of(2026, 7, 22, 9, 0),
                LocalDateTime.of(2026, 7, 22, 10, 0),
                "업무",
                false
        );

        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("SCHEDULE"))
                .andExpect(jsonPath("$.data.description").value("릴리스 범위 확인"))
                .andExpect(jsonPath("$.data.startAt").value("2026-07-22T09:00:00"))
                .andExpect(jsonPath("$.data.endAt").value("2026-07-22T10:00:00"))
                .andExpect(jsonPath("$.data.allDay").value(false))
                .andExpect(jsonPath("$.data.unscheduled").value(false))
                .andExpect(jsonPath("$.data.category").value("업무"))
                .andExpect(jsonPath("$.data.status").value("TODAY"))
                .andExpect(jsonPath("$.data.plannedDate").value("2026-07-22"))
                .andExpect(jsonPath("$.data.targetDate").value("2026-07-22"))
                .andExpect(jsonPath("$.data.todayOrder").isEmpty())
                .andExpect(jsonPath("$.data.completedAt").isEmpty())
                .andExpect(jsonPath("$.data.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.updatedAt").isEmpty());
    }

    @Test
    @DisplayName("v1 MONTH Task 조회는 YYYY-MM을 바인딩하고 owner 범위 월간 일정을 반환한다")
    void getTasks_month_success_yearMonthBindingAndOwnerScope() throws Exception {
        String ownerToken = accessToken("task-month-owner@example.com");
        String otherOwnerToken = accessToken("task-month-other@example.com");

        createTask(ownerToken, new TaskRequest(
                "7월 일정",
                null,
                LocalDateTime.of(2026, 7, 1, 9, 0),
                LocalDateTime.of(2026, 7, 1, 10, 0),
                null,
                false
        ));
        createTask(ownerToken, new TaskRequest(
                "월말 걸침",
                null,
                LocalDateTime.of(2026, 7, 31, 23, 0),
                LocalDateTime.of(2026, 8, 1, 1, 0),
                null,
                false
        ));
        createTask(ownerToken, new TaskRequest(
                "8월 일정",
                null,
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                null,
                false
        ));
        createTask(otherOwnerToken, new TaskRequest(
                "다른 사용자 7월 일정",
                null,
                LocalDateTime.of(2026, 7, 2, 9, 0),
                LocalDateTime.of(2026, 7, 2, 10, 0),
                null,
                false
        ));

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("type", "MONTH")
                        .param("taskType", "SCHEDULE")
                        .param("date", "2026-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("7월 일정"))
                .andExpect(jsonPath("$.data[0].startAt").value("2026-07-01T09:00:00"))
                .andExpect(jsonPath("$.data[1].title").value("월말 걸침"))
                .andExpect(jsonPath("$.data[1].startAt").value("2026-07-31T23:00:00"));
    }

    @Test
    @DisplayName("v1 MONTH Task 조회는 YYYY-MM-DD 형식을 거부한다")
    void getTasks_month_fail_invalidDateFormat() throws Exception {
        String accessToken = accessToken("task-month-invalid@example.com");

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("type", "MONTH")
                        .param("taskType", "SCHEDULE")
                        .param("date", "2026-07-22"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"));
    }

    @Test
    @DisplayName("v1 Task 삭제 응답은 data null envelope를 반환한다")
    void deleteTask_success_dataNull() throws Exception {
        String accessToken = accessToken("task-delete@example.com");
        Long taskId = createTask(accessToken, new TaskRequest("삭제 대상", null, null, null, null, false));

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }

    private String accessToken(String email) {
        User owner = userRepository.save(new User(email, "encoded-password", "Task 사용자"));
        return jwtTokenService.createAccessToken(owner).tokenValue();
    }

    private Long createTask(String accessToken, TaskRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number id = JsonPath.read(response, "$.data.id");
        return id.longValue();
    }
}
