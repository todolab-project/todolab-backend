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
    @DisplayName("새 Task의 이월 횟수 기본값은 0이다")
    void carryOverCount_defaultZero() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();

        // then
        assertThat(task.getCarryOverCount()).isZero();
    }

    @Test
    @DisplayName("이월 횟수가 3회 이상이면 다시 정리 대상이다")
    void isStaleCarryOver() {
        // given
        Task stale = Task.builder()
                .title("stale")
                .carryOverCount(3)
                .build();
        Task active = Task.builder()
                .title("active")
                .carryOverCount(2)
                .build();

        // then
        assertThat(stale.isStaleCarryOver()).isTrue();
        assertThat(active.isStaleCarryOver()).isFalse();
    }

    @Test
    @DisplayName("음수 이월 횟수는 0으로 보정한다")
    void carryOverCount_normalizesNegative() {
        // given
        Task task = Task.builder()
                .title("task")
                .carryOverCount(-1)
                .build();

        // then
        assertThat(task.getCarryOverCount()).isZero();
    }

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
    @DisplayName("자동 종일 Task를 기록함으로 이동하면 캘린더 일정도 제거한다")
    void moveToInbox_removesAutoTodaySchedule() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 11);
        Task task = Task.builder()
                .title("task")
                .type(TaskType.TODO)
                .startAt(targetDate.atStartOfDay())
                .endAt(targetDate.plusDays(1).atStartOfDay())
                .allDay(true)
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .build();

        // when
        task.moveToInbox();

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.INBOX);
        assertThat(task.getTargetDate()).isNull();
        assertThat(task.getStartAt()).isNull();
        assertThat(task.getEndAt()).isNull();
        assertThat(task.isAllDay()).isFalse();
        assertThat(task.getType()).isEqualTo(TaskType.TODO);
    }

    @Test
    @DisplayName("사용자 지정 일정도 기록함으로 이동하면 제거한다")
    void moveToInbox_removesUserSchedule() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 11, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 6, 11, 15, 0);
        Task task = Task.builder()
                .title("task")
                .startAt(startAt)
                .endAt(endAt)
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 6, 11))
                .build();

        // when
        task.moveToInbox();

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.INBOX);
        assertThat(task.getTargetDate()).isNull();
        assertThat(task.getStartAt()).isNull();
        assertThat(task.getEndAt()).isNull();
        assertThat(task.isAllDay()).isFalse();
        assertThat(task.getType()).isEqualTo(TaskType.TODO);
    }

    @Test
    @DisplayName("일정 없는 Task를 Today로 이동하면 실행 날짜의 자동 종일 일정을 만든다")
    void moveToToday_createsAutoAllDaySchedule() {
        // given
        Task task = Task.builder()
                .title("task")
                .type(TaskType.TODO)
                .build();
        LocalDate targetDate = LocalDate.of(2026, 5, 20);

        // when
        task.moveToToday(targetDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(targetDate);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getStartAt()).isEqualTo(targetDate.atStartOfDay());
        assertThat(task.getEndAt()).isEqualTo(targetDate.plusDays(1).atStartOfDay());
        assertThat(task.isAllDay()).isTrue();
        assertThat(task.getType()).isEqualTo(TaskType.TODO);
    }

    @Test
    @DisplayName("기존 수동 일정이 있는 Task를 Today로 이동하면 시간은 유지하고 날짜를 맞춘다")
    void moveToToday_movesUserSchedulePreservingTime() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 19, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 5, 19, 15, 0);
        Task task = Task.builder()
                .title("task")
                .startAt(startAt)
                .endAt(endAt)
                .build();
        LocalDate targetDate = LocalDate.of(2026, 5, 20);

        // when
        task.moveToToday(targetDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(targetDate);
        assertThat(task.getStartAt()).isEqualTo(LocalDateTime.of(2026, 5, 20, 14, 0));
        assertThat(task.getEndAt()).isEqualTo(LocalDateTime.of(2026, 5, 20, 15, 0));
        assertThat(task.isAllDay()).isFalse();
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
    @DisplayName("일정 없는 Task의 완료를 취소하면 지정 날짜의 자동 종일 일정을 생성한다")
    void reopenToday_createsAutoTodaySchedule() {
        // given
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.DONE)
                .completedAt(LocalDateTime.of(2026, 5, 20, 11, 0))
                .build();
        LocalDate targetDate = LocalDate.of(2026, 5, 20);

        // when
        task.reopenToday(targetDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(targetDate);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getStartAt()).isEqualTo(targetDate.atStartOfDay());
        assertThat(task.getEndAt()).isEqualTo(targetDate.plusDays(1).atStartOfDay());
        assertThat(task.isAllDay()).isTrue();
    }

    @Test
    @DisplayName("자동 종일 일정의 완료를 취소하면 일정을 지정 날짜로 이동한다")
    void reopenToday_movesAutoTodaySchedule() {
        // given
        LocalDate completedDate = LocalDate.of(2026, 5, 20);
        LocalDate targetDate = LocalDate.of(2026, 5, 22);
        Task task = Task.builder()
                .title("task")
                .startAt(completedDate.atStartOfDay())
                .endAt(completedDate.plusDays(1).atStartOfDay())
                .allDay(true)
                .status(TaskStatus.DONE)
                .targetDate(completedDate)
                .completedAt(LocalDateTime.of(2026, 5, 20, 11, 0))
                .build();

        // when
        task.reopenToday(targetDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(targetDate);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getStartAt()).isEqualTo(targetDate.atStartOfDay());
        assertThat(task.getEndAt()).isEqualTo(targetDate.plusDays(1).atStartOfDay());
        assertThat(task.isAllDay()).isTrue();
    }

    @Test
    @DisplayName("사용자 지정 일정의 완료를 취소하면 시간은 유지하고 선택 날짜로 이동한다")
    void reopenToday_movesUserSchedulePreservingTime() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 20, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 5, 20, 15, 0);
        LocalDate targetDate = LocalDate.of(2026, 5, 22);
        Task task = Task.builder()
                .title("task")
                .startAt(startAt)
                .endAt(endAt)
                .status(TaskStatus.DONE)
                .targetDate(LocalDate.of(2026, 5, 20))
                .completedAt(LocalDateTime.of(2026, 5, 20, 16, 0))
                .build();

        // when
        task.reopenToday(targetDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(targetDate);
        assertThat(task.getStartAt()).isEqualTo(LocalDateTime.of(2026, 5, 22, 14, 0));
        assertThat(task.getEndAt()).isEqualTo(LocalDateTime.of(2026, 5, 22, 15, 0));
    }

    @Test
    @DisplayName("자동 종일 일정을 이월하면 실행일과 캘린더 일정을 함께 이동한다")
    void carryOverTo_movesAutoTodaySchedule() {
        // given
        LocalDate currentDate = LocalDate.of(2026, 5, 20);
        Task task = Task.builder()
                .title("task")
                .startAt(currentDate.atStartOfDay())
                .endAt(currentDate.plusDays(1).atStartOfDay())
                .allDay(true)
                .status(TaskStatus.DONE)
                .targetDate(currentDate)
                .completedAt(LocalDateTime.of(2026, 5, 20, 21, 0))
                .carryOverCount(2)
                .build();
        LocalDate nextDate = LocalDate.of(2026, 5, 21);

        // when
        task.carryOverTo(nextDate);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(nextDate);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getCarryOverCount()).isEqualTo(3);
        assertThat(task.getStartAt()).isEqualTo(nextDate.atStartOfDay());
        assertThat(task.getEndAt()).isEqualTo(nextDate.plusDays(1).atStartOfDay());
        assertThat(task.isAllDay()).isTrue();
    }

    @Test
    @DisplayName("사용자 지정 일정을 이월하면 시간은 유지하고 다음 날짜로 이동한다")
    void carryOverTo_movesUserSchedulePreservingTime() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2026, 5, 20, 14, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 5, 20, 15, 0);
        Task task = Task.builder()
                .title("task")
                .startAt(startAt)
                .endAt(endAt)
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 20))
                .carryOverCount(1)
                .build();
        LocalDate nextDate = LocalDate.of(2026, 5, 21);

        // when
        task.carryOverTo(nextDate);

        // then
        assertThat(task.getTargetDate()).isEqualTo(nextDate);
        assertThat(task.getStartAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 14, 0));
        assertThat(task.getEndAt()).isEqualTo(LocalDateTime.of(2026, 5, 21, 15, 0));
        assertThat(task.getCarryOverCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("여러 날 일정을 이월하면 기간을 유지하고 날짜 전체를 이동한다")
    void carryOverTo_movesPeriodSchedulePreservingDuration() {
        // given
        Task task = Task.builder()
                .title("출장")
                .startAt(LocalDateTime.of(2026, 5, 20, 9, 0))
                .endAt(LocalDateTime.of(2026, 5, 22, 18, 0))
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 20))
                .build();

        // when
        task.carryOverTo(LocalDate.of(2026, 5, 23));

        // then
        assertThat(task.getTargetDate()).isEqualTo(LocalDate.of(2026, 5, 23));
        assertThat(task.getStartAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 9, 0));
        assertThat(task.getEndAt()).isEqualTo(LocalDateTime.of(2026, 5, 25, 18, 0));
    }

    @Test
    @DisplayName("날짜 없는 Today Task의 내용을 수정해도 실행 상태와 날짜를 유지한다")
    void update_unscheduledToday_preservesWorkflowState() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 6, 9);
        Task task = Task.builder()
                .title("기존 제목")
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .carryOverCount(2)
                .build();

        // when
        task.update("수정 제목", "설명 추가", TaskType.TODO, null, null, false, null);

        // then
        assertThat(task.getTitle()).isEqualTo("수정 제목");
        assertThat(task.getDescription()).isEqualTo("설명 추가");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(targetDate);
        assertThat(task.getCarryOverCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("날짜 없는 Inbox Task의 내용을 수정해도 Inbox 상태를 유지한다")
    void update_unscheduledInbox_preservesWorkflowState() {
        // given
        Task task = Task.builder()
                .title("기존 제목")
                .status(TaskStatus.INBOX)
                .build();

        // when
        task.update("수정 제목", "설명 추가", TaskType.TODO, null, null, false, null);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.INBOX);
        assertThat(task.getTargetDate()).isNull();
    }

    @Test
    @DisplayName("일정 날짜를 지정해 수정하면 Today 상태와 실행 날짜를 일정 시작일로 갱신한다")
    void update_scheduledTask_updatesWorkflowDate() {
        // given
        Task task = Task.builder()
                .title("기존 제목")
                .status(TaskStatus.INBOX)
                .build();
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 12, 14, 0);

        // when
        task.update("일정 제목", null, TaskType.SCHEDULE, startAt, null, false, null);

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(task.getTargetDate()).isEqualTo(LocalDate.of(2026, 6, 12));
    }

    @Test
    @DisplayName("자동 생성 일정을 사용자가 수정하면 USER 일정으로 전환한다")
    void update_autoSchedule_changesSourceToUser() {
        // given
        Task task = Task.builder()
                .title("기존 제목")
                .startAt(LocalDateTime.of(2026, 6, 10, 0, 0))
                .endAt(LocalDateTime.of(2026, 6, 11, 0, 0))
                .allDay(true)
                .build();

        // when
        task.update(
                "수정 제목",
                null,
                TaskType.SCHEDULE,
                LocalDateTime.of(2026, 6, 10, 14, 0),
                LocalDateTime.of(2026, 6, 10, 15, 0),
                false,
                null
        );

        // then
    }

    @Test
    @DisplayName("자동 생성 일정의 내용만 수정하면 AUTO_TODAY 출처를 유지한다")
    void update_autoScheduleContent_preservesSource() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2026, 6, 10, 0, 0);
        LocalDateTime endAt = LocalDateTime.of(2026, 6, 11, 0, 0);
        Task task = Task.builder()
                .title("기존 제목")
                .startAt(startAt)
                .endAt(endAt)
                .allDay(true)
                .build();

        // when
        task.update("수정 제목", "설명 추가", TaskType.SCHEDULE, startAt, endAt, true, null);

        // then
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

    @Test
    @DisplayName("setDeferReason은 미룬 이유를 기록한다")
    void setDeferReason() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();

        // when
        task.setDeferReason(DeferReason.ETC);

        // then
        assertThat(task.getDeferReason()).isEqualTo(DeferReason.ETC);
    }

    @Test
    @DisplayName("setDeferReason은 미룬 이유가 없으면 실패한다")
    void setDeferReason_requiresReason() {
        // given
        Task task = Task.builder()
                .title("task")
                .build();

        // when & then
        assertThatThrownBy(() -> task.setDeferReason(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("deferReason은 필수입니다.");
    }

    @Test
    @DisplayName("clearDeferReason은 미룬 이유를 해제한다")
    void clearDeferReason() {
        // given
        Task task = Task.builder()
                .title("task")
                .deferReason(DeferReason.TOO_BIG)
                .build();

        // when
        task.clearDeferReason();

        // then
        assertThat(task.getDeferReason()).isNull();
    }

    @Test
    @DisplayName("계획 날짜는 targetDate를 우선 사용한다")
    void getPlannedDate_prefersTargetDate() {
        LocalDate targetDate = LocalDate.of(2026, 6, 15);
        Task task = Task.builder()
                .title("task")
                .startAt(LocalDateTime.of(2026, 6, 14, 10, 0))
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .build();

        assertThat(task.getPlannedDate()).isEqualTo(targetDate);
    }

    @Test
    @DisplayName("targetDate가 없으면 일정 시작일을 계획 날짜로 사용한다")
    void getPlannedDate_fallsBackToStartDate() {
        LocalDate startDate = LocalDate.of(2026, 6, 15);
        Task task = Task.builder()
                .title("task")
                .startAt(startDate.atTime(10, 0))
                .build();

        assertThat(task.getPlannedDate()).isEqualTo(startDate);
    }

    @Test
    @DisplayName("기록함 Task는 계획 날짜가 없다")
    void getPlannedDate_isNullForInboxTask() {
        Task task = Task.builder()
                .title("task")
                .build();

        assertThat(task.getPlannedDate()).isNull();
    }
}
