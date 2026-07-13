package com.todolab.dday.controller;

import com.todolab.auth.service.CurrentUserService;
import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.dto.DdayGoalResponse;
import com.todolab.dday.service.DdayGoalService;
import com.todolab.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DdayGoalV1ControllerTest {

    @Mock
    DdayGoalService ddayGoalService;

    @Mock
    CurrentUserService currentUserService;

    @Test
    @DisplayName("v1 D-Day 목표 생성은 JWT 사용자를 owner로 전달한다")
    void create_success_ownerAware() {
        DdayGoalV1Controller controller = new DdayGoalV1Controller(ddayGoalService, currentUserService);
        Jwt jwt = jwt("1");
        User owner = new User("owner@example.com", "encoded-password", "Owner");
        DdayGoalRequest request = new DdayGoalRequest("정보처리기사", LocalDate.of(2026, 6, 10));
        DdayGoalResponse response = DdayGoalResponse.from(new DdayGoal(
                "정보처리기사",
                LocalDate.of(2026, 6, 10),
                owner
        ));
        given(currentUserService.requireUser(jwt)).willReturn(owner);
        given(ddayGoalService.createForOwner(request, owner)).willReturn(response);

        var result = controller.create(jwt, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().data().title()).isEqualTo("정보처리기사");
        then(currentUserService).should().requireUser(jwt);
        then(ddayGoalService).should().createForOwner(request, owner);
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
