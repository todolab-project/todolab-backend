package com.todolab.dday.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.dto.DdayGoalResponse;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DdayGoalService {

    private final DdayGoalRepository ddayGoalRepository;

    @Transactional
    public DdayGoalResponse create(DdayGoalRequest request) {
        DdayGoal saved = ddayGoalRepository.save(new DdayGoal(request.title(), request.targetDate()));
        return DdayGoalResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DdayGoalResponse> findAll() {
        return ddayGoalRepository.findAllByOrderByTargetDateAscIdAsc().stream()
                .map(DdayGoalResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!ddayGoalRepository.existsById(id)) {
            throw new DdayGoalNotFoundException(id);
        }
        ddayGoalRepository.deleteById(id);
    }
}
