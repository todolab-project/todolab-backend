package com.todolab.task.domain.query;

import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

@Getter
public class DateRange {

    private final LocalDateTime start; // Inclusive
    private final LocalDateTime end;   // Exclusive

    private DateRange(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public static DateRange ofDay(String date) {
        LocalDate d = LocalDate.parse(date);
        LocalDateTime start = d.atStartOfDay();                         // 00:00
        LocalDateTime end = d.plusDays(1).atStartOfDay();    // 다음날 00:00
        return new DateRange(start, end);
    }

    public static DateRange ofWeek(String date) {
        LocalDate d = LocalDate.parse(date);
        LocalDate startDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));      // 일요일
        LocalDateTime start = startDate.atStartOfDay();      // 00:00
        LocalDateTime end = start.plusDays(7);               // 다음주 일요일 00:00
        return new DateRange(start, end);
    }

    public static DateRange ofMonth(String date) {
        YearMonth ym = YearMonth.parse(date);
        LocalDateTime start = ym.atDay(1).atStartOfDay();                           // 1일 00:00
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();   // 다음달 1일 00:00 (Exclusive)
        return new DateRange(start, end);
    }
}
