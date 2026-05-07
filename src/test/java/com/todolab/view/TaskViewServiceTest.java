package com.todolab.view;

import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.service.TaskService;
import com.todolab.view.model.CalendarCell;
import com.todolab.view.model.DaySchedule;
import com.todolab.view.model.MonthPageModel;
import com.todolab.view.model.WeekPageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TaskViewServiceTest {

    @Mock
    TaskService taskService;

    TaskViewService taskViewService;

    @BeforeEach
    void setUp() {
        taskViewService = new TaskViewService(taskService);
    }

    @Test
    @DisplayName("주간 화면 조회 - 선택 날짜 기준 주간 범위와 요일별 일정을 구성한다")
    void getWeekPage_buildsWeekModel() {
        // given
        List<TaskResponse> tasks = List.of(
                task(1L, "단일 일정", LocalDateTime.of(2025, 11, 25, 10, 0), null, false),
                task(2L, "기간 일정", LocalDateTime.of(2025, 11, 25, 22, 0), LocalDateTime.of(2025, 11, 27, 0, 0), false),
                task(3L, "미정 일정", null, null, true)
        );
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(tasks);

        // when
        WeekPageModel page = taskViewService.getWeekPage(null, "2025-11-25");

        // then
        assertThat(page.currentDate()).isEqualTo(LocalDate.of(2025, 11, 25));
        assertThat(page.weekStart()).isEqualTo(LocalDate.of(2025, 11, 23));
        assertThat(page.weekEnd()).isEqualTo(LocalDate.of(2025, 11, 29));
        assertThat(page.weekRange()).isEqualTo("2025-11-23 ~ 2025-11-29");
        assertThat(page.selectedDate()).isEqualTo(LocalDate.of(2025, 11, 25));
        assertThat(page.weeklyTasks()).hasSize(7);
        assertThat(page.weekTotalCount()).isEqualTo(3);

        DaySchedule tuesday = page.weeklyTasks().get(2);
        DaySchedule wednesday = page.weeklyTasks().get(3);
        DaySchedule thursday = page.weeklyTasks().get(4);

        assertThat(tuesday.dayLabel()).isEqualTo("화");
        assertThat(tuesday.tasks()).extracting("title")
                .containsExactly("단일 일정", "기간 일정");
        assertThat(wednesday.tasks()).extracting("title")
                .containsExactly("기간 일정");
        assertThat(thursday.tasks()).isEmpty();
        assertThat(page.selectedSchedule()).isEqualTo(tuesday);

        ArgumentCaptor<TaskQueryRequest> requestCaptor = ArgumentCaptor.forClass(TaskQueryRequest.class);
        then(taskService).should().getTasks(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getType()).isEqualTo(TaskQueryType.WEEK);
        assertThat(requestCaptor.getValue().getDate()).isEqualTo("2025-11-25");
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("주간 화면 조회 - prev/next 이동 값을 조회 기준 날짜에 반영한다")
    void getWeekPage_appliesMove() {
        // given
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(List.of());

        // when
        WeekPageModel page = taskViewService.getWeekPage("next", "2025-11-25");

        // then
        assertThat(page.currentDate()).isEqualTo(LocalDate.of(2025, 12, 2));
        assertThat(page.weekStart()).isEqualTo(LocalDate.of(2025, 11, 30));
        assertThat(page.weekEnd()).isEqualTo(LocalDate.of(2025, 12, 6));
        assertThat(page.weekTotalCount()).isZero();

        ArgumentCaptor<TaskQueryRequest> requestCaptor = ArgumentCaptor.forClass(TaskQueryRequest.class);
        then(taskService).should().getTasks(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getType()).isEqualTo(TaskQueryType.WEEK);
        assertThat(requestCaptor.getValue().getDate()).isEqualTo("2025-12-02");
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("월간 화면 조회 - yyyy-MM 기준으로 월 달력 모델을 구성한다")
    void getMonthPage_buildsMonthModel() {
        // given
        List<TaskResponse> tasks = List.of(
                task(10L, "월간 일정", LocalDateTime.of(2026, 2, 10, 12, 0), null, false),
                task(11L, "월말 기간 일정", LocalDateTime.of(2026, 2, 28, 22, 0), LocalDateTime.of(2026, 3, 1, 1, 0), false),
                task(12L, "미정 일정", null, null, true)
        );
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(tasks);

        // when
        MonthPageModel page = taskViewService.getMonthPage(null, "2026-02");

        // then
        assertThat(page.currentDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(page.selectedDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(page.monthStart()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(page.monthEnd()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(page.monthLabel()).isEqualTo("2026-02");
        assertThat(page.monthRange()).isEqualTo("2026-02");
        assertThat(page.monthDays()).hasSize(28);
        assertThat(page.monthTotalCount()).isEqualTo(2);

        CalendarCell feb10 = findCell(page, LocalDate.of(2026, 2, 10));
        CalendarCell feb28 = findCell(page, LocalDate.of(2026, 2, 28));

        assertThat(feb10.inMonth()).isTrue();
        assertThat(feb10.tasks()).extracting("title")
                .containsExactly("월간 일정");
        assertThat(feb28.tasks()).extracting("title")
                .containsExactly("월말 기간 일정");

        ArgumentCaptor<TaskQueryRequest> requestCaptor = ArgumentCaptor.forClass(TaskQueryRequest.class);
        then(taskService).should().getTasks(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getType()).isEqualTo(TaskQueryType.MONTH);
        assertThat(requestCaptor.getValue().getDate()).isEqualTo("2026-02");
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("월간 화면 조회 - 날짜와 이동 값을 월 조회 기준에 반영한다")
    void getMonthPage_appliesDateAndMove() {
        // given
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(List.of());

        // when
        MonthPageModel page = taskViewService.getMonthPage("prev", "2026-02-15");

        // then
        assertThat(page.currentDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(page.selectedDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(page.monthStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(page.monthEnd()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(page.monthLabel()).isEqualTo("2026-01");
        assertThat(page.monthDays()).hasSize(35);

        ArgumentCaptor<TaskQueryRequest> requestCaptor = ArgumentCaptor.forClass(TaskQueryRequest.class);
        then(taskService).should().getTasks(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getType()).isEqualTo(TaskQueryType.MONTH);
        assertThat(requestCaptor.getValue().getDate()).isEqualTo("2026-01");
        then(taskService).shouldHaveNoMoreInteractions();
    }

    private TaskResponse task(Long id, String title, LocalDateTime startAt, LocalDateTime endAt, boolean unscheduled) {
        return TaskResponse.builder()
                .id(id)
                .title(title)
                .description(title + " 설명")
                .startAt(startAt)
                .endAt(endAt)
                .allDay(false)
                .unscheduled(unscheduled)
                .category("일")
                .createdAt(null)
                .build();
    }

    private CalendarCell findCell(MonthPageModel page, LocalDate date) {
        return page.monthDays().stream()
                .filter(cell -> cell.date().equals(date))
                .findFirst()
                .orElseThrow();
    }
}
