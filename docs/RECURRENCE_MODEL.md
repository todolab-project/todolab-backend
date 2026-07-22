# Recurrence Model

Last updated: 2026-07-23

이 문서는 반복 Task/일정 저장을 위한 백엔드 내부 모델 기준이다. 현재 단계에서는 모델과 저장 구조만 확정하며, 모바일에서 호출할 반복 생성/수정 API는 아직 제공하지 않는다.

## 목표

- 반복 규칙은 `RECURRENCE_SERIES`에 한 번 저장한다.
- 실제로 표시, 완료, 이동, 수정되는 각 occurrence는 기존 `TASK` row로 표현한다.
- 반복 예외는 occurrence Task에 표시해 `SKIPPED`, `MOVED`, `MODIFIED`를 구분한다.
- owner scope는 series와 occurrence Task 양쪽에 유지한다.

## 테이블

### `RECURRENCE_SERIES`

| 컬럼 | 의미 |
| --- | --- |
| `ID` | 반복 series ID |
| `OWNER_USER_ID` | 소유 사용자 |
| `FREQUENCY` | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `INTERVAL_VALUE` | 반복 간격. 1 이상 |
| `RRULE` | 원본 RRULE 문자열 |
| `TIME_ZONE` | occurrence 계산 기준 time zone. 현재 기본 기준은 `Asia/Seoul` |
| `RECURRENCE_START_AT` | 반복 시작 일시 |
| `RECURRENCE_UNTIL` | 반복 종료일. 없을 수 있음 |
| `RECURRENCE_COUNT` | 반복 횟수. 없을 수 있음 |
| `CREATED_AT`, `UPDATED_AT` | 생성/수정 시각 |

### `TASK` 반복 필드

| 컬럼 | 의미 |
| --- | --- |
| `RECURRENCE_SERIES_ID` | 연결된 반복 series |
| `OCCURRENCE_DATE` | 이 Task가 나타내는 occurrence 날짜 |
| `ORIGINAL_OCCURRENCE_DATE` | 이동/수정 전 원래 occurrence 날짜 |
| `RECURRENCE_EXCEPTION` | `SKIPPED`, `MOVED`, `MODIFIED` |

## 검증 기준

- `frequency`, `rrule`, `timeZone`, `recurrenceStartAt`은 필수다.
- `interval`은 1 이상이다.
- `recurrenceCount`가 있으면 1 이상이다.
- `recurrenceUntil`은 `recurrenceStartAt`의 날짜보다 빠를 수 없다.
- RRULE의 허용 토큰과 월말/윤년/time zone 경계 처리는 다음 단계에서 별도로 확정한다.

## API 상태

- 반복 생성/수정/삭제 API는 아직 없다.
- 기존 Task 생성/수정 API는 반복 필드를 받지 않는다.
- Today/Calendar 조회에서 occurrence materialize는 아직 수행하지 않는다.
- 모바일은 반복 UI를 실제 저장 기능처럼 열면 안 된다.
