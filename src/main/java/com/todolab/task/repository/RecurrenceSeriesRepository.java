package com.todolab.task.repository;

import com.todolab.task.domain.RecurrenceSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecurrenceSeriesRepository extends JpaRepository<RecurrenceSeries, Long> {

    List<RecurrenceSeries> findByOwnerId(Long ownerId);

    Optional<RecurrenceSeries> findByIdAndOwnerId(Long id, Long ownerId);
}
