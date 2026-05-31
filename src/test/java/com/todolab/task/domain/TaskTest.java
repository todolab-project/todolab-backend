package com.todolab.task.domain;

import com.todolab.dday.domain.DdayGoal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    @Test
    @DisplayName("moveToInbox는 Inbox 상태로 바꾸고 실행 날짜와 완료 시간을 비운다")
    void moveToInbox() {
        // given
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.DONE)
                .targetDate(LocalDate.of(2026, 5, 20))
                .completedAt(LocalDateTime.of(2026, 5, 20, 10, 30))
                .build();

        // when
        task.moveToInbox();

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.INBOX);
        assertThat(task.getTargetDate()).isNull();
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("moveToToday는 Today 상태로 바꾸고 실행 날짜를 지정한다")
    void moveToToday() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();
        LocalDate targetDate = LocalDate.of(2026, 5, 20);

        // when
        task.moveToToday(targetDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(targetDate);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("complete는 Done 상태로 바꾸고 완료 시간을 기록한다")
    void complete() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 20, 11, 0);

        // when
        task.complete(completedAt);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    @DisplayName("carryOverTo는 Today 상태를 유지하고 실행 날짜를 다음 날짜로 바꾼다")
    void carryOverTo() {
        // given
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.DONE)
                .targetDate(LocalDate.of(2026, 5, 20))
                .completedAt(LocalDateTime.of(2026, 5, 20, 21, 0))
                .build();
        LocalDate nextDate = LocalDate.of(2026, 5, 21);

        // when
        task.carryOverTo(nextDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(nextDate);
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("moveToToday는 targetDate가 없으면 실패한다")
    void moveToToday_requiresTargetDate() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();

        // when & then
        assertThatThrownBy(() -> task.moveToToday(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetDate는 필수입니다.");
    }

    @Test
    @DisplayName("complete는 completedAt이 없으면 실패한다")
    void complete_requiresCompletedAt() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();

        // when & then
        assertThatThrownBy(() -> task.complete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("completedAt은 필수입니다.");
    }

    @Test
    @DisplayName("connectDdayGoal은 D-Day 목표를 연결한다")
    void connectDdayGoal() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));

        // when
        task.connectDdayGoal(goal);

        // then
        assertThat(task.getDdayGoal()).isEqualTo(goal);
    }

    @Test
    @DisplayName("connectDdayGoal은 D-Day 목표가 없으면 실패한다")
    void connectDdayGoal_requiresGoal() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();

        // when & then
        assertThatThrownBy(() -> task.connectDdayGoal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ddayGoal은 필수입니다.");
    }

    @Test
    @DisplayName("disconnectDdayGoal은 D-Day 목표 연결을 해제한다")
    void disconnectDdayGoal() {
        // given
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));
        Task task = Task.builder()
                .title("task")
                .ddayGoal(goal)
                .build();

        // when
        task.disconnectDdayGoal();

        // then
        assertThat(task.getDdayGoal()).isNull();
    }
}
