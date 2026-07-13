# Mobile API Backend Status

Last audited: 2026-07-13

이 문서는 `todolab-mobile/docs/API_*.md`와 모바일 로드맵의 백엔드 확인 항목을 `todolab-backend` 현재 코드 기준으로 대조한 관리 문서다.

상태 기준:

- `[x]` 완료 또는 현재 코드로 확인됨
- `[~]` 부분 구현됨. 계약과 차이가 있어 보완 필요
- `[ ]` 미구현 또는 아직 확인 불가

## 1. 실제 사용 전 최우선

1. [~] 인증 사용자 소유권
   - 완료: `Task`, `DdayGoal` owner 필드와 owner-aware repository/service path 추가
   - 완료: `/api/v1/tasks`, `/api/v1/dday-goals`의 주요 조회/수정/삭제 endpoint를 owner-aware service path로 확장
   - 남음: 기존 `/api/tasks`, `/api/ddays` 호환 유지/제거 정책 결정

2. [ ] Today / Calendar 여러 날 일정 범위 조회
   - 현재 `GET /api/tasks/today?date=...`는 `targetDate` 기준 `TODAY` Task만 조회한다.
   - 여러 날 `SCHEDULE`이 요청 날짜와 겹쳐도 `targetDate`가 시작일이면 다음 날 Today에는 포함되지 않는다.
   - `DAY/WEEK/MONTH` 범위 조회는 `startAt/endAt` overlap 조건을 사용하므로 Calendar 쪽 기반은 있음.
   - 남음: Today도 `API_SCHEDULE_RANGE.md`의 overlap 기준을 적용하고, 여러 날 일정이 실행 순서와 `todayOrder`에 섞이지 않게 분리.

3. [ ] 통합 검색 API
   - `GET /api/tasks/search` 없음.
   - 검색어, 필터, relevantDate/dateSource, cursor pagination 모두 미구현.

4. [~] 기존 D-Day 500 이슈 확인
   - 현재 `GET /api/ddays/{id}` endpoint 자체가 없음.
   - 현재 `POST /api/ddays/{id}/tasks` endpoint 자체가 없음.
   - 현재 Task와 D-Day 연결은 `PATCH /api/tasks/{id}/dday-goal?ddayGoalId=...`로 제공됨.
   - 남음: 모바일 문서/클라이언트가 기대하는 D-Day endpoint 이름을 백엔드 실제 계약과 맞추거나 alias 추가.

## 2. 여러 날 일정 / Calendar 범위 조회

문서: `todolab-mobile/docs/API_SCHEDULE_RANGE.md`

| 항목 | 상태 | 백엔드 현재 상태 |
| --- | --- | --- |
| `GET /api/tasks/today?date=YYYY-MM-DD`가 오늘과 겹치는 `SCHEDULE`도 반환 | [ ] | `TaskService.getTodayTasks`는 `findPlannedTasks(targetDate, targetDate+1)` 사용. overlap 미적용 |
| Calendar `DAY/WEEK/MONTH`가 범위와 겹치는 일정 반환 | [x] | `TaskRepositoryImpl.findByDateRangeAndType`가 `startAt < end && endAt > start` overlap 사용 |
| 원본 ID로 한 번만 반환 | [x] | 날짜별 row materialize 없이 `Task` row를 그대로 반환 |
| 여러 날 일정이 날짜마다 중복 row로 내려오지 않음 | [x] | Calendar 범위 조회는 원본 row 조회 방식 |
| 자정 종료, 종일 일정 종료일 exclusive 처리 | [x] | `endAt.isAfter(startAt)` 검증, range query는 `[start,end)` overlap. `endAt == rangeStart`는 미포함 |
| `endAt < startAt` 요청 validation | [x] | `TaskRequest.validate`, `Task.validatePeriodSchedule` 모두 거부 |
| 서비스 기준 시간대 `Asia/Seoul` | [x] | `Constant.ZONE_ID = "Asia/Seoul"` |

필요 작업:

- [ ] Today 조회에 schedule overlap 포함
- [ ] Today 응답에서 실행 TODO 정렬과 일정 정렬 기준 분리
- [ ] 여러 날 일정이 `todayOrder` 재정렬 대상에서 제외되는 테스트 추가
- [ ] Today/Calendar가 같은 overlap 기준을 쓰는 통합 테스트 추가

## 3. 통합 검색 API

문서: `todolab-mobile/docs/API_SEARCH_FILTER.md`

현재 상태: [ ] 미구현

필요 작업:

- [ ] `GET /api/tasks/search`
- [ ] `q` 제목/설명 검색
- [ ] `statuses`, `taskTypes`, `category`, `ddayGoalId`, `hasDday`, `allDay`
- [ ] `dateField`, `dateFrom`, `dateTo`
- [ ] `sort`, `cursor`, `limit`
- [ ] cursor pagination 중복/누락 방지
- [ ] 한글 검색, 영문 대소문자 검색 일관성
- [ ] `relevantDate`, `dateSource` 반환
- [ ] 잘못된 enum, 날짜 범위, cursor는 HTTP 400
- [ ] 모바일에 노출 가능한 안전한 오류 message
- [ ] 인증 사용자 owner 조건 적용

## 4. 반복 Task / 반복 일정

문서: `todolab-mobile/docs/API_RECURRENCE.md`

현재 상태: [ ] 미구현

백엔드에는 아래 모델과 endpoint가 없다.

