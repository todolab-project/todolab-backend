package com.todolab.dday.controller;

import com.todolab.auth.service.JwtTokenService;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.mail.MailService;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DdayGoalV1IntegrationTest {

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
    @DisplayName("v1 D-Day 목표 생성 응답은 nullable 필드 없이 계약 필드를 반환한다")
    void create_responseFields_nonNull() throws Exception {
        User owner = userRepository.save(new User("dday-v1@example.com", "encoded-password", "D-Day 사용자"));
        String accessToken = jwtTokenService.createAccessToken(owner).tokenValue();
        DdayGoalRequest request = new DdayGoalRequest("출시", LocalDate.of(2026, 8, 1));

        mockMvc.perform(post("/api/v1/dday-goals")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(notNullValue()))
                .andExpect(jsonPath("$.data.title").value("출시"))
                .andExpect(jsonPath("$.data.targetDate").value("2026-08-01"))
                .andExpect(jsonPath("$.data.daysLeft").value(notNullValue()))
                .andExpect(jsonPath("$.data.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
    }
}
