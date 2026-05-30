package com.todolab.dday.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "`DDAY_GOAL`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DdayGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`ID`")
    private Long id;

    @Column(name = "`TITLE`", nullable = false)
    private String title;

    @Column(name = "`TARGET_DATE`", nullable = false)
    private LocalDate targetDate;

    @Column(name = "`CREATED_AT`", nullable = false)
    private LocalDateTime createdAt;

    public DdayGoal(String title, LocalDate targetDate) {
        update(title, targetDate);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String title, LocalDate targetDate) {
        String normalizedTitle = normalizeTitle(title);
        if (normalizedTitle == null) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate는 필수입니다.");
        }

        this.title = normalizedTitle;
        this.targetDate = targetDate;
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }

        String normalized = title.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
