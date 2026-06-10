package com.todolab.task.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import com.todolab.task.domain.DeferReason;
import com.todolab.task.domain.ScheduleSource;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.domain.TaskType;
import com.todolab.task.dto.TaskRequest;
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
    @DisplayName("updateTx는 날짜 없는 Today Task의 설명을 수정해도 실행 상태를 유지한다")
    void updateTx_unscheduledToday_preservesWorkflowState() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 6, 9);
        Task task = Task.builder()
                .title("기존 제목")
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .build();
        TaskRequest request = new TaskRequest(
                "기존 제목",
                "새 설명",
                TaskType.TODO,
                null,
                null,
                null,
                false
        );
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.updateTx(id, request);

        // then
        assertThat(result.getDescription()).isEqualTo("새 설명");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.getTargetDate()).isEqualTo(targetDate);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("moveToTodayTx는 일정 없는 Task에 자동 종일 일정을 만들고 저장한다")
    void moveToTodayTx_createsAutoAllDaySchedule() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 5, 21);
        Task task = Task.builder()
                .title("task")
                .type(TaskType.TODO)
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
        assertThat(result.getStartAt()).isEqualTo(targetDate.atStartOfDay());
        assertThat(result.getEndAt()).isEqualTo(targetDate.plusDays(1).atStartOfDay());
        assertThat(result.isAllDay()).isTrue();
        assertThat(result.getScheduleSource()).isEqualTo(ScheduleSource.AUTO_TODAY);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("moveToTodayTx는 기존 수동 일정을 변경하지 않는다")
    void moveToTodayTx_preservesUserSchedule() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 5, 21);
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 20, 10, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 5, 20, 11, 0);
        Task task = Task.builder()
                .title("task")
                .startAt(startAt)
                .endAt(endAt)
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.moveToTodayTx(id, targetDate);

        // then
        assertThat(result.getTargetDate()).isEqualTo(targetDate);
        assertThat(result.getStartAt()).isEqualTo(startAt);
        assertThat(result.getEndAt()).isEqualTo(endAt);
        assertThat(result.getScheduleSource()).isEqualTo(ScheduleSource.USER);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("moveToInboxTx는 자동 종일 일정을 제거하고 기록함으로 이동한다")
    void moveToInboxTx_removesAutoTodaySchedule() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 6, 11);
        Task task = Task.builder()
                .title("task")
                .type(TaskType.TODO)
                .startAt(targetDate.atStartOfDay())
                .endAt(targetDate.plusDays(1).atStartOfDay())
                .allDay(true)
                .scheduleSource(ScheduleSource.AUTO_TODAY)
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.moveToInboxTx(id, false);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.INBOX);
        assertThat(result.getTargetDate()).isNull();
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getScheduleSource()).isNull();

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("moveToInboxTx는 요청에 따라 사용자 지정 일정도 제거한다")
    void moveToInboxTx_removesUserScheduleWhenRequested() {
        // given
        long id = 1L;
        Task task = Task.builder()
                .title("task")
                .type(TaskType.SCHEDULE)
                .startAt(LocalDateTime.of(2026, 6, 11, 14, 0))
                .endAt(LocalDateTime.of(2026, 6, 11, 15, 0))
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 6, 11))
                .build();
        TaskTxService service = new TaskTxService(taskRepository, ddayGoalRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.moveToInboxTx(id, true);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.INBOX);
        assertThat(result.getStartAt()).isNull();
        assertThat(result.getEndAt()).isNull();
        assertThat(result.getType()).isEqualTo(TaskType.TODO);

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
    @DisplayName("carryOverTx는 자동 종일 일정과 실행일을 함께 다음 날짜로 이동한다")
    void carryOverTx_movesAutoTodaySchedule() {
        // given
        long id = 1L;
        LocalDate currentDate = LocalDate.of(2026, 5, 21);
        LocalDate nextDate = LocalDate.of(2026, 5, 22);
        Task task = Task.builder()
                .title("task")
                .startAt(currentDate.atStartOfDay())
                .endAt(currentDate.plusDays(1).atStartOfDay())
                .allDay(true)
                .scheduleSource(ScheduleSource.AUTO_TODAY)
                .status(TaskStatus.TODAY)
                .targetDate(currentDate)
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
        assertThat(result.getStartAt()).isEqualTo(nextDate.atStartOfDay());
        assertThat(result.getEndAt()).isEqualTo(nextDate.plusDays(1).atStartOfDay());
        assertThat(result.getScheduleSource()).isEqualTo(ScheduleSource.AUTO_TODAY);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("carryOverTx는 사용자 지정 캘린더 일정을 변경하지 않는다")
    void carryOverTx_preservesUserSchedule() {
        // given
        long id = 1L;
        LocalDate nextDate = LocalDate.of(2026, 5, 22);
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 21, 10, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 5, 21, 11, 0);
        Task task = Task.builder()
                .title("task")
                .startAt(startAt)
                .endAt(endAt)
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
        assertThat(result.getTargetDate()).isEqualTo(nextDate);
        assertThat(result.getStartAt()).isEqualTo(startAt);
        assertThat(result.getEndAt()).isEqualTo(endAt);
        assertThat(result.getScheduleSource()).isEqualTo(ScheduleSource.USER);

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
