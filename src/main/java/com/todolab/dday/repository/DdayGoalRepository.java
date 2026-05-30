package com.todolab.dday.repository;

import com.todolab.dday.domain.DdayGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DdayGoalRepository extends JpaRepository<DdayGoal, Long> {

    List<DdayGoal> findAllByOrderByTargetDateAscIdAsc();
}
