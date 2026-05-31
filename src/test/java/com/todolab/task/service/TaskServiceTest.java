package com.todolab.task.service;

import com.todolab.common.api.ErrorCode;
import com.todolab.dday.domain.DdayGoal;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.domain.TaskType;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskCategoryGroupResponse;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    TaskTxService taskTxService;

    TaskCategoryGrouper taskCategoryGrouper;

    TaskService taskService;

    @BeforeEach
    void setUp() {
        taskCategoryGrouper = new TaskCategoryGrouper();
        taskService = new TaskService(taskTxService, taskRepository, taskCategoryGrouper);
    }

    /*******************
     *  일정 등록
     *******************/
    @Test
    @DisplayName("일정 등록 성공 - 단일 일정(endAt=null)")
    void createTask_success_single() {
        // given
        LocalDateTime startAt = LocalDateTime.of(2025, 11, 27, 10, 30);

        Task saved = Task.builder()
                .title("title")
                .description("desc")
                .startAt(startAt)
                .endAt(null)
                .category("일")
                .allDay(false)
                .build();

        given(taskRepository.save(any())).willReturn(saved);

        TaskRequest request = new TaskRequest(
                "title",
                "desc",
                startAt,
                null,
                "일",
                false
        );

        // when
        TaskResponse res = taskService.create(request);

        // then
        assertThat(res.title()).isEqualTo("title");
        assertThat(res.startAt()).isEqualTo(startAt);
        assertThat(res.endAt()).isNull();
        assertThat(res.allDay()).isFalse();
        assertThat(res.unscheduled()).isFalse();
        assertThat(res.type()).isEqualTo(TaskType.SCHEDULE);
        assertThat(res.status()).isEqualTo(TaskStatus.TODAY);
        assertThat(res.targetDate()).isEqualTo(startAt.toLocalDate());
        assertThat(res.completedAt()).isNull();

        then(taskRepository).should(times(1)).save(any(Task.class));
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 등록 성공 - type이 없고 날짜가 없으면 TODO로 저장된다")
    void createTask_success_defaultType() {
        // given
        TaskRequest request = new TaskRequest(
                "default type",
                "desc",
                null,
                null,
                "일",
                false
        );

        given(taskRepository.save(any(Task.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TaskResponse res = taskService.create(request);

        // then
        assertThat(res.type()).isEqualTo(TaskType.TODO);
        assertThat(res.unscheduled()).isTrue();
        assertThat(res.status()).isEqualTo(TaskStatus.INBOX);
        assertThat(res.targetDate()).isNull();
        assertThat(res.completedAt()).isNull();

        then(taskRepository).should(times(1)).save(any(Task.class));
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 등록 성공 - 제목만 있으면 Inbox 상태로 저장된다")
    void createTask_success_titleOnlyInbox() {
        // given
        TaskRequest request = new TaskRequest(
                "quick task",
                null,
                null,
                null,
                null,
                false
        );

        given(taskRepository.save(any(Task.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TaskResponse res = taskService.create(request);

        // then
        assertThat(res.title()).isEqualTo("quick task");
        assertThat(res.type()).isEqualTo(TaskType.TODO);
        assertThat(res.status()).isEqualTo(TaskStatus.INBOX);
        assertThat(res.targetDate()).isNull();
        assertThat(res.completedAt()).isNull();
        assertThat(res.unscheduled()).isTrue();

        then(taskRepository).should(times(1)).save(any(Task.class));
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 등록 성공 - 요청 type을 저장한다")
    void createTask_success_requestedType() {
        // given
        TaskRequest request = new TaskRequest(
                "idea",
                "desc",
                TaskType.IDEA,
                null,
                null,
                "아이디어",
                false
        );

        given(taskRepository.save(any(Task.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        TaskResponse res = taskService.create(request);

        // then
        assertThat(res.type()).isEqualTo(TaskType.IDEA);
        assertThat(res.unscheduled()).isTrue();
        assertThat(res.status()).isEqualTo(TaskStatus.INBOX);
        assertThat(res.targetDate()).isNull();
        assertThat(res.completedAt()).isNull();

        then(taskRepository).should(times(1)).save(any(Task.class));
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 등록 성공 - 기간 일정은 [startInclusive, endExclusive)로 저장된다")
    void createTask_success_period_endExclusive() {
        // given
        LocalDateTime startInclusive = LocalDateTime.of(2025, 1, 22, 10, 30);
        LocalDateTime endExclusive = LocalDateTime.of(2025, 1, 22, 11, 30);

        Task saved = Task.builder()
                .title("period")
                .description("desc")
                .startAt(startInclusive)
                .endAt(endExclusive)
                .category("일")
                .allDay(false)
                .build();

        given(taskRepository.save(any())).willReturn(saved);

        TaskRequest request = new TaskRequest(
                "period",
                "desc",
                startInclusive,
                endExclusive,
                "일",
                false
        );

        // when
        TaskResponse res = taskService.create(request);

        // then
        assertThat(res.startAt()).isEqualTo(startInclusive);
        assertThat(res.endAt()).isEqualTo(endExclusive);
        assertThat(res.endAt()).isAfter(res.startAt());

        then(taskRepository).should(times(1)).save(any(Task.class));
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 등록 성공 - 종일(allDay) 일정은 00:00 ~ 다음날 00:00(endExclusive) 형태다")
    void createTask_success_allDay_endExclusive() {
        // given
        LocalDateTime startInclusive = LocalDate.of(2025, 1, 22).atStartOfDay();      // 01-22 00:00
        LocalDateTime endExclusive = LocalDate.of(2025, 1, 23).atStartOfDay();        // 01-23 00:00

        Task saved = Task.builder()
                .title("allDay")
                .description("desc")
                .startAt(startInclusive)
                .endAt(endExclusive)
                .category("집")
                .allDay(true)
                .build();

        given(taskRepository.save(any())).willReturn(saved);

        TaskRequest request = new TaskRequest(
                "allDay",
                "desc",
                startInclusive,
                endExclusive,
                "집",
                true
        );

        // when
        TaskResponse res = taskService.create(request);

        // then
        assertThat(res.allDay()).isTrue();
        assertThat(res.startAt()).isEqualTo(startInclusive);
        assertThat(res.endAt()).isEqualTo(endExclusive);

        // [start, end)로 하루 전체를 표현한다: endExclusive 는 start+1day(00:00)
        assertThat(res.endAt()).isEqualTo(res.startAt().plusDays(1));

        then(taskRepository).should(times(1)).save(any(Task.class));
        then(taskTxService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 조회 (단건)
     *******************/
    @Test
    @DisplayName("일정 조회(단건) 성공")
    void getTask_success() {
        // given
        Long taskId = 1L;
        LocalDateTime startAt = LocalDateTime.of(2025, 12, 16, 10, 0);

        Task task = Task.builder()
                .title("테스트 일정")
                .description("설명")
                .startAt(startAt)
                .endAt(null)
                .category("일")
                .allDay(false)
                .build();

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

        // when
        TaskResponse res = taskService.getTask(taskId);

        // then
        assertThat(res.title()).isEqualTo("테스트 일정");
        assertThat(res.startAt()).isEqualTo(startAt);
        assertThat(res.endAt()).isNull();
        assertThat(res.unscheduled()).isFalse();

        then(taskRepository).should(times(1)).findById(taskId);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 조회(단건) 실패 - 존재하지 않는 ID면 TaskNotFoundException 발생")
    void getTask_notFound() {
        // given
        Long taskId = 999L;
        given(taskRepository.findById(taskId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> taskService.getTask(taskId))
                .isInstanceOf(TaskNotFoundException.class)
                .satisfies(ex -> {
                    TaskNotFoundException e = (TaskNotFoundException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
                });

        then(taskRepository).should(times(1)).findById(taskId);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 조회 (DAY / WEEK / MONTH)
     *******************/
    @Test
    @DisplayName("일정 조회 성공 - DAY는 [dayStart, nextDayStart) 기준으로 조회한다")
    void getTasks_day_success_endExclusive_boundary() {
        // given
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("DAY")
                .rawDate("2025-11-27")
                .build();

        LocalDate day = LocalDate.of(2025, 11, 27);
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime nextDayStart = day.plusDays(1).atStartOfDay();

        List<Task> returnedByRepo = List.of(
                Task.builder().title("include-start").description("d1")
                        .startAt(dayStart)
                        .endAt(null).allDay(false).category("일").build(),
                Task.builder().title("include-late").description("d2")
                        .startAt(LocalDateTime.of(2025, 11, 27, 23, 59))
                        .endAt(null).allDay(false).category("일").build()
        );

        given(taskRepository.findByDateRangeAndType(dayStart, nextDayStart, TaskType.SCHEDULE)).willReturn(returnedByRepo);

        // when
        List<TaskResponse> res = taskService.getTasks(request);

        // then
        assertThat(res).hasSize(2);
        assertThat(res).extracting(TaskResponse::title)
                .containsExactlyInAnyOrder("include-start", "include-late");
        assertThat(res).allMatch(r -> !r.unscheduled());
        assertThat(res).allMatch(r ->
                !r.startAt().isBefore(dayStart) && r.startAt().isBefore(nextDayStart)
        );

        then(taskRepository).should(times(1)).findByDateRangeAndType(dayStart, nextDayStart, TaskType.SCHEDULE);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 조회 성공 - WEEK는 [weekStart, weekEndExclusive) 기준으로 조회한다")
    void getTasks_week_success_endExclusive_boundary() {
        // given
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("WEEK")
                .rawDate("2025-11-27")
                .build();

        DateRange week = TaskQueryType.WEEK.calculate("2025-11-27");

        LocalDateTime weekStart = week.getStart();
        LocalDateTime weekEndExclusive = week.getEnd();

        List<Task> returnedByRepo = List.of(
                Task.builder().title("mon").description("d1")
                        .startAt(weekStart)
                        .endAt(null).allDay(false).category("일").build(),
                Task.builder().title("sun-2359").description("d2")
                        .startAt(weekEndExclusive.minusMinutes(1))
                        .endAt(null).allDay(false).category("일").build()
        );

        given(taskRepository.findByDateRangeAndType(weekStart, weekEndExclusive, TaskType.SCHEDULE)).willReturn(returnedByRepo);

        // when
        List<TaskResponse> res = taskService.getTasks(request);

        // then
        assertThat(res).hasSize(2);
        assertThat(res).extracting(TaskResponse::title)
                .containsExactlyInAnyOrder("mon", "sun-2359");
        assertThat(res).allMatch(r ->
                !r.startAt().isBefore(weekStart) && r.startAt().isBefore(weekEndExclusive)
        );

        then(taskRepository).should(times(1)).findByDateRangeAndType(weekStart, weekEndExclusive, TaskType.SCHEDULE);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 조회 성공 - MONTH는 [monthStart, monthEndExclusive) 기준으로 조회한다")
    void getTasks_month_success_endExclusive_boundary() {
        // given
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("MONTH")
                .rawDate("2025-11")
                .build();

        DateRange month = TaskQueryType.MONTH.calculate("2025-11");
        LocalDateTime monthStart = month.getStart();
        LocalDateTime monthEndExclusive = month.getEnd();

        List<Task> returnedByRepo = List.of(
                Task.builder().title("m-start").description("d1")
                        .startAt(monthStart)
                        .endAt(null).allDay(false).category("일").build(),
                Task.builder().title("m-end-1m").description("d2")
                        .startAt(monthEndExclusive.minusMinutes(1))
                        .endAt(null).allDay(false).category("일").build()
        );

        given(taskRepository.findByDateRangeAndType(monthStart, monthEndExclusive, TaskType.SCHEDULE)).willReturn(returnedByRepo);

        // when
        List<TaskResponse> res = taskService.getTasks(request);

        // then
        assertThat(res).hasSize(2);
        assertThat(res).extracting(TaskResponse::title)
                .containsExactlyInAnyOrder("m-start", "m-end-1m");
        assertThat(res).allMatch(r ->
                !r.startAt().isBefore(monthStart) && r.startAt().isBefore(monthEndExclusive)
        );

        then(taskRepository).should(times(1)).findByDateRangeAndType(monthStart, monthEndExclusive, TaskType.SCHEDULE);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 조회 성공 - 요청한 TaskType 기준으로 조회한다")
    void getTasks_success_filtersTaskType() {
        // given
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("DAY")
                .rawTaskType("IDEA")
                .rawDate("2026-05-17")
                .build();

        DateRange day = TaskQueryType.DAY.calculate("2026-05-17");
        Task idea = Task.builder()
                .title("idea")
                .type(TaskType.IDEA)
                .startAt(day.getStart().plusHours(1))
                .endAt(null)
                .allDay(false)
                .category("아이디어")
                .build();

        given(taskRepository.findByDateRangeAndType(day.getStart(), day.getEnd(), TaskType.IDEA))
                .willReturn(List.of(idea));

        // when
        List<TaskResponse> res = taskService.getTasks(request);

        // then
        assertThat(res).hasSize(1);
        assertThat(res.getFirst().type()).isEqualTo(TaskType.IDEA);
        assertThat(res.getFirst().title()).isEqualTo("idea");

        then(taskRepository).should(times(1)).findByDateRangeAndType(day.getStart(), day.getEnd(), TaskType.IDEA);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("카테고리 그룹 일정 조회는 미분류를 마지막에 정렬한다")
    void getGroupedTasks_ordersUncategorizedLast() {
        // given
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("DAY")
                .rawDate("2025-11-27")
                .build();

        DateRange day = TaskQueryType.DAY.calculate("2025-11-27");
        Task uncategorized = Task.builder()
                .title("u")
                .startAt(day.getStart())
                .endAt(null)
                .allDay(false)
                .category(null)
                .build();
        Task work = Task.builder()
                .title("w")
                .startAt(day.getStart().plusHours(1))
                .endAt(null)
                .allDay(false)
                .category("WORK")
                .build();

        given(taskRepository.findByDateRangeAndType(day.getStart(), day.getEnd(), TaskType.SCHEDULE))
                .willReturn(List.of(uncategorized, work));

        // when
        List<TaskCategoryGroupResponse> result = taskService.getGroupedTasks(request);

        // then
        assertThat(result).extracting(TaskCategoryGroupResponse::category)
                .containsExactly("WORK", "미분류");

        then(taskRepository).should(times(1)).findByDateRangeAndType(day.getStart(), day.getEnd(), TaskType.SCHEDULE);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 수정
     *******************/
    @Test
    @DisplayName("일정 수정 성공")
    void updateTask_success() {
        // given
        long id = 10L;

        LocalDateTime updatedStartAt = LocalDateTime.of(2026, 1, 20, 1, 20);

        Task updated = Task.builder()
                .title("수정 title")
                .description("수정 desc")
                .startAt(updatedStartAt)
                .endAt(null)
                .category("집")
                .allDay(false)
                .build();

        TaskRequest req = new TaskRequest(
                "수정 title",
                "수정 desc",
                LocalDateTime.of(2026, 12, 20, 9, 15),
                null,
                "집",
                false
        );

        given(taskTxService.updateTx(id, req)).willReturn(updated);

        // when
        TaskResponse res = taskService.update(id, req);

        // then
        assertThat(res.title()).isEqualTo("수정 title");
        assertThat(res.startAt()).isEqualTo(updatedStartAt);
        assertThat(res.endAt()).isNull();

        then(taskTxService).should(times(1)).updateTx(id, req);
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 수정 실패 - 존재하지 않는 ID면 TaskNotFoundException 발생")
    void updateTask_notFound() {
        // given
        long id = 999L;

        TaskRequest req = new TaskRequest(
                "t",
                "d",
                LocalDateTime.of(2026, 1, 20, 1, 25),
                null,
                "일",
                false
        );

        given(taskTxService.updateTx(id, req)).willThrow(new TaskNotFoundException(id));

        // when & then
        assertThatThrownBy(() -> taskService.update(id, req))
                .isInstanceOf(TaskNotFoundException.class)
                .satisfies(ex -> {
                    TaskNotFoundException e = (TaskNotFoundException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
                });

        then(taskTxService).should(times(1)).updateTx(id, req);
        then(taskRepository).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 삭제
     *******************/
    @Test
    @DisplayName("일정 삭제 성공")
    void deleteTask_success() {
        // given
        long id = 1L;
        given(taskRepository.existsById(id)).willReturn(true);

        // when
        taskService.delete(id);

        // then
        InOrder inOrder = inOrder(taskRepository);
        inOrder.verify(taskRepository).existsById(id);
        inOrder.verify(taskRepository).deleteById(id);

        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 삭제 실패 - 존재하지 않는 ID면 TaskNotFoundException 발생")
    void deleteTask_notFound() {
        // given
        long id = 999L;
        given(taskRepository.existsById(id)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> taskService.delete(id))
                .isInstanceOf(TaskNotFoundException.class)
                .satisfies(ex -> {
                    TaskNotFoundException e = (TaskNotFoundException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
                });

        then(taskRepository).should(times(1)).existsById(id);
        then(taskRepository).should(never()).deleteById(id);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    /*******************
     *  미정 일정 조화
     *******************/
    @Test
    @DisplayName("미정 일정 조회는 startAt/endAt이 모두 null 인 일정만 반환한다")
    void getUnscheduledTasks_onlyNullStartAndEnd() {
        // given
        Task unscheduled1 = Task.builder()
                .title("u1")
                .startAt(null)
                .endAt(null)
                .build();

        Task unscheduled2 = Task.builder()
                .title("u2")
                .startAt(null)
                .endAt(null)
                .category("WORK")
                .build();

        given(taskRepository.findUnscheduledTask())
                .willReturn(List.of(unscheduled1, unscheduled2));

        // when
        List<TaskResponse> result = taskService.getUnscheduledTasks();

        // then
        assertThat(result).hasSize(2);

        assertThat(result).extracting(TaskResponse::title)
                .containsExactly("u1", "u2");
        assertThat(result).allMatch(TaskResponse::unscheduled);
        assertThat(result).allMatch(task -> task.startAt() == null && task.endAt() == null);

        then(taskRepository).should(times(1)).findUnscheduledTask();
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Inbox 조회는 INBOX 상태 Task를 반환한다")
    void getInboxTasks_success() {
        // given
        Task inbox = Task.builder()
                .title("inbox")
                .status(TaskStatus.INBOX)
                .build();

        given(taskRepository.findByStatus(TaskStatus.INBOX))
                .willReturn(List.of(inbox));

        // when
        List<TaskResponse> result = taskService.getInboxTasks();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("inbox");
        assertThat(result.getFirst().status()).isEqualTo(TaskStatus.INBOX);

        then(taskRepository).should(times(1)).findByStatus(TaskStatus.INBOX);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Today 조회는 targetDate 기준 Task를 반환한다")
    void getTodayTasks_success() {
        // given
        LocalDate targetDate = LocalDate.of(2026, 5, 20);
        Task today = Task.builder()
                .title("today")
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .build();

        given(taskRepository.findTodayTasks(targetDate))
                .willReturn(List.of(today));

        // when
        List<TaskResponse> result = taskService.getTodayTasks(targetDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("today");
        assertThat(result.getFirst().status()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.getFirst().targetDate()).isEqualTo(targetDate);

        then(taskRepository).should(times(1)).findTodayTasks(targetDate);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Done 조회는 completedAt 날짜 기준 Task를 반환한다")
    void getDoneTasks_success() {
        // given
        LocalDate completedDate = LocalDate.of(2026, 5, 20);
        LocalDateTime completedAt = completedDate.atTime(21, 0);
        Task done = Task.builder()
                .title("done")
                .status(TaskStatus.DONE)
                .completedAt(completedAt)
                .build();

        given(taskRepository.findDoneTasks(completedDate))
                .willReturn(List.of(done));

        // when
        List<TaskResponse> result = taskService.getDoneTasks(completedDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("done");
        assertThat(result.getFirst().status()).isEqualTo(TaskStatus.DONE);
        assertThat(result.getFirst().completedAt()).isEqualTo(completedAt);

        then(taskRepository).should(times(1)).findDoneTasks(completedDate);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Today 이동은 트랜잭션 서비스에 위임하고 응답을 반환한다")
    void moveToToday_success() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 5, 21);
        Task moved = Task.builder()
                .title("moved")
                .status(TaskStatus.TODAY)
                .targetDate(targetDate)
                .build();

        given(taskTxService.moveToTodayTx(id, targetDate)).willReturn(moved);

        // when
        TaskResponse result = taskService.moveToToday(id, targetDate);

        // then
        assertThat(result.title()).isEqualTo("moved");
        assertThat(result.status()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.targetDate()).isEqualTo(targetDate);
        assertThat(result.completedAt()).isNull();

        then(taskTxService).should(times(1)).moveToTodayTx(id, targetDate);
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("완료 처리는 트랜잭션 서비스에 위임하고 응답을 반환한다")
    void complete_success() {
        // given
        long id = 1L;
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 21, 22, 0);
        Task completed = Task.builder()
                .title("completed")
                .status(TaskStatus.DONE)
                .completedAt(completedAt)
                .build();

        given(taskTxService.completeTx(id, completedAt)).willReturn(completed);

        // when
        TaskResponse result = taskService.complete(id, completedAt);

        // then
        assertThat(result.title()).isEqualTo("completed");
        assertThat(result.status()).isEqualTo(TaskStatus.DONE);
        assertThat(result.completedAt()).isEqualTo(completedAt);

        then(taskTxService).should(times(1)).completeTx(id, completedAt);
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이월 처리는 트랜잭션 서비스에 위임하고 응답을 반환한다")
    void carryOver_success() {
        // given
        long id = 1L;
        LocalDate nextDate = LocalDate.of(2026, 5, 22);
        Task carriedOver = Task.builder()
                .title("carried over")
                .status(TaskStatus.TODAY)
                .targetDate(nextDate)
                .build();

        given(taskTxService.carryOverTx(id, nextDate)).willReturn(carriedOver);

        // when
        TaskResponse result = taskService.carryOver(id, nextDate);

        // then
        assertThat(result.title()).isEqualTo("carried over");
        assertThat(result.status()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.targetDate()).isEqualTo(nextDate);
        assertThat(result.completedAt()).isNull();

        then(taskTxService).should(times(1)).carryOverTx(id, nextDate);
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("D-Day 연결은 트랜잭션 서비스에 위임하고 응답을 반환한다")
    void connectDdayGoal_success() {
        // given
        long id = 1L;
        long ddayGoalId = 10L;
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));
        ReflectionTestUtils.setField(goal, "id", ddayGoalId);
        Task connected = Task.builder()
                .title("기출 20문제 풀기")
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 31))
                .ddayGoal(goal)
                .build();

        given(taskTxService.connectDdayGoalTx(id, ddayGoalId)).willReturn(connected);

        // when
        TaskResponse result = taskService.connectDdayGoal(id, ddayGoalId);

        // then
        assertThat(result.title()).isEqualTo("기출 20문제 풀기");
        assertThat(result.ddayGoalId()).isEqualTo(ddayGoalId);
        assertThat(result.ddayGoalTitle()).isEqualTo("정보처리기사");
        assertThat(result.ddayGoalTargetDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(result.ddayDaysLeft()).isNotNull();

        then(taskTxService).should(times(1)).connectDdayGoalTx(id, ddayGoalId);
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("D-Day 연결 해제는 트랜잭션 서비스에 위임하고 응답을 반환한다")
    void disconnectDdayGoal_success() {
        // given
        long id = 1L;
        Task disconnected = Task.builder()
                .title("기출 20문제 풀기")
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 31))
                .build();

        given(taskTxService.disconnectDdayGoalTx(id)).willReturn(disconnected);

        // when
        TaskResponse result = taskService.disconnectDdayGoal(id);

        // then
        assertThat(result.title()).isEqualTo("기출 20문제 풀기");
        assertThat(result.ddayGoalId()).isNull();

        then(taskTxService).should(times(1)).disconnectDdayGoalTx(id);
        then(taskRepository).shouldHaveNoInteractions();
    }
}
