package com.todolab.task.domain;

import com.todolab.Constant;
import com.todolab.dday.domain.DdayGoal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Entity
@Table(name = "`TASK`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    public static final int STALE_CARRY_OVER_THRESHOLD = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID`")
    private Long id;

    @Column(name = "`TITLE`")
    private String title;

    @Column(name = "`DESCRIPTION`")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "`TYPE`", nullable = false)
    private TaskType type;

    /***
     * 미정 : startAt == null && endAt == null
     * 단일 : startAt != null && endAt == null
     * 기간 : startAT != null && endAt != null
     */
    @Column(name = "`START_AT`")
    private LocalDateTime startAt;

    @Column(name = "`END_AT`")
    private LocalDateTime endAt;

    /***
     * 종일 일정 여부
     *  - true 면 시간 입력 개념이 없으며, startAt/endAt은 00:00으로 정규화되어야 함
     */
    @Column(name = "`ALL_DAY`")
    private boolean allDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "`SCHEDULE_SOURCE`")
    private ScheduleSource scheduleSource;

    @Column(name = "`CATEGORY`")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "`STATUS`", nullable = false)
    private TaskStatus status;

    @Column(name = "`TARGET_DATE`")
    private LocalDate targetDate;

    @Column(name = "`COMPLETED_AT`")
    private LocalDateTime completedAt;

    @Column(name = "`CARRY_OVER_COUNT`", nullable = false)
    private int carryOverCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "`DEFER_REASON`")
    private DeferReason deferReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`DDAY_GOAL_ID`")
    private DdayGoal ddayGoal;

    @Column(name = "`CREATED_AT`")
    private LocalDateTime createdAt;

    @Column(name = "`UPDATED_AT`")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            applyInitialStatus();
        }
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    @Builder
    public Task(String title, String description, TaskType type, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category,
                TaskStatus status, LocalDate targetDate, LocalDateTime completedAt, Integer carryOverCount,
                DeferReason deferReason, DdayGoal ddayGoal, ScheduleSource scheduleSource) {
        apply(title, description, type, startAt, endAt, allDay, category, scheduleSource);
        applyStatus(status, targetDate, completedAt);
        this.carryOverCount = carryOverCount == null ? 0 : Math.max(0, carryOverCount);
        this.deferReason = deferReason;
        this.ddayGoal = ddayGoal;
    }

    public void update(String title, String description, TaskType type, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        ScheduleSource updatedScheduleSource = resolveUpdatedScheduleSource(startAt, endAt, allDay);
        apply(title, description, type, startAt, endAt, allDay, category, updatedScheduleSource);
        if (this.status != TaskStatus.DONE && !isUnscheduled()) {
            applyInitialStatus();
        }
    }

    public boolean isUnscheduled() {
        return startAt == null && endAt == null;
    }

    public boolean isPeriodTask() {
        return startAt != null && endAt != null;
    }

    public boolean isStaleCarryOver() {
        return carryOverCount >= STALE_CARRY_OVER_THRESHOLD;
    }

    public void moveToInbox() {
        clearSchedule();
        this.status = TaskStatus.INBOX;
        this.targetDate = null;
        this.completedAt = null;
    }

    public void moveToToday(LocalDate targetDate) {
        validateTargetDate(targetDate);
        moveScheduleTo(targetDate);
        this.status = TaskStatus.TODAY;
        this.targetDate = targetDate;
        this.completedAt = null;
    }

    public void complete(LocalDateTime completedAt) {
        validateCompletedAt(completedAt);
        this.status = TaskStatus.DONE;
        this.completedAt = completedAt;
    }

    public void reopenToday(LocalDate targetDate) {
        validateTargetDate(targetDate);
        moveToToday(targetDate);
    }

    public void carryOverTo(LocalDate nextDate) {
        validateTargetDate(nextDate);
        moveScheduleTo(nextDate);
        this.status = TaskStatus.TODAY;
        this.targetDate = nextDate;
        this.completedAt = null;
        this.carryOverCount++;
    }

    public void connectDdayGoal(DdayGoal ddayGoal) {
        if (ddayGoal == null) {
            throw new IllegalArgumentException("ddayGoal은 필수입니다.");
        }
        this.ddayGoal = ddayGoal;
    }

    public void disconnectDdayGoal() {
        this.ddayGoal = null;
    }

    public void setDeferReason(DeferReason deferReason) {
        if (deferReason == null) {
            throw new IllegalArgumentException("deferReason은 필수입니다.");
        }
        this.deferReason = deferReason;
    }

    public void clearDeferReason() {
        this.deferReason = null;
    }

    private void apply(
            String title,
            String description,
            TaskType type,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay,
            String category,
            ScheduleSource scheduleSource
    ) {
        validateSchedule(startAt, endAt, allDay);
        ScheduleSource normalizedScheduleSource = normalizeScheduleSource(startAt, endAt, scheduleSource);

        String normalizedCategory = normalizeCategory(category);
        validateCategory(normalizedCategory);

        this.title = title;
        this.description = description;
        this.type = normalizeType(type);
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.scheduleSource = normalizedScheduleSource;
        this.category = normalizedCategory;

        if (this.status == null) {
            applyInitialStatus();
        }
    }

    private TaskType normalizeType(TaskType type) {
        return type == null ? TaskType.defaultType() : type;
    }

    private ScheduleSource normalizeScheduleSource(
            LocalDateTime startAt,
            LocalDateTime endAt,
            ScheduleSource scheduleSource
    ) {
        if (startAt == null && endAt == null) {
            return null;
        }
        return scheduleSource == null ? ScheduleSource.USER : scheduleSource;
    }

    private ScheduleSource resolveUpdatedScheduleSource(
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay
    ) {
        if (startAt == null && endAt == null) {
            return null;
        }

        boolean scheduleUnchanged = Objects.equals(this.startAt, startAt)
                && Objects.equals(this.endAt, endAt)
                && this.allDay == allDay;
        if (scheduleUnchanged && this.scheduleSource != null) {
            return this.scheduleSource;
        }
        return ScheduleSource.USER;
    }

    private void clearSchedule() {
        this.startAt = null;
        this.endAt = null;
        this.allDay = false;
        this.scheduleSource = null;
        if (this.type == TaskType.SCHEDULE) {
            this.type = TaskType.TODO;
        }
    }

    private void applyAutoTodaySchedule(LocalDate targetDate) {
        this.startAt = targetDate.atStartOfDay();
        this.endAt = targetDate.plusDays(1).atStartOfDay();
        this.allDay = true;
        this.scheduleSource = ScheduleSource.AUTO_TODAY;
    }

    private void moveScheduleTo(LocalDate targetDate) {
        if (isUnscheduled()) {
            applyAutoTodaySchedule(targetDate);
            return;
        }

        long daysToMove = ChronoUnit.DAYS.between(this.startAt.toLocalDate(), targetDate);
        this.startAt = this.startAt.plusDays(daysToMove);
        if (this.endAt != null) {
            this.endAt = this.endAt.plusDays(daysToMove);
        }
    }

    private void validateTargetDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate는 필수입니다.");
        }
    }

    private void validateCompletedAt(LocalDateTime completedAt) {
        if (completedAt == null) {
            throw new IllegalArgumentException("completedAt은 필수입니다.");
        }
    }

    private void applyStatus(TaskStatus status, LocalDate targetDate, LocalDateTime completedAt) {
        if (status == null) {
            applyInitialStatus();
            return;
        }

        this.status = status;
        this.targetDate = targetDate;
        this.completedAt = completedAt;
    }

    private void applyInitialStatus() {
        if (isUnscheduled()) {
            this.status = TaskStatus.INBOX;
            this.targetDate = null;
            this.completedAt = null;
            return;
        }

        this.status = TaskStatus.TODAY;
        this.targetDate = startAt.toLocalDate();
        this.completedAt = null;
    }

    private void validateSchedule(LocalDateTime startAt, LocalDateTime endAt, boolean allDay) {
        validateEndAtWithoutStartAt(startAt, endAt);
        validateUnscheduledAllDay(startAt, endAt, allDay);
        validateSingleSchedule(startAt, endAt, allDay);
        validatePeriodSchedule(startAt, endAt, allDay);
    }

    private void validateEndAtWithoutStartAt(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null && endAt != null) {
            throw new IllegalArgumentException("endAt이 존재하면 startAt은 필수입니다.");
        }
    }

    private void validateUnscheduledAllDay(LocalDateTime startAt, LocalDateTime endAt, boolean allDay) {
        if (startAt == null && endAt == null && allDay) {
            throw new IllegalArgumentException("미정 일정에는 allDay 를 설정할 수 없습니다.");
        }
    }

    private void validateSingleSchedule(LocalDateTime startAt, LocalDateTime endAt, boolean allDay) {
        if (startAt == null || endAt != null) {
            return;
        }

        if (allDay && !isMidnight(startAt)) {
            throw new IllegalArgumentException("allDay 일정의 startAt은 00:00 이어야 합니다.");
        }
    }

    private void validatePeriodSchedule(LocalDateTime startAt, LocalDateTime endAt, boolean allDay) {
        if (startAt == null || endAt == null) {
            return;
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt은 startAt 이후여야 합니다.");
        }

        if (allDay && (!isMidnight(startAt) || !isMidnight(endAt))) {
            throw new IllegalArgumentException("allDay 일정의 startAt/endAt은 00:00 이어야 합니다.");
        }
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }

        String normalized = category.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateCategory(String category) {
        if (category == null) {
            return;
        }

        if (Constant.UNCATEGORIZED.equals(category)) {
            throw new IllegalArgumentException("'미분류'는 카테고리명으로 사용할 수 없습니다.");
        }
    }

    private boolean isMidnight(LocalDateTime dateTime) {
        return dateTime.toLocalTime().equals(LocalTime.MIDNIGHT);
    }

}
