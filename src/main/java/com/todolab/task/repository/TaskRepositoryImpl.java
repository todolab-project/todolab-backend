package com.todolab.task.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todolab.task.domain.QTask;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.domain.TaskType;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TaskRepositoryImpl implements TaskRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public TaskRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Task> findByDateRange(LocalDateTime start, LocalDateTime end) {
        QTask t = QTask.task;

        return queryFactory
                .selectFrom(t)
                .where(
                        t.startAt.isNotNull(),
                        overlapsRange(t, start, end)
                )
                .orderBy(t.startAt.asc(), t.id.asc())
                .fetch();
    }

    @Override
    public List<Task> findByDateRangeAndType(LocalDateTime start, LocalDateTime end, TaskType taskType) {
        QTask t = QTask.task;

        return queryFactory
                .selectFrom(t)
                .where(
                        t.type.eq(taskType),
                        t.startAt.isNotNull(),
                        overlapsRange(t, start, end)
                )
                .orderBy(t.startAt.asc(), t.id.asc())
                .fetch();
    }

    @Override
    public List<Task> findUnscheduledTask() {
        QTask t = QTask.task;

        return queryFactory
                .selectFrom(t)
                .where(
                        t.startAt.isNull(),
                        t.endAt.isNull()
                )
                .orderBy(t.id.asc())
                .fetch();
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        QTask t = QTask.task;

        return queryFactory
                .selectFrom(t)
                .where(t.status.eq(status))
                .orderBy(t.createdAt.asc(), t.id.asc())
                .fetch();
    }

    @Override
    public List<Task> findTodayTasks(LocalDate targetDate) {
        QTask t = QTask.task;

        return queryFactory
                .selectFrom(t)
                .where(
                        t.status.eq(TaskStatus.TODAY),
                        t.targetDate.eq(targetDate)
                )
                .orderBy(t.createdAt.asc(), t.id.asc())
                .fetch();
    }

    @Override
    public List<Task> findDoneTasks(LocalDate completedDate) {
        QTask t = QTask.task;
        LocalDateTime start = completedDate.atStartOfDay();
        LocalDateTime end = completedDate.plusDays(1).atStartOfDay();

        return queryFactory
                .selectFrom(t)
                .where(
                        t.status.eq(TaskStatus.DONE),
                        t.completedAt.goe(start),
                        t.completedAt.lt(end)
                )
                .orderBy(t.completedAt.desc(), t.id.asc())
                .fetch();
    }

    @Override
    public List<Task> findByDdayGoalId(Long ddayGoalId) {
        QTask t = QTask.task;

        return queryFactory
                .selectFrom(t)
                .where(t.ddayGoal.id.eq(ddayGoalId))
                .orderBy(t.targetDate.asc().nullsLast(), t.createdAt.asc(), t.id.asc())
                .fetch();
    }

    private BooleanExpression overlapsRange(QTask t, LocalDateTime start, LocalDateTime end) {
        return singleScheduleInRange(t, start, end)
                .or(periodScheduleOverlapsRange(t, start, end));
    }

    // 단일 일정은 시작 시각이 조회 범위 [start, end)에 포함되면 조회한다.
    private BooleanExpression singleScheduleInRange(QTask t, LocalDateTime start, LocalDateTime end) {
        return t.endAt.isNull()
                .and(t.startAt.goe(start))
                .and(t.startAt.lt(end));
    }

    // 기간 일정은 일정 구간과 조회 범위가 겹치면 조회한다.
    private BooleanExpression periodScheduleOverlapsRange(QTask t, LocalDateTime start, LocalDateTime end) {
        return t.endAt.isNotNull()
                .and(t.startAt.lt(end))
                .and(t.endAt.gt(start));
    }
}
