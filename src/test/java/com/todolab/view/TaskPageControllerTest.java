package com.todolab.view;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class TaskPageControllerTest {

    @Mock
    SpringTemplateEngine templateEngine;

    @Mock
    TaskService taskService;

    @Mock
    TaskViewService taskViewService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TaskPageController controller = new TaskPageController(templateEngine, taskService, taskViewService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("일정 등록 fragment 요청 시 create fragment HTML을 반환한다")
    void createFragment_returnsHtml() throws Exception {
        // given
        writeFragment("pages/task/create", Set.of("#create-page"), "<section id=\"create-page\">create</section>");

        // when & then
        mockMvc.perform(get("/tasks/create")
                        .header("X-Requested-With", "fetch"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<section id=\"create-page\">create</section>"));

        then(templateEngine).should()
                .process(eq("pages/task/create"), eq(Set.of("#create-page")), any(Context.class), any(Writer.class));
        then(templateEngine).shouldHaveNoMoreInteractions();
        then(taskService).shouldHaveNoInteractions();
        then(taskViewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 상세 fragment 요청 시 task 조회 후 detail fragment HTML을 반환한다")
    void detailFragment_returnsHtml() throws Exception {
        // given
        long taskId = 11L;
        TaskResponse task = TaskResponse.builder()
                .id(taskId)
                .title("상세 일정")
                .startAt(LocalDateTime.of(2026, 5, 12, 10, 0))
                .build();

        given(taskService.getTask(taskId)).willReturn(task);
        writeFragment("pages/task/detail", Set.of("#detail-fragment"), "<section id=\"detail-fragment\">detail</section>");

        // when & then
        mockMvc.perform(get("/tasks/detail")
                        .header("X-Requested-With", "fetch")
                        .param("id", String.valueOf(taskId)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<section id=\"detail-fragment\">detail</section>"));

        then(taskService).should().getTask(taskId);
        then(taskService).shouldHaveNoMoreInteractions();
        then(templateEngine).should()
                .process(eq("pages/task/detail"), eq(Set.of("#detail-fragment")), any(Context.class), any(Writer.class));
        then(templateEngine).shouldHaveNoMoreInteractions();
        then(taskViewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("미정 일정 페이지는 base layout과 unscheduled 탭 모델을 반환한다")
    void unscheduled_returnsBaseLayout() throws Exception {
        mockMvc.perform(get("/tasks/unscheduled"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/base"))
                .andExpect(model().attribute("title", "ToDoLab"))
                .andExpect(model().attribute("showBaseHeader", false))
                .andExpect(model().attribute("headerTitle", "ToDoLab"))
                .andExpect(model().attribute("activeTab", "unscheduled"))
                .andExpect(model().attribute("contentView", "pages/task/unscheduled"));

        then(templateEngine).shouldHaveNoInteractions();
        then(taskService).shouldHaveNoInteractions();
        then(taskViewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("오늘 일정 페이지는 base layout과 today 탭 모델을 반환한다")
    void today_returnsBaseLayout() throws Exception {
        mockMvc.perform(get("/tasks/today"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/base"))
                .andExpect(model().attribute("title", "ToDoLab"))
                .andExpect(model().attribute("showBaseHeader", false))
                .andExpect(model().attribute("headerTitle", "Today"))
                .andExpect(model().attribute("activeTab", "today"))
                .andExpect(model().attributeExists("date"))
                .andExpect(model().attribute("contentView", "pages/task/today"));

        then(templateEngine).shouldHaveNoInteractions();
        then(taskService).shouldHaveNoInteractions();
        then(taskViewService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("주간 일정 페이지는 TaskViewService 결과를 calendar 탭 모델로 반환한다")
    void week_returnsBaseLayoutWithWeekModel() throws Exception {
        // given
        LocalDate currentDate = LocalDate.of(2026, 5, 12);
        LocalDate weekStart = LocalDate.of(2026, 5, 10);
        LocalDate weekEnd = LocalDate.of(2026, 5, 16);
        DaySchedule selectedSchedule = new DaySchedule(currentDate, "화", List.of());
        List<DaySchedule> weeklyTasks = List.of(selectedSchedule);
        WeekPageModel page = new WeekPageModel(
                currentDate,
                weekStart,
                weekEnd,
                "2026-05-10 ~ 2026-05-16",
                currentDate,
                weeklyTasks,
                selectedSchedule,
                0
        );

        given(taskViewService.getWeekPage("next", "2026-05-05")).willReturn(page);

        // when & then
        mockMvc.perform(get("/tasks/week")
                        .param("move", "next")
                        .param("date", "2026-05-05"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/base"))
                .andExpect(model().attribute("title", "ToDoLab"))
                .andExpect(model().attribute("showBaseHeader", false))
                .andExpect(model().attribute("headerTitle", "2026년 5월"))
                .andExpect(model().attribute("activeTab", "calendar"))
                .andExpect(model().attribute("currentDate", currentDate))
                .andExpect(model().attribute("weekStart", weekStart))
                .andExpect(model().attribute("weekEnd", weekEnd))
                .andExpect(model().attribute("weekRange", "2026-05-10 ~ 2026-05-16"))
                .andExpect(model().attribute("selectedDate", currentDate))
                .andExpect(model().attribute("weeklyTasks", weeklyTasks))
                .andExpect(model().attribute("selectedSchedule", selectedSchedule))
                .andExpect(model().attribute("weekTotalCount", 0))
                .andExpect(model().attribute("contentView", "pages/task/week"));

        then(taskViewService).should().getWeekPage("next", "2026-05-05");
        then(taskViewService).shouldHaveNoMoreInteractions();
        then(templateEngine).shouldHaveNoInteractions();
        then(taskService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("월간 일정 페이지는 TaskViewService 결과를 calendar 탭 모델로 반환한다")
    void month_returnsBaseLayoutWithMonthModel() throws Exception {
        // given
        LocalDate currentDate = LocalDate.of(2026, 5, 1);
        LocalDate selectedDate = LocalDate.of(2026, 5, 12);
        LocalDate monthStart = LocalDate.of(2026, 5, 1);
        LocalDate monthEnd = LocalDate.of(2026, 5, 31);
        List<CalendarCell> monthDays = List.of(new CalendarCell(selectedDate, true, List.of()));
        MonthPageModel page = new MonthPageModel(
                currentDate,
                selectedDate,
                monthStart,
                monthEnd,
                "2026-05",
                "2026-05",
                monthDays,
                0
        );

        given(taskViewService.getMonthPage(null, "2026-05")).willReturn(page);

        // when & then
        mockMvc.perform(get("/tasks/month")
                        .param("date", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(view().name("layout/base"))
                .andExpect(model().attribute("title", "ToDoLab"))
                .andExpect(model().attribute("showBaseHeader", false))
                .andExpect(model().attribute("headerTitle", "2026년 5월"))
                .andExpect(model().attribute("activeTab", "calendar"))
                .andExpect(model().attribute("currentDate", currentDate))
                .andExpect(model().attribute("selectedDate", selectedDate))
                .andExpect(model().attribute("monthStart", monthStart))
                .andExpect(model().attribute("monthEnd", monthEnd))
                .andExpect(model().attribute("monthLabel", "2026-05"))
                .andExpect(model().attribute("monthRange", "2026-05"))
                .andExpect(model().attribute("monthDays", monthDays))
                .andExpect(model().attribute("monthTotalCount", 0))
                .andExpect(model().attribute("contentView", "pages/task/month"));

        then(taskViewService).should().getMonthPage(null, "2026-05");
        then(taskViewService).shouldHaveNoMoreInteractions();
        then(templateEngine).shouldHaveNoInteractions();
        then(taskService).shouldHaveNoInteractions();
    }

    private void writeFragment(String template, Set<String> selectors, String html) {
        willAnswer(invocation -> {
            Writer writer = invocation.getArgument(3);
            writer.write(html);
            return null;
        }).given(templateEngine).process(eq(template), eq(selectors), any(Context.class), any(Writer.class));
    }
}
