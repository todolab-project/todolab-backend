package com.todolab.dday.service;

import com.todolab.dday.domain.DdayGoal;
import com.todolab.dday.dto.DdayGoalRequest;
import com.todolab.dday.exception.DdayGoalNotFoundException;
import com.todolab.dday.repository.DdayGoalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class DdayGoalServiceTest {

    @Mock
    DdayGoalRepository ddayGoalRepository;

    @Test
    @DisplayName("D-Day 목표를 생성한다")
    void create_success() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository);
        DdayGoal goal = new DdayGoal("정보처리기사", LocalDate.of(2026, 6, 10));
        given(ddayGoalRepository.save(org.mockito.ArgumentMatchers.any(DdayGoal.class))).willReturn(goal);

        var response = service.create(new DdayGoalRequest("정보처리기사", LocalDate.of(2026, 6, 10)));

        assertThat(response.title()).isEqualTo("정보처리기사");
        assertThat(response.targetDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        then(ddayGoalRepository).should().save(org.mockito.ArgumentMatchers.any(DdayGoal.class));
        then(ddayGoalRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("D-Day 목표를 날짜순으로 조회한다")
    void findAll_success() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository);
        given(ddayGoalRepository.findAllByOrderByTargetDateAscIdAsc()).willReturn(List.of(
                new DdayGoal("포트폴리오 제출", LocalDate.of(2026, 6, 5))
        ));

        var responses = service.findAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("포트폴리오 제출");
        then(ddayGoalRepository).should().findAllByOrderByTargetDateAscIdAsc();
        then(ddayGoalRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 D-Day 목표 삭제 시 예외를 던진다")
    void delete_fail_notFound() {
        DdayGoalService service = new DdayGoalService(ddayGoalRepository);
        given(ddayGoalRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(DdayGoalNotFoundException.class);

        then(ddayGoalRepository).should().existsById(99L);
        then(ddayGoalRepository).shouldHaveNoMoreInteractions();
    }
}
