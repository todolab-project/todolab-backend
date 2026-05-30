package com.todolab.dday.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DdayGoalServiceTest {

    @Mock
    DdayGoalRepository ddayGoalRepository;

    @Mock
    TaskRepository taskRepository;

    @Test
    @DisplayName("D-Day 목표를 생성한다")
    void create_success() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository, taskRepository);
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));
        given(ddayGoalRepository.save(org.mockito.ArgumentMatchers.any(DdayGoal.class))).willReturn(goal);

        var response = service.create(new DdayGoalRequest("정보처리기사", LocalDate.of(2026, 6, 10)));

        assertThat(response.title()).isEqualTo("정보처리기사");
        assertThat(response.targetDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        then(ddayGoalRepository).should().save(org.mockito.ArgumentMatchers.any(DdayGoal.class));
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("D-Day 목표를 날짜순으로 조회한다")
    void findAll_success() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository, taskRepository);
        given(ddayGoalRepository.findAllByOrderByTargetDateAscIdAsc()).willReturn(List.of(
                new DdayGoal("포트폴리오 제출", LocalDate.of(2026, 6, 5))
        ));

        var responses = service.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("포트폴리오 제출");
        then(ddayGoalRepository).should().findAllByOrderByTargetDateAscIdAsc();
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("D-Day 목표에 연결된 Task를 조회한다")
    void findTasks_success() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository, taskRepository);
        long ddayGoalId = 1L;
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));
        Task task = Task.builder()
                .title("기출 20문제 풀기")
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 30))
                .ddayGoal(goal)
                .build();

        given(ddayGoalRepository.existsById(ddayGoalId)).willReturn(true);
        given(taskRepository.findByDdayGoalId(ddayGoalId)).willReturn(List.of(task));

        var responses = service.findTasks(ddayGoalId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("기출 20문제 풀기");
        assertThat(responses.getFirst().status()).isEqualTo(TaskStatus.TODAY);
        then(ddayGoalRepository).should().existsById(ddayGoalId);
        then(taskRepository).should().findByDdayGoalId(ddayGoalId);
    }

    @Test
    @DisplayName("존재하지 않는 D-Day 목표의 Task 조회 시 예외를 던진다")
    void findTasks_fail_notFound() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository, taskRepository);
        given(ddayGoalRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.findTasks(99L))
                .isInstanceOf(DdayGoalNotFoundException.class);

        then(ddayGoalRepository).should().existsById(99L);
        then(taskRepository).should(never()).findByDdayGoalId(99L);
    }

    @Test
    @DisplayName("존재하지 않는 D-Day 목표 삭제 시 예외를 던진다")
    void delete_fail_notFound() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository, taskRepository);
        given(ddayGoalRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(DdayGoalNotFoundException.class);

        then(ddayGoalRepository).should().existsById(99L);
        then(taskRepository).shouldHaveNoInteractions();
    }
}
