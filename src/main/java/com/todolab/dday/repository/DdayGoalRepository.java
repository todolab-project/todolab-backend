package com.todolab.dday.repository;

import com.todolab.dday.domain.DdayGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DdayGoalRepository extends JpaRepository<DdayGoal, Long> {

    List<DdayGoal> findAllByOrderByTargetDateAscIdAsc();

    List<DdayGoal> findAllByOwnerIdOrderByTargetDateAscIdAsc(Long ownerId);

    List<DdayGoal> findByTargetDateBetweenOrderByTargetDateAscIdAsc(LocalDate startDate, LocalDate endDate);

    List<DdayGoal> findByOwnerIdAndTargetDateBetweenOrderByTargetDateAscIdAsc(Long ownerId, LocalDate startDate, LocalDate endDate);

    Optional<DdayGoal> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
