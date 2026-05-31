package com.todolab.dday.repository;

import com.todolab.dday.domain.DdayGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DdayGoalRepository extends JpaRepository<DdayGoal, Long> {

    List<DdayGoal> findAllByOrderByTargetDateAscIdAsc();

    List<DdayGoal> findByTargetDateBetweenOrderByTargetDateAscIdAsc(LocalDate startDate, LocalDate endDate);
}
