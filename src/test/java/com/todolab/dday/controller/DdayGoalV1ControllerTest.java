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
import java.util.List;

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

    @Test
    @DisplayName("v1 D-Day 목표 목록 조회는 owner 범위 서비스를 호출한다")
    void findAll_success_ownerScoped() {
        DdayGoalV1Controller controller = new DdayGoalV1Controller(ddayGoalService, currentUserService);
        Jwt jwt = jwt("1");
        User owner = owner();
        DdayGoalResponse response = DdayGoalResponse.from(new DdayGoal(
                "정보처리기사",
                LocalDate.of(2026, 6, 10),
                owner
        ));
        given(currentUserService.requireUser(jwt)).willReturn(owner);
        given(ddayGoalService.findAllForOwner(owner)).willReturn(List.of(response));

        var result = controller.findAll(jwt);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().data()).hasSize(1);
        then(ddayGoalService).should().findAllForOwner(owner);
    }

    @Test
    @DisplayName("v1 D-Day 연결 Task 조회는 owner 범위 서비스를 호출한다")
    void findTasks_success_ownerScoped() {
        DdayGoalV1Controller controller = new DdayGoalV1Controller(ddayGoalService, currentUserService);
        Jwt jwt = jwt("1");
        User owner = owner();
        given(currentUserService.requireUser(jwt)).willReturn(owner);
        given(ddayGoalService.findTasksForOwner(10L, owner)).willReturn(List.of());

        var result = controller.findTasks(jwt, 10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().data()).isEmpty();
        then(ddayGoalService).should().findTasksForOwner(10L, owner);
    }

    @Test
    @DisplayName("v1 D-Day 목표 삭제는 owner 범위 서비스를 호출하고 null data를 반환한다")
    void delete_success_ownerScoped() {
        DdayGoalV1Controller controller = new DdayGoalV1Controller(ddayGoalService, currentUserService);
        Jwt jwt = jwt("1");
        User owner = owner();
        given(currentUserService.requireUser(jwt)).willReturn(owner);

        var result = controller.delete(jwt, 10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().data()).isNull();
        then(ddayGoalService).should().deleteForOwner(10L, owner);
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }

    private User owner() {
        return new User("owner@example.com", "encoded-password", "Owner");
    }
}
