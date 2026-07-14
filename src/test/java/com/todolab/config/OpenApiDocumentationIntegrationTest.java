package com.todolab.config;

import com.todolab.mail.MailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.batch.job.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MailService mailService;

    @Test
    @DisplayName("OpenAPI JSON에 v1 Task 문서 tag, summary, security, 오류 응답 schema가 노출된다")
    void apiDocs_v1TaskDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.schemas.ApiResponse.description").value("공통 API 응답 envelope"))
                .andExpect(jsonPath("$.components.schemas.ErrorBody.description").value("공통 API 오류 응답 본문"))
                .andExpect(jsonPath("$.paths['/api/v1/tasks'].get.tags[0]").value("v1 Task"))
                .andExpect(jsonPath("$.paths['/api/v1/tasks'].get.summary").value("Task 범위 조회"))
                .andExpect(jsonPath("$.paths['/api/v1/tasks'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/tasks'].get.responses['401'].description").value("인증 필요"))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/tasks'].get.responses['401'].content['application/json'].schema.$ref"
                ).exists());
    }

    @Test
    @DisplayName("OpenAPI JSON에 v1 Auth와 D-Day 문서 tag와 summary가 노출된다")
    void apiDocs_v1AuthAndDdayDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.tags[0]").value("v1 Auth"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.summary").value("로그인"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/dday-goals'].post.tags[0]").value("v1 D-Day"))
                .andExpect(jsonPath("$.paths['/api/v1/dday-goals'].post.summary").value("D-Day 목표 생성"))
                .andExpect(jsonPath("$.paths['/api/v1/dday-goals'].post.responses['404'].description")
                        .value("D-Day 목표 없음"));
    }
}
