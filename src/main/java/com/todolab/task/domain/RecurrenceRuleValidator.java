package com.todolab.task.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RecurrenceRuleValidator {

    private static final Set<String> SUPPORTED_KEYS = Set.of("FREQ", "INTERVAL", "COUNT", "UNTIL", "BYDAY", "BYMONTHDAY");
    private static final Set<String> WEEK_DAYS = Set.of("MO", "TU", "WE", "TH", "FR", "SA", "SU");
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private RecurrenceRuleValidator() {
    }

    public static String validate(
            RecurrenceFrequency frequency,
            int interval,
            String recurrenceRule,
            String timeZone,
            LocalDate recurrenceUntil,
            Integer recurrenceCount
    ) {
        String normalizedRule = normalizeRequired(recurrenceRule, "recurrenceRule은 필수입니다.");
        validateTimeZone(timeZone);
        Map<String, String> parts = parse(normalizedRule);

        RecurrenceFrequency ruleFrequency = parseFrequency(parts);
        if (ruleFrequency != frequency) {
            throw new IllegalArgumentException("RRULE FREQ는 frequency와 일치해야 합니다.");
        }

        int ruleInterval = parsePositiveInteger(parts.getOrDefault("INTERVAL", "1"), "RRULE INTERVAL은 1 이상이어야 합니다.");
        if (ruleInterval != interval) {
            throw new IllegalArgumentException("RRULE INTERVAL은 interval과 일치해야 합니다.");
        }

        validateEndCondition(parts, recurrenceUntil, recurrenceCount);
        validateByDay(parts.get("BYDAY"));
        validateByMonthDay(parts.get("BYMONTHDAY"));
        return normalizedRule;
    }

    private static Map<String, String> parse(String recurrenceRule) {
        Map<String, String> parts = new HashMap<>();
        for (String token : recurrenceRule.split(";")) {
            int delimiterIndex = token.indexOf('=');
            if (delimiterIndex < 1 || delimiterIndex == token.length() - 1) {
                throw new IllegalArgumentException("RRULE 형식이 올바르지 않습니다.");
            }

            String key = token.substring(0, delimiterIndex).trim().toUpperCase();
            String value = token.substring(delimiterIndex + 1).trim().toUpperCase();
            if (!SUPPORTED_KEYS.contains(key)) {
                throw new IllegalArgumentException("지원하지 않는 RRULE 항목입니다: " + key);
            }
            if (parts.put(key, value) != null) {
                throw new IllegalArgumentException("RRULE 항목이 중복되었습니다: " + key);
            }
        }

        if (!parts.containsKey("FREQ")) {
            throw new IllegalArgumentException("RRULE FREQ는 필수입니다.");
        }
        return parts;
    }

    private static RecurrenceFrequency parseFrequency(Map<String, String> parts) {
        try {
            return RecurrenceFrequency.valueOf(parts.get("FREQ"));
        } catch (Exception e) {
            throw new IllegalArgumentException("지원하지 않는 RRULE FREQ 값입니다.");
        }
    }

    private static void validateEndCondition(Map<String, String> parts, LocalDate recurrenceUntil, Integer recurrenceCount) {
        boolean hasUntil = parts.containsKey("UNTIL");
        boolean hasCount = parts.containsKey("COUNT");
        if (hasUntil && hasCount) {
            throw new IllegalArgumentException("RRULE UNTIL과 COUNT는 함께 사용할 수 없습니다.");
        }
        if (recurrenceUntil != null && recurrenceCount != null) {
            throw new IllegalArgumentException("recurrenceUntil과 recurrenceCount는 함께 사용할 수 없습니다.");
        }
        if (hasUntil != (recurrenceUntil != null)) {
            throw new IllegalArgumentException("RRULE UNTIL은 recurrenceUntil과 함께 지정해야 합니다.");
        }
        if (hasCount != (recurrenceCount != null)) {
            throw new IllegalArgumentException("RRULE COUNT는 recurrenceCount와 함께 지정해야 합니다.");
        }
        if (hasUntil && !parseUntil(parts.get("UNTIL")).equals(recurrenceUntil)) {
            throw new IllegalArgumentException("RRULE UNTIL은 recurrenceUntil과 일치해야 합니다.");
        }
        if (hasCount && parsePositiveInteger(parts.get("COUNT"), "RRULE COUNT는 1 이상이어야 합니다.") != recurrenceCount) {
            throw new IllegalArgumentException("RRULE COUNT는 recurrenceCount와 일치해야 합니다.");
        }
    }

    private static void validateByDay(String byDay) {
        if (byDay == null) {
            return;
        }
        Set<String> values = splitValues(byDay);
        if (values.isEmpty() || !WEEK_DAYS.containsAll(values)) {
            throw new IllegalArgumentException("RRULE BYDAY 값이 올바르지 않습니다.");
        }
    }

    private static void validateByMonthDay(String byMonthDay) {
        if (byMonthDay == null) {
            return;
        }
        for (String value : splitValues(byMonthDay)) {
            int day = parseInteger(value, "RRULE BYMONTHDAY 값이 올바르지 않습니다.");
            if (day == 0 || day < -31 || day > 31) {
                throw new IllegalArgumentException("RRULE BYMONTHDAY 값은 -31 이상 31 이하이며 0일 수 없습니다.");
            }
        }
    }

    private static Set<String> splitValues(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static LocalDate parseUntil(String value) {
        try {
            return LocalDate.parse(value, BASIC_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("RRULE UNTIL은 YYYYMMDD 형식이어야 합니다.");
        }
    }

    private static int parsePositiveInteger(String value, String message) {
        int parsed = parseInteger(value, message);
        if (parsed < 1) {
            throw new IllegalArgumentException(message);
        }
        return parsed;
    }

    private static int parseInteger(String value, String message) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateTimeZone(String timeZone) {
        String normalized = normalizeRequiredPreservingCase(timeZone, "timeZone은 필수입니다.");
        try {
            ZoneId.of(normalized);
        } catch (Exception e) {
            throw new IllegalArgumentException("timeZone 값이 올바르지 않습니다.");
        }
    }

    private static String normalizeRequired(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized.toUpperCase();
    }

    private static String normalizeRequiredPreservingCase(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }
}
