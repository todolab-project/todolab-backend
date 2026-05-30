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

@Entity
@Table(name = "`TASK`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

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

    @Column(name = "`CATEGORY`")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "`STATUS`", nullable = false)
    private TaskStatus status;

    @Column(name = "`TARGET_DATE`")
    private LocalDate targetDate;

    @Column(name = "`COMPLETED_AT`")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`DDAY_GOAL_ID`")
    private DdayGoal ddayGoal;

    @Column(name = "`CREATED_AT`")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            applyInitialStatus();
        }
        this.createdAt = LocalDateTime.now();
    }


    @Builder
    public Task(String title, String description, TaskType type, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category,
                TaskStatus status, LocalDate targetDate, LocalDateTime completedAt, DdayGoal ddayGoal) {
        apply(title, description, type, startAt, endAt, allDay, category);
        applyStatus(status, targetDate, completedAt);
        this.ddayGoal = ddayGoal;
    }

    public void update(String title, String description, TaskType type, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        apply(title, description, type, startAt, endAt, allDay, category);
        if (this.status != TaskStatus.DONE) {
            applyInitialStatus();
        }
    }

    public boolean isUnscheduled() {
        return startAt == null && endAt == null;
    }

    public boolean isPeriodTask() {
        return startAt != null && endAt != null;
    }

    public void moveToInbox() {
        this.status = TaskStatus.INBOX;
        this.targetDate = null;
        this.completedAt = null;
    }

    public void moveToToday(LocalDate targetDate) {
        validateTargetDate(targetDate);
        this.status = TaskStatus.TODAY;
        this.targetDate = targetDate;
        this.completedAt = null;
    }

    public void complete(LocalDateTime completedAt) {
        validateCompletedAt(completedAt);
        this.status = TaskStatus.DONE;
        this.completedAt = completedAt;
    }

    public void carryOverTo(LocalDate nextDate) {
        validateTargetDate(nextDate);
        this.status = TaskStatus.TODAY;
        this.targetDate = nextDate;
        this.completedAt = null;
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

    private void apply(String title, String description, TaskType type, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        validateSchedule(startAt, endAt, allDay);

        String normalizedCategory = normalizeCategory(category);
        validateCategory(normalizedCategory);

        this.title = title;
        this.description = description;
        this.type = normalizeType(type);
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.category = normalizedCategory;

        if (this.status == null) {
            applyInitialStatus();
        }
    }

    private TaskType normalizeType(TaskType type) {
        return type == null ? TaskType.defaultType() : type;
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
