package com.todolab.task.controller;

import com.todolab.auth.service.CurrentUserService;
import com.todolab.task.domain.Task;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.service.TaskService;
import com.todolab.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TaskV1ControllerTest {

    @Mock
    TaskService taskService;

    @Mock
    CurrentUserService currentUserService;

    @Test
    @DisplayName("v1 Task 생성은 JWT 사용자를 owner로 전달한다")
    void createTask_success_ownerAware() {
        TaskV1Controller controller = new TaskV1Controller(taskService, currentUserService);
        Jwt jwt = jwt("1");
        User owner = new User("owner@example.com", "encoded-password", "Owner");
        TaskRequest request = new TaskRequest("owned task", null, null, null, null, false);
        TaskResponse response = TaskResponse.from(Task.builder()
                .title("owned task")
                .owner(owner)
                .build());
        given(currentUserService.requireUser(jwt)).willReturn(owner);
        given(taskService.createForOwner(request, owner)).willReturn(response);

        var result = controller.createTask(jwt, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().data().title()).isEqualTo("owned task");
        then(currentUserService).should().requireUser(jwt);
        then(taskService).should().createForOwner(request, owner);
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
