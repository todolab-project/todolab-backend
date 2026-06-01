package com.todolab.task.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import com.todolab.task.domain.DeferReason;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TaskTxServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    DdayGoalRepository ddayGoalRepository;

    @Test
    @DisplayName("moveToTodayTx는 Task를 Today 상태로 변경하고 저장한다")
    void moveToTodayTx_success() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 5, 21);
        Task task = Task.builder()
                .title("task")
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.moveToTodayTx(id, targetDate);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.getTargetDate()).isEqualTo(targetDate);
        assertThat(result.getCompletedAt()).isNull();

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("completeTx는 Task를 Done 상태로 변경하고 저장한다")
    void completeTx_success() {
        // given
        long id = 1L;
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 21, 22, 0);
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 21))
                .carryOverCount(1)
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.completeTx(id, completedAt);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(result.getCompletedAt()).isEqualTo(completedAt);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("reopenTodayTx는 Done Task를 지정 날짜의 Today 상태로 되돌리고 저장한다")
    void reopenTodayTx_success() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 5, 21);
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.DONE)
                .completedAt(LocalDateTime.of(2026, 5, 21, 22, 0))
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.reopenTodayTx(id, targetDate);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.getTargetDate()).isEqualTo(targetDate);
        assertThat(result.getCompletedAt()).isNull();

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("carryOverTx는 Task를 다음 날짜의 Today 상태로 변경하고 저장한다")
    void carryOverTx_success() {
        // given
        long id = 1L;
        LocalDate nextDate = LocalDate.of(2026, 5, 22);
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 21))
                .carryOverCount(1)
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.carryOverTx(id, nextDate);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.getTargetDate()).isEqualTo(nextDate);
        assertThat(result.getCompletedAt()).isNull();
        assertThat(result.getCarryOverCount()).isEqualTo(2);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("상태 변경 대상 Task가 없으면 TaskNotFoundException이 발생한다")
    void changeStatus_notFound() {
        // given
        long id = 999L;
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);
        given(taskRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.moveToTodayTx(id, LocalDate.of(2026, 5, 21)))
                .isInstanceOf(TaskNotFoundException.class);

        then(taskRepository).should(times(1)).findById(id);
    }

    @Test
    @DisplayName("connectDdayGoalTx는 Task에 D-Day 목표를 연결하고 저장한다")
    void connectDdayGoalTx_success() {
        // given
        long id = 1L;
        long ddayGoalId = 10L;
        Task task = Task.builder()
                .title("기출 20문제 풀기")
                .build();
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(ddayGoalRepository.findById(ddayGoalId)).willReturn(Optional.of(goal));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.connectDdayGoalTx(id, ddayGoalId);

        // then
        assertThat(result.getDdayGoal()).isEqualTo(goal);

        then(taskRepository).should(times(1)).findById(id);
        then(ddayGoalRepository).should(times(1)).findById(ddayGoalId);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("connectDdayGoalTx는 D-Day 목표가 없으면 DdayGoalNotFoundException이 발생한다")
    void connectDdayGoalTx_fail_goalNotFound() {
        // given
        long id = 1L;
        long ddayGoalId = 99L;
        Task task = Task.builder()
                .title("기출 20문제 풀기")
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(ddayGoalRepository.findById(ddayGoalId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.connectDdayGoalTx(id, ddayGoalId))
                .isInstanceOf(DdayGoalNotFoundException.class);

        then(taskRepository).should(times(1)).findById(id);
        then(ddayGoalRepository).should(times(1)).findById(ddayGoalId);
    }

    @Test
    @DisplayName("disconnectDdayGoalTx는 Task의 D-Day 목표 연결을 해제하고 저장한다")
    void disconnectDdayGoalTx_success() {
        // given
        long id = 1L;
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));
        Task task = Task.builder()
                .title("기출 20문제 풀기")
                .ddayGoal(goal)
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.disconnectDdayGoalTx(id);

        // then
        assertThat(result.getDdayGoal()).isNull();

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("setDeferReasonTx는 Task에 미룬 이유를 저장한다")
    void setDeferReasonTx_success() {
        // given
        long id = 1L;
        Task task = Task.builder()
                .title("task")
                .carryOverCount(3)
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.setDeferReasonTx(id, DeferReason.TOO_BIG);

        // then
        assertThat(result.getDeferReason()).isEqualTo(DeferReason.TOO_BIG);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("clearDeferReasonTx는 Task의 미룬 이유를 해제한다")
    void clearDeferReasonTx_success() {
        // given
        long id = 1L;
        Task task = Task.builder()
                .title("task")
                .deferReason(DeferReason.TOO_BIG)
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.clearDeferReasonTx(id);

        // then
        assertThat(result.getDeferReason()).isNull();

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }
}
