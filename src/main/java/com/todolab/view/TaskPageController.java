package com.todolab.view;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.StringWriter;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TaskPageController {

    private final SpringTemplateEngine templateEngine;
    private final RestClient restClient;

    private final String[] dayLabels = {"일", "월", "화", "수", "목", "금", "토"};

    // ===========================
    //  일정 등록 모달
    // ===========================
    @GetMapping(
            value = "/tasks/create",
            headers = "X-Requested-With=fetch",
            produces = MediaType.TEXT_HTML_VALUE
    )
    @ResponseBody
    public String createFragment() {
        Context ctx = new Context();

        StringWriter writer = new StringWriter();
        templateEngine.process(
                "pages/task/create",
                Set.of("#create-page"),
                ctx,
                writer
        );

        return writer.toString();
    }

    // ===========================
    //  일정 상세 모달
    // ===========================
    @GetMapping(
            value = "/tasks/detail",
            headers = "X-Requested-With=fetch",
            produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String detailFragment(@RequestParam Long id) {

        ApiResponse<TaskResponse> resp = restClient.get()
                .uri("/api/tasks/{id}", id)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        TaskResponse task = resp.data();

        Context ctx = new Context();
        ctx.setVariable("task", task);

        StringWriter writer = new StringWriter();
        templateEngine.process(
                "pages/task/detail",
                Set.of("#detail-fragment"),
                ctx,
                writer
        );

        return writer.toString();
    }

    // ===========================
    //  일정 미정 페이지
    // ===========================
    @GetMapping("/tasks/unscheduled")
    public String unscheduled(Model model) {
        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle", "ToDoLab");
        model.addAttribute("activeTab", "unscheduled");

        model.addAttribute("contentView", "pages/task/unscheduled");
        return "layout/base";
    }

    // ===========================
    //  일간 일정 페이지
    // ===========================
    @GetMapping("/tasks/today")
    public String today(Model model) {

        LocalDate today = LocalDate.now();

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle", "Today");
        model.addAttribute("activeTab", "today");
        model.addAttribute("date", today);
        model.addAttribute("contentView", "pages/task/today");
        return "layout/base";
    }

    // ===========================
    //  주간 일정 페이지
    // ===========================
    @GetMapping("/tasks/week")
    public String week(
            @RequestParam(name = "move", required = false) String move,
            @RequestParam(name = "date", required = false) String date,
            Model model
    ) {
        LocalDate targetDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        if ("prev".equals(move)) targetDate = targetDate.minusWeeks(1);
        if ("next".equals(move)) targetDate = targetDate.plusWeeks(1);

        LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        final LocalDate finalTargetDate = targetDate;
        ApiResponse<List<TaskResponse>> resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tasks")
                        .queryParam("type", "WEEK")
                        .queryParam("date", finalTargetDate.toString())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<TaskResponse> taskList = (resp != null && resp.data() != null) ? resp.data() : List.of();

        List<DaySchedule> weeklyTasks = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);

            List<TaskUi> uiTasks = taskList.stream()
                    .filter(t -> occursOn(t, day))
                    .map(this::toUi)
                    .toList();

            weeklyTasks.add(new DaySchedule(day, dayLabels[i], uiTasks));
        }

        LocalDate selectedDate = targetDate;
        if (selectedDate.isBefore(weekStart) || selectedDate.isAfter(weekEnd)) {
            selectedDate = weekStart;
        }

        final LocalDate finalSelectedDate = selectedDate;
        DaySchedule selectedSchedule = weeklyTasks.stream()
                .filter(ds -> ds.date().equals(finalSelectedDate))
                .findFirst()
                .orElse(null);

        int weekTotalCount = weeklyTasks.stream()
                .mapToInt(ds -> ds.tasks() == null ? 0 : ds.tasks().size())
                .sum();

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle", targetDate.getYear() + "년 " + targetDate.getMonthValue() + "월");
        model.addAttribute("activeTab", "calendar");

        model.addAttribute("currentDate", targetDate);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("weekEnd", weekEnd);
        model.addAttribute("weekRange", weekStart + " ~ " + weekEnd);

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("weeklyTasks", weeklyTasks);
        model.addAttribute("selectedSchedule", selectedSchedule);
        model.addAttribute("weekTotalCount", weekTotalCount);

        model.addAttribute("contentView", "pages/task/week");
        return "layout/base";
    }

    // ===========================
    //  월간 일정 페이지 ✅ (MONTH API: date=yyyy-MM)
    // ===========================
    @GetMapping("/tasks/month")
    public String month(
            @RequestParam(name = "move", required = false) String move,
            @RequestParam(name = "date", required = false) String date,
            Model model
    ) {
        // date 파싱: "yyyy-MM" 또는 "yyyy-MM-dd" 모두 허용
        LocalDate targetDate = parseMonthTargetDate(date);

        if ("prev".equals(move)) targetDate = targetDate.minusMonths(1);
        if ("next".equals(move)) targetDate = targetDate.plusMonths(1);

        // ✅ month() 내부에서 gridStart/42칸 로직을 아래로 교체

        YearMonth ym = YearMonth.from(targetDate);
        String ymKey = ym.toString(); // "2026-02"

        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // ✅ 그 달을 포함하는 “최소 주수” 계산
        LocalDate gridStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate gridEnd = monthEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        int totalCells = (int) Duration.between(gridStart.atStartOfDay(), gridEnd.plusDays(1).atStartOfDay()).toDays();              // inclusive 계산을 위에서 +1 처리했으므로 days가 칸 수
        int weeks = totalCells / 7;         // 4~6

        // ✅ 백엔드 호출: type=MONTH, date=yyyy-MM
        ApiResponse<List<TaskResponse>> resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tasks")
                        .queryParam("type", "MONTH")
                        .queryParam("date", ymKey)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<TaskResponse> taskList = (resp != null && resp.data() != null) ? resp.data() : List.of();

        LocalDate selectedDate = (date != null && date.length() == 10)
                ? LocalDate.parse(date)
                : targetDate;

        // ✅ “필요한 주수만큼”만 생성
        List<CalendarCell> monthDays = new ArrayList<>(weeks * 7);
        for (int i = 0; i < weeks * 7; i++) {
            LocalDate day = gridStart.plusDays(i);

            // ✅ 이번 달에 속한 날만 보이게 하려면 inMonth가 false인 셀은 만들지 않는 방식도 가능하지만,
            // 요청은 “이번 달에 속한 날만큼만” = 최소 주수, 즉 앞/뒤 padding(회색)은 남겨도 되지만
            // 화면은 4주/5주/6주만 보이게 하는 게 핵심이므로 여기서는 “주 단위 패딩”은 유지하고,
            // 스타일에서 inMonth=false를 연하게 처리.
            boolean inMonth = !day.isBefore(monthStart) && !day.isAfter(monthEnd);

            List<TaskUi> uiTasks = taskList.stream()
                    .filter(t -> occursOn(t, day))
                    .map(this::toUi)
                    .toList();

            monthDays.add(new CalendarCell(day, inMonth, uiTasks));
        }

        int monthTotalCount = monthDays.stream()
                .mapToInt(c -> c.tasks() == null ? 0 : c.tasks().size())
                .sum();

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle", targetDate.getYear() + "년 " + targetDate.getMonthValue() + "월");
        model.addAttribute("activeTab", "calendar");

        model.addAttribute("currentDate", targetDate);
        model.addAttribute("selectedDate", selectedDate);

        model.addAttribute("monthStart", monthStart);
        model.addAttribute("monthEnd", monthEnd);

        // ✅ dataset/month.js에서 그대로 쓰게 "yyyy-MM" 내려줌
        model.addAttribute("monthLabel", ymKey);
        model.addAttribute("monthRange", ymKey);

        model.addAttribute("monthDays", monthDays);
        model.addAttribute("monthTotalCount", monthTotalCount);

        model.addAttribute("contentView", "pages/task/month");
        return "layout/base";
    }

    /**
     * date 입력:
     * - null/blank -> 오늘
     * - "yyyy-MM"  -> 해당 월 1일로 변환
     * - "yyyy-MM-dd" -> 해당 날짜
     */
    private LocalDate parseMonthTargetDate(String date) {
        if (date == null || date.isBlank()) return LocalDate.now();

        String s = date.trim();
        try {
            if (s.length() == 7) { // yyyy-MM
                YearMonth ym = YearMonth.parse(s);
                return ym.atDay(1);
            }
            if (s.length() == 10) { // yyyy-MM-dd
                return LocalDate.parse(s);
            }
            // fallback
            return LocalDate.now();
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    // ===========================
    // 일정이 특정 날짜(day)에 "발생/겹침"하는지 판단
    // ===========================
    private boolean occursOn(TaskResponse t, LocalDate day) {
        if (t == null) return false;
        if (t.unscheduled()) return false;
        if (t.startAt() == null) return false;

        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay(); // [dayStart, dayEnd)

        LocalDateTime start = t.startAt();
        LocalDateTime end = t.endAt();

        if (end == null) {
            return !start.isBefore(dayStart) && start.isBefore(dayEnd);
        }
        return start.isBefore(dayEnd) && end.isAfter(dayStart);
    }

    // ===========================
    // TaskResponse -> TaskUi 변환
    // ===========================
    private TaskUi toUi(TaskResponse t) {
        LocalDateTime startAt = t.startAt();

        LocalDate date = (startAt != null) ? startAt.toLocalDate() : null;
        LocalTime time = (startAt != null && !t.allDay()) ? startAt.toLocalTime() : null;

        return new TaskUi(
                t.id(),
                t.title(),
                t.description(),
                date,
                time,
                t.allDay(),
                t.startAt(),
                t.endAt(),
                t.category(),
                pickColor(t.id())
        );
    }

    // ===========================
    // 색상 알고리즘 (✅ id 기반)
    // ===========================
    private String pickColor(Long id) {
        String[] colors = {
                "#BFDBFE", "#C4B5FD", "#FDE68A",
                "#FBCFE8", "#BBF7D0"
        };
        if (id == null) {
            return colors[0];
        }
        int idx = Math.floorMod(id.hashCode(), colors.length);
        return colors[idx];
    }

    public record TaskUi(
            Long id,
            String title,
            String description,
            LocalDate date,
            LocalTime time,
            boolean allDay,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String category,
            String color
    ) {
    }

    public record DaySchedule(
            LocalDate date,
            String dayLabel,
            List<TaskUi> tasks
    ) {
    }

    public record CalendarCell(
            LocalDate date,
            boolean inMonth,
            List<TaskUi> tasks
    ) {
    }
}