- [ ] `recurrenceSeriesId`
- [ ] `recurrenceRule` RRULE
- [ ] `recurrenceTimeZone`
- [ ] `recurrenceStartAt`
- [ ] `recurrenceUntil` / `recurrenceCount`
- [ ] `occurrenceDate`
- [ ] `originalOccurrenceDate`
- [ ] `recurrenceException`
- [ ] Today / Calendar 조회 시 occurrence materialize
- [ ] occurrence별 완료 상태 저장
- [ ] `THIS` / `THIS_AND_FUTURE` / `ALL` 수정·삭제 scope
- [ ] 반복 전체 수정 후 기존 완료 기록 보존
- [ ] 월말, 윤년, 공휴일 등 RRULE validation 범위

제품 주의:

- 반복 UI는 이 계약 확정 전까지 실제 저장 기능처럼 열면 안 된다.

## 5. 반복 일정과 알림 책임

문서: `todolab-mobile/docs/API_NOTIFICATIONS.md`

현재 상태: [ ] 미구현

확정해야 할 백엔드 책임:

- [ ] 반복 occurrence 계산은 백엔드 책임
- [ ] 모바일은 가까운 미래 occurrence만 로컬 알림 예약
- [ ] 완료·미룸·삭제 후 같은 occurrence가 다음 동기화에서 제외/변경되는지
- [ ] `SKIPPED`, `MOVED`, `MODIFIED` 예외 처리
- [ ] time zone 변경 시 과거/미래 occurrence 재계산 방식
- [ ] 향후 서버 push 알림과 로컬 알림 중복 방지 방식

## 6. Today 재정렬 API

문서: `todolab-mobile/docs/API_TODAY_REORDER.md`

현재 상태: [~] 부분 구현

현재 제공:

- [x] `PATCH /api/tasks/{taskId}/today-order?date=YYYY-MM-DD&direction=UP|DOWN`
- [x] 단일 Task를 위/아래 한 칸 이동

권장 API 미구현:

- [ ] `PUT /api/tasks/today-order`
- [ ] request `{ date, orderedTaskIds }`
- [ ] 전체 순서를 transaction으로 저장
- [ ] 중복/누락/다른 날짜/완료/일정 Task ID 거부
- [ ] 동시 변경 시 HTTP 409
- [ ] 저장 직후 Today 조회 순서와 응답 순서 일치
- [ ] OpenAPI request/response/error schema 등록
- [ ] 인증 사용자 owner 조건 적용

우선순위: 실제 사용 시작에는 필수는 아니고 drag 안정성 개선 항목이다.

## 7. 날짜·시간 기준

문서: `todolab-mobile/docs/API_DATE_TIME.md`

| 항목 | 상태 | 백엔드 현재 상태 |
| --- | --- | --- |
| 서비스 기준 시간대 `Asia/Seoul` | [x] | `Constant.ZONE_ID`에 정의 |
| `LocalDate`는 `YYYY-MM-DD` | [x] | Spring `LocalDate` binding/JSON 기본 형식 |
| `LocalDateTime`은 offset 없는 `YYYY-MM-DDTHH:mm:ss` | [x] | Java `LocalDateTime` 사용 |
| 모바일이 서울 기준으로 해석 가능 | [~] | 값 형식은 맞지만 전체 코드가 `Constant.ZONE_ID`를 일관되게 쓰는 구조는 아님 |
| 향후 사용자 time zone 계약 | [ ] | 미정 |

필요 작업:

- [ ] `LocalDate.now()` 직접 사용 지점은 `Constant.ZONE_ID` 기준 clock으로 정리
- [ ] 사용자 time zone 도입 전까지 API 문서에 “서버/서비스 기준은 Asia/Seoul” 명시 유지

## 8. 기존 백엔드 이슈와 운영 확인

문서: `todolab-mobile/docs/ROADMAP.md`

| 항목 | 상태 | 메모 |
| --- | --- | --- |
| 개발 / 스테이징 / 운영 API URL | [~] | local CORS는 있음. staging/production URL 정책은 미정 |
| 인증 방식과 토큰 계약 | [~] | `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/me` 있음. 모바일 저장/refresh token 정책은 미정 |
| OpenAPI 명세 | [ ] | springdoc/swagger 설정 없음 |
| `GET /api/tasks` 범위 조회 계약 | [~] | `DAY/WEEK/MONTH`, `taskType` 지원. v1/owner 기준 계약 문서화 필요 |
| `GET /api/ddays/{id}` HTTP 500 | [~] | endpoint 없음. 구현하거나 모바일 계약에서 제거 필요 |
| `POST /api/ddays/{id}/tasks` HTTP 500 | [~] | endpoint 없음. 현재는 Task 생성 후 D-Day 연결 API를 사용 |
| D-Day 연결 Task Today 이동 후 HTTP 500 | [~] | 관련 회귀 테스트 필요. 현재 `PATCH /api/tasks/{id}/today`와 D-Day fetch join 응답 구조는 있음 |

## 9. 추천 구현 순서

1. [x] `/api/v1/tasks` 조회/수정/삭제를 owner-aware service path로 확장
2. [x] `/api/v1/dday-goals` 조회/삭제/연결 Task 조회를 owner-aware service path로 확장
3. [ ] Today 조회에 여러 날 schedule overlap 포함
4. [ ] D-Day legacy 500 이슈 재현 테스트 또는 endpoint 계약 정리
5. [ ] `GET /api/tasks/search` 구현
6. [ ] Today 일괄 재정렬 API 구현
7. [ ] 반복/알림 계약 설계 확정 후 recurrence 모델링
