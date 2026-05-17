package com.todolab.task.domain;

import com.todolab.Constant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "`CREATED_AT`")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


    @Builder
    public Task(String title, String description, TaskType type, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        apply(title, description, type, startAt, endAt, allDay, category);
    }

    public void update(String title, String description, TaskType type, LocalDateTime startAt, LocalDateTime endAt, boolean allDay, String category) {
        apply(title, description, type, startAt, endAt, allDay, category);
    }

    public boolean isUnscheduled() {
        return startAt == null && endAt == null;
    }

    public boolean isPeriodTask() {
        return startAt != null && endAt != null;
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
    }

    private TaskType normalizeType(TaskType type) {
        return type == null ? TaskType.defaultType() : type;
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
