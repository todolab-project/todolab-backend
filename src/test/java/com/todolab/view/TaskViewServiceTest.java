package com.todolab.view;

import com.todolab.dday.dto.DdayGoalResponse;
import com.todolab.task.domain.TaskStatus;
import com.todolab.dday.service.DdayGoalService;
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

    @Mock
    DdayGoalService ddayGoalService;

    TaskViewService taskViewService;

    @BeforeEach
    void setUp() {
        taskViewService = new TaskViewService(taskService, ddayGoalService);
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
        given(taskService.getTodayTasksBetween(LocalDate.of(2025, 11, 23), LocalDate.of(2025, 11, 29)))
                .willReturn(List.of());
        given(ddayGoalService.findByDateRange(LocalDate.of(2025, 11, 23), LocalDate.of(2025, 11, 29)))
                .willReturn(List.of());

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
        then(taskService).should().getTodayTasksBetween(LocalDate.of(2025, 11, 23), LocalDate.of(2025, 11, 29));
        then(ddayGoalService).should().findByDateRange(LocalDate.of(2025, 11, 23), LocalDate.of(2025, 11, 29));
        then(taskService).shouldHaveNoMoreInteractions();
        then(ddayGoalService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("주간 화면 조회 - prev/next 이동 값을 조회 기준 날짜에 반영한다")
    void getWeekPage_appliesMove() {
        // given
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(List.of());
        given(taskService.getTodayTasksBetween(LocalDate.of(2025, 11, 30), LocalDate.of(2025, 12, 6)))
                .willReturn(List.of());
        given(ddayGoalService.findByDateRange(LocalDate.of(2025, 11, 30), LocalDate.of(2025, 12, 6)))
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
        then(taskService).should().getTodayTasksBetween(LocalDate.of(2025, 11, 30), LocalDate.of(2025, 12, 6));
        then(ddayGoalService).should().findByDateRange(LocalDate.of(2025, 11, 30), LocalDate.of(2025, 12, 6));
        then(taskService).shouldHaveNoMoreInteractions();
        then(ddayGoalService).shouldHaveNoMoreInteractions();
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
        given(taskService.getTodayTasksBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .willReturn(List.of());
        given(ddayGoalService.findByDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .willReturn(List.of());

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
        then(taskService).should().getTodayTasksBetween(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        then(ddayGoalService).should().findByDateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        then(taskService).shouldHaveNoMoreInteractions();
        then(ddayGoalService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("월간 화면 조회 - 날짜와 이동 값을 월 조회 기준에 반영한다")
    void getMonthPage_appliesDateAndMove() {
        // given
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(List.of());
        given(taskService.getTodayTasksBetween(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 31)))
                .willReturn(List.of());
        given(ddayGoalService.findByDateRange(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 31)))
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
        then(taskService).should().getTodayTasksBetween(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 31));
        then(ddayGoalService).should().findByDateRange(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 31));
        then(taskService).shouldHaveNoMoreInteractions();
        then(ddayGoalService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("주간 화면 조회 - D-Day 목표를 날짜별로 구성한다")
    void getWeekPage_buildsDdayGoalsByDate() {
        // given
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(List.of());
        given(taskService.getTodayTasksBetween(LocalDate.of(2026, 6, 7), LocalDate.of(2026, 6, 13)))
                .willReturn(List.of());
        given(ddayGoalService.findByDateRange(LocalDate.of(2026, 6, 7), LocalDate.of(2026, 6, 13)))
                .willReturn(List.of(new DdayGoalResponse(
                        1L,
                        "정보처리기사",
                        LocalDate.of(2026, 6, 10),
                        10,
                        null
                )));

        // when
        WeekPageModel page = taskViewService.getWeekPage(null, "2026-06-10");

        // then
        DaySchedule selected = page.selectedSchedule();
        assertThat(selected.date()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(selected.ddayGoals()).hasSize(1);
        assertThat(selected.ddayGoals().getFirst().title()).isEqualTo("정보처리기사");
        assertThat(selected.ddayGoals().getFirst().label()).isEqualTo("D-10");

        then(ddayGoalService).should().findByDateRange(LocalDate.of(2026, 6, 7), LocalDate.of(2026, 6, 13));
    }

    @Test
    @DisplayName("주간 화면 조회 - Today 할 일을 targetDate 기준 날짜에 포함한다")
    void getWeekPage_includesTodayTasksByTargetDate() {
        // given
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(List.of());
        given(taskService.getTodayTasksBetween(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 6)))
                .willReturn(List.of(todayTask(20L, "3회 이월 Today", LocalDate.of(2026, 6, 1))));
        given(ddayGoalService.findByDateRange(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 6)))
                .willReturn(List.of());

        // when
        WeekPageModel page = taskViewService.getWeekPage(null, "2026-06-01");

        // then
        DaySchedule monday = page.weeklyTasks().get(1);
        assertThat(monday.date()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(monday.tasks()).extracting("title")
                .containsExactly("3회 이월 Today");
        assertThat(page.weekTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("월간 화면 조회 - Today 할 일을 targetDate 기준 날짜 셀에 포함한다")
    void getMonthPage_includesTodayTasksByTargetDate() {
        // given
        given(taskService.getTasks(org.mockito.ArgumentMatchers.any(TaskQueryRequest.class)))
                .willReturn(List.of());
        given(taskService.getTodayTasksBetween(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 7, 4)))
                .willReturn(List.of(todayTask(21L, "3회 이월 Today", LocalDate.of(2026, 6, 1))));
        given(ddayGoalService.findByDateRange(LocalDate.of(2026, 5, 31), LocalDate.of(2026, 7, 4)))
                .willReturn(List.of());

        // when
        MonthPageModel page = taskViewService.getMonthPage(null, "2026-06-01");

        // then
        CalendarCell june1 = findCell(page, LocalDate.of(2026, 6, 1));
        assertThat(june1.tasks()).extracting("title")
                .containsExactly("3회 이월 Today");
        assertThat(page.monthTotalCount()).isEqualTo(1);
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

    private TaskResponse todayTask(Long id, String title, LocalDate targetDate) {
        return TaskResponse.builder()
                .id(id)
                .title(title)
                .description(title + " 설명")
                .startAt(null)
                .endAt(null)
                .allDay(false)
                .unscheduled(true)
                .category("일")
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .carryOverCount(3)
                .staleCarryOver(true)
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
