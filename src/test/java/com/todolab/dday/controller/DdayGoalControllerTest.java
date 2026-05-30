package com.todolab.dday.controller;

import com.todolab.common.api.ApiExceptionHandler;
import com.todolab.common.api.ErrorCode;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.dto.DdayGoalResponse;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.service.DdayGoalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ApiExceptionHandler.class)
@WebMvcTest(controllers = DdayGoalController.class)
class DdayGoalControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DdayGoalService ddayGoalService;

    @Test
    @DisplayName("D-Day 목표 등록 성공")
    void create_success() throws Exception {
        DdayGoalRequest request = new DdayGoalRequest("정보처리기사", LocalDate.of(2026, 6, 10));
        DdayGoalResponse response = new DdayGoalResponse(
                1L,
                "정보처리기사",
                LocalDate.of(2026, 6, 10),
                11,
                LocalDateTime.of(2026, 5, 30, 10, 0)
        );

        given(ddayGoalService.create(any(DdayGoalRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/ddays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("정보처리기사"))
                .andExpect(jsonPath("$.data.targetDate").value("2026-06-10"))
                .andExpect(jsonPath("$.data.daysLeft").value(11));

        then(ddayGoalService).should().create(any(DdayGoalRequest.class));
        then(ddayGoalService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("D-Day 목표 등록 실패 - 제목은 필수다")
    void create_fail_titleMissing() throws Exception {
        DdayGoalRequest request = new DdayGoalRequest("", LocalDate.of(2026, 6, 10));

        mockMvc.perform(post("/api/ddays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()));

        then(ddayGoalService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("D-Day 목표 목록 조회 성공")
    void findAll_success() throws Exception {
        given(ddayGoalService.findAll()).willReturn(List.of(
                new DdayGoalResponse(1L, "포트폴리오 제출", LocalDate.of(2026, 6, 5), 6, null)
        ));

        mockMvc.perform(get("/api/ddays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("포트폴리오 제출"));

        then(ddayGoalService).should().findAll();
        then(ddayGoalService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("D-Day 목표 삭제 성공")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/ddays/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1));

        then(ddayGoalService).should().delete(1L);
        then(ddayGoalService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("D-Day 목표 삭제 실패 - 존재하지 않으면 404를 반환한다")
    void delete_fail_notFound() throws Exception {
        willThrow(new DdayGoalNotFoundException(99L)).given(ddayGoalService).delete(99L);

        mockMvc.perform(delete("/api/ddays/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.DDAY_GOAL_NOT_FOUND.getCode()));

        then(ddayGoalService).should().delete(99L);
        then(ddayGoalService).shouldHaveNoMoreInteractions();
    }
}
