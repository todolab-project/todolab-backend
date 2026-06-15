// src/main/resources/static/js/task-modal.js
window.TaskModal = (() => {
  const $modal = document.getElementById('taskModal');
  if (!$modal) return {};

  const $backdrop = document.getElementById('taskModalBackdrop');
  const $panel = document.getElementById('taskModalPanel');

  const $titleBar = document.getElementById('taskModalTitle');

  const $title = document.getElementById('tmTitle');
  const $desc = document.getElementById('tmDescription');
  const $category = document.getElementById('tmCategory');
  const $unscheduled = document.getElementById('tmUnscheduled');
  const $allDay = document.getElementById('tmAllDay');
  const $allDayLabel = document.getElementById('tmAllDayLabel');
  const $dateHint = document.getElementById('tmDateHint');
  const $startAt = document.getElementById('tmStartAt');
  const $endAt = document.getElementById('tmEndAt');
  const $endField = document.getElementById('tmEndField');
  const $multiDay = document.getElementById('tmMultiDayBtn');
  const $startLabel = document.getElementById('tmStartLabel');
  const $endLabel = document.getElementById('tmEndLabel');
  const $scheduleFields = document.getElementById('tmScheduleFields');
  const $whenSummary = document.getElementById('tmWhenSummary');
  const $whenValue = document.getElementById('tmWhenValue');
  const $whenStatus = document.getElementById('tmWhenStatus');
  const $whenMeta = document.getElementById('tmWhenMeta');

  const $meta = document.getElementById('tmMeta');
  const $createdAt = document.getElementById('tmCreatedAt');
  const $updatedAt = document.getElementById('tmUpdatedAt');

  const $primary = document.getElementById('tmPrimaryBtn');
  const $delete = document.getElementById('tmDeleteBtn');

  let mode = 'create'; // create | detail | edit
  let currentId = null;
  let currentType = null;
  let currentTask = null;
  let multiDay = false;

  function basePrimaryText() {
    if (mode === 'detail') return '수정';
    if (mode === 'edit') return '저장';
    return '항목 등록';
  }

  /* -----------------------------
   * open / close
   * ----------------------------- */
  function open() {
    $modal.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
  }

  function close() {
    $modal.classList.add('hidden');
    document.body.style.overflow = '';
    reset();
  }

  // 닫기: X/닫기 버튼(data-action="close"), dim 클릭, ESC
  $modal.addEventListener('click', (e) => {
    if (e.target.closest('[data-action="close"]')) close();
  });

  $backdrop?.addEventListener('click', (e) => {
    if ($panel && !$panel.contains(e.target)) close();
  });

  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !$modal.classList.contains('hidden')) close();
  });

  /* -----------------------------
   * helpers
   * ----------------------------- */
  function fmtIso(iso) {
    if (!iso) return '-';
    const [d, t] = String(iso).split('T');
    const hm = (t || '').substring(0, 5);
    return hm ? `${d} ${hm}` : d;
  }

  function toInputLocal(iso) {
    if (!iso) return '';
    const [d, t] = String(iso).split('T');
    const hm = (t || '').substring(0, 5);
    return hm ? `${d}T${hm}` : '';
  }

  function nextDate(date) {
    const dt = new Date(`${date}T00:00:00`);
    dt.setDate(dt.getDate() + 1);
    const year = dt.getFullYear();
    const month = String(dt.getMonth() + 1).padStart(2, '0');
    const day = String(dt.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function previousDate(date) {
    const dt = new Date(`${date}T00:00:00`);
    dt.setDate(dt.getDate() - 1);
    const year = dt.getFullYear();
    const month = String(dt.getMonth() + 1).padStart(2, '0');
    const day = String(dt.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function todayDate() {
    const dt = new Date();
    const year = dt.getFullYear();
    const month = String(dt.getMonth() + 1).padStart(2, '0');
    const day = String(dt.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function baseScheduleDate() {
    const page = document.querySelector('.app-page[data-selected-date], .app-page[data-current-date]');
    return $startAt.value?.slice(0, 10)
      || $endAt.value?.slice(0, 10)
      || page?.dataset?.selectedDate
      || page?.dataset?.currentDate
      || todayDate();
  }

  function datePart(value) {
    const text = String(value || '');
    return /^\d{4}-\d{2}-\d{2}/.test(text) ? text.slice(0, 10) : '';
  }

  function dateTimeAtStartOfDay(date) {
    return date ? `${date}T00:00` : null;
  }

  function setScheduleInputTypes(isAllDay) {
    const type = isAllDay ? 'date' : 'datetime-local';
    $startAt.type = type;
    $endAt.type = type;
  }

  function executionDatePresentation(targetDate) {
    const [year, month, day] = targetDate.split('-').map(Number);
    const target = new Date(year, month - 1, day);
    const todayText = todayDate();
    const [todayYear, todayMonth, todayDay] = todayText.split('-').map(Number);
    const today = new Date(todayYear, todayMonth - 1, todayDay);
    const diffDays = Math.round((target - today) / 86400000);
    const weekdays = ['일', '월', '화', '수', '목', '금', '토'];

    let status = '';
    if (diffDays === 0) status = '오늘';
    else if (diffDays === -1) status = '어제';
    else if (diffDays === 1) status = '내일';
    else if (diffDays < 0) status = `${Math.abs(diffDays)}일 지남`;
    else status = `${diffDays}일 후`;

    return {
      value: window.TaskUI?.formatDateKorean?.(targetDate)
        || `${month}월 ${day}일 ${weekdays[target.getDay()]}요일`,
      status,
      isPast: diffDays < 0
    };
  }

  function formatScheduleTime(task) {
    if (!task?.startAt) return '';
    if (task.allDay) return '종일';

    const startTime = String(task.startAt).split('T')[1]?.slice(0, 5) || '';
    const endTime = String(task.endAt || '').split('T')[1]?.slice(0, 5) || '';
    return endTime ? `${startTime}–${endTime}` : startTime;
  }

  function formatScheduleDate(task, fallbackDate) {
    const startDate = datePart(task?.startAt) || fallbackDate;
    if (!task?.allDay || !startDate) {
      return executionDatePresentation(fallbackDate).value;
    }

    const exclusiveEndDate = datePart(task.endAt);
    const lastDate = exclusiveEndDate ? previousDate(exclusiveEndDate) : startDate;
    const startLabel = window.TaskUI?.formatDateKorean?.(startDate)
      || executionDatePresentation(startDate).value;
    if (!lastDate || lastDate <= startDate) return startLabel;

    const lastLabel = window.TaskUI?.formatDateKorean?.(lastDate)
      || executionDatePresentation(lastDate).value;
    return `${startLabel} – ${lastLabel}`;
  }

  function syncWhenSummary(task = null) {
    const targetDate = task?.status === 'TODAY' ? String(task.targetDate || '').trim() : '';
    const scheduleDate = datePart(task?.startAt);
    const displayDate = targetDate || scheduleDate;
    const isDetail = mode === 'detail';

    $whenSummary?.classList.toggle('hidden', !isDetail);
    $scheduleFields?.classList.toggle('hidden', isDetail);
    $allDayLabel?.classList.toggle('hidden', isDetail);
    if (!isDetail) return;

    if (!displayDate) {
      if ($whenValue) $whenValue.textContent = '날짜 없음';
      if ($whenStatus) $whenStatus.classList.add('hidden');
      if ($whenMeta) $whenMeta.textContent = '날짜 정하기';
      return;
    }

    const presentation = executionDatePresentation(displayDate);
    if ($whenValue) $whenValue.textContent = formatScheduleDate(task, displayDate);
    if ($whenStatus) {
      $whenStatus.textContent = presentation.status;
      $whenStatus.classList.remove('hidden');
      $whenStatus.classList.toggle('is-past', presentation.isPast);
    }

    const timeText = formatScheduleTime(task);
    if ($whenMeta) {
      $whenMeta.textContent = timeText || '시간 없음';
    }
  }

  function setScheduleInputMode() {
    const isAllDay = !!$allDay.checked;
    const startValue = $startAt.value;
    const endValue = $endAt.value;
    const startDate = datePart(startValue);
    const endDate = datePart(endValue);

    setScheduleInputTypes(isAllDay);

    $startLabel.textContent = isAllDay ? '날짜' : '시작';
    $endLabel.textContent = isAllDay ? (multiDay ? '마지막 날' : '기간') : '종료';
    $scheduleFields?.classList.toggle('is-all-day', isAllDay);
    $scheduleFields?.classList.toggle('is-single-day', isAllDay && !multiDay);
    $allDayLabel?.classList.toggle('is-active', isAllDay);
    $multiDay?.classList.toggle('hidden', !isAllDay);
    if ($multiDay) {
      $multiDay.textContent = multiDay ? '하루 일정으로 변경' : '여러 날로 변경';
      $multiDay.setAttribute(
        'aria-label',
        multiDay ? '여러 날 일정을 하루 일정으로 변경' : '하루 일정을 여러 날 일정으로 변경'
      );
    }

    if (isAllDay) {
      $startAt.value = startDate;
      $endAt.value = multiDay ? (endDate || startDate) : startDate;
      return;
    }

    $startAt.value = startValue && !startValue.includes('T') ? (dateTimeAtStartOfDay(startDate) || '') : startValue;
    $endAt.value = endValue && !endValue.includes('T') ? (dateTimeAtStartOfDay(endDate) || '') : endValue;
  }

  function normalizeAllDayRange() {
    if (!$allDay.checked) return null;

    setScheduleInputMode();

    const startDate = datePart($startAt.value) || baseScheduleDate();
    const requestedLastDate = multiDay ? (datePart($endAt.value) || startDate) : startDate;
    const lastDate = requestedLastDate < startDate ? startDate : requestedLastDate;
    const exclusiveEndDate = nextDate(lastDate);

    $startAt.value = startDate;
    $endAt.value = lastDate;

    return {
      startAt: dateTimeAtStartOfDay(startDate),
      endAt: dateTimeAtStartOfDay(exclusiveEndDate),
    };
  }

  function setReadOnly(ro) {
    $title.readOnly = ro;
    $desc.readOnly = ro;
    $category.readOnly = ro;

    $allDay.disabled = ro;
    $startAt.disabled = ro;
    $endAt.disabled = ro;
  }

  function syncDateDisabled({ normalize = false } = {}) {
    setScheduleInputMode();

    if (normalize) {
      normalizeAllDayRange();
    }

    const hasSchedule = !!($startAt.value || $endAt.value);
    $unscheduled.checked = !hasSchedule;
    $allDay.disabled = mode === 'detail';
    $dateHint?.classList.toggle('hidden', mode === 'detail');
    if (!hasSchedule && !$allDay.checked) {
      $allDay.checked = false;
    }
    $scheduleFields?.classList.toggle('task-modal-date-grid-empty', !hasSchedule);

    $dateHint.textContent = hasSchedule
      ? (currentTask?.scheduleSource === 'AUTO_TODAY'
        ? '오늘 할 일로 이동하며 자동 설정된 날짜'
        : ($allDay.checked ? '선택한 날짜에 종일로 표시해요' : '시간은 선택 사항이에요'))
      : '날짜를 정하지 않으면 기록함에서 관리해요';
    syncWhenSummary(currentTask);
    $primary.textContent = basePrimaryText();
  }

  $startAt.addEventListener('input', syncDateDisabled);
  $endAt.addEventListener('input', syncDateDisabled);
  $startAt.addEventListener('change', () => syncDateDisabled({ normalize: true }));
  $endAt.addEventListener('change', () => syncDateDisabled({ normalize: true }));
  $allDay.addEventListener('change', () => {
    if ($allDay.checked) {
      const startDate = datePart($startAt.value) || baseScheduleDate();
      const endDate = datePart($endAt.value);
      multiDay = Boolean(endDate && endDate > startDate);
      setScheduleInputTypes(true);
      $startAt.value = startDate;
      $endAt.value = multiDay ? endDate : startDate;
    } else {
      const startDate = datePart($startAt.value) || baseScheduleDate();
      setScheduleInputTypes(false);
      $startAt.value = dateTimeAtStartOfDay(startDate) || '';
      $endAt.value = '';
      multiDay = false;
    }
    syncDateDisabled();
  });
  $multiDay?.addEventListener('click', () => {
    multiDay = !multiDay;
    if (multiDay) {
      $endAt.value = datePart($endAt.value) || datePart($startAt.value) || baseScheduleDate();
    }
    syncDateDisabled();
    (multiDay ? $endAt : $startAt).focus();
  });
  $whenSummary?.addEventListener('click', () => {
    if (mode !== 'detail' || !currentId) return;
    setModeEdit(currentId);
    $startAt.focus();
  });

  function reset() {
    mode = 'create';
    currentId = null;
    currentType = null;
    currentTask = null;
    multiDay = false;

    $title.value = '';
    $desc.value = '';
    $category.value = '';
    $unscheduled.checked = true;
    $allDay.checked = false;
    $startAt.value = '';
    $endAt.value = '';
    syncWhenSummary();

    $meta.classList.add('hidden');
    $createdAt.textContent = '-';
    $updatedAt.textContent = '-';

    $delete.classList.add('hidden');
    $titleBar.textContent = '항목 등록';

    setReadOnly(false);
    syncDateDisabled();
  }

  function fill(task) {
    currentTask = task;
    currentType = task.type ?? null;

    $title.value = task.title ?? '';
    $desc.value = task.description ?? '';
    $category.value = task.category ?? '';

    $unscheduled.checked = !(task.startAt || task.endAt);
    const targetDate = datePart(task.targetDate);
    const inferredAllDay = Boolean(task.allDay || (!task.startAt && targetDate));
    const inferredStartAt = task.startAt || (inferredAllDay ? dateTimeAtStartOfDay(targetDate) : null);
    const inferredEndAt = task.endAt
      || (inferredAllDay && targetDate ? dateTimeAtStartOfDay(nextDate(targetDate)) : null);
    currentTask = {
      ...task,
      allDay: inferredAllDay,
      startAt: inferredStartAt,
      endAt: inferredEndAt
    };
    $allDay.checked = inferredAllDay;
    setScheduleInputTypes(inferredAllDay);

    $startAt.value = inferredAllDay ? datePart(inferredStartAt) : toInputLocal(inferredStartAt);
    $endAt.value = inferredAllDay ? datePart(inferredEndAt) : toInputLocal(inferredEndAt);
    if (inferredAllDay) {
      const startDate = datePart(inferredStartAt) || targetDate;
      const exclusiveEndDate = datePart(inferredEndAt);
      const lastDate = exclusiveEndDate ? previousDate(exclusiveEndDate) : startDate;
      multiDay = Boolean(startDate && lastDate && lastDate > startDate);
      $startAt.value = startDate;
      $endAt.value = lastDate || startDate;
    } else {
      multiDay = false;
    }
    syncWhenSummary(currentTask);

    $createdAt.textContent = fmtIso(task.createdAt);
    $updatedAt.textContent = fmtIso(task.updatedAt || task.timestamp);

    syncDateDisabled();
  }

  function payloadFromForm() {
    const allDayRange = normalizeAllDayRange();

    const startAt = allDayRange?.startAt ?? ($startAt.value || null);
    const endAt = allDayRange?.endAt ?? ($endAt.value || null);
    const hasSchedule = !!(startAt || endAt);
    const isUnscheduled = !hasSchedule;
    const type = hasSchedule ? 'SCHEDULE' : (currentType === 'IDEA' ? 'IDEA' : 'TODO');

    return {
      title: ($title.value || '').trim(),
      description: $desc.value || '',
      category: $category.value || '',
      type,
      unscheduled: isUnscheduled,
      allDay: hasSchedule && !!$allDay.checked,
      startAt: isUnscheduled ? null : startAt,
      endAt: isUnscheduled ? null : endAt,
    };
  }

  /* -----------------------------
   * mode set
   * ----------------------------- */
  function setModeCreate(preset = {}) {
    mode = 'create';
    currentId = null;

    $titleBar.textContent = '항목 등록';
    $delete.classList.add('hidden');
    $meta.classList.add('hidden');

    setReadOnly(false);

    // preset 값 반영 (예: 특정 날짜에서 바로 등록)
    if (preset.title != null) $title.value = preset.title;
    if (preset.description != null) $desc.value = preset.description;
    if (preset.category != null) $category.value = preset.category;
    if (preset.unscheduled != null) $unscheduled.checked = !!preset.unscheduled;
    if (preset.allDay != null) $allDay.checked = !!preset.allDay;
    setScheduleInputTypes($allDay.checked);
    if (preset.startAt != null) {
      $startAt.value = $allDay.checked ? datePart(preset.startAt) : toInputLocal(preset.startAt);
    }
    if (preset.endAt != null) {
      const presetEndDate = datePart(preset.endAt);
      $endAt.value = $allDay.checked && presetEndDate
        ? previousDate(presetEndDate)
        : toInputLocal(preset.endAt);
    }

    syncDateDisabled();
  }

  function setModeDetail(id, task) {
    mode = 'detail';
    currentId = id;

    $titleBar.textContent = '일정 상세';
    $primary.textContent = '수정';
    $delete.classList.remove('hidden');
    $meta.classList.remove('hidden');

    fill(task);
    setReadOnly(true);
  }

  function setModeEdit(id) {
    mode = 'edit';
    currentId = id;

    $titleBar.textContent = '일정 수정';
    $primary.textContent = '저장';
    $delete.classList.remove('hidden');
    $meta.classList.remove('hidden');

    setReadOnly(false);
    syncDateDisabled();
  }

  /* -----------------------------
   * primary / delete actions
   * ----------------------------- */
  $primary.addEventListener('click', async () => {
    try {
      if (mode === 'create') {
        const payload = payloadFromForm();
        if (!payload.title) return alert('제목을 입력해주세요.');
        await TaskApi.createTask(payload);
        close();
        location.reload();
        return;
      }

      if (mode === 'detail') {
        // 상세에서 "수정" 누르면 편집모드로 전환
        if (!currentId) return;
        setModeEdit(currentId);
        return;
      }

      if (mode === 'edit') {
        const payload = payloadFromForm();
        if (!payload.title) return alert('제목을 입력해주세요.');
        if (!currentId) return;

        await TaskApi.updateTask(currentId, payload);
        close();
        location.reload();
      }
    } catch (e) {
      alert(`${e.message || e}`);
    }
  });

  $delete.addEventListener('click', async () => {
    if (!currentId) return;
    const title = (currentTask?.title || $title.value || '이 항목').trim();
    if (!confirm(`'${title}'을(를) 삭제하시겠어요?`)) return;

    try {
      await TaskApi.deleteTask(currentId);
      close();
      location.reload();
    } catch (e) {
      alert(`${e.message || e}`);
    }
  });

  /* -----------------------------
   * public API
   * ----------------------------- */
  async function openCreate(preset = {}) {
    reset();
    setModeCreate(preset);
    open();
  }

  async function openDetail(id) {
    reset();
    open(); // 로딩 중에도 모달은 띄움

    try {
      const task = await TaskApi.getTask(id);
      setModeDetail(id, task);
    } catch (e) {
      alert(`상세 로딩 실패: ${e.message || e}`);
      close();
    }
  }

  return { openCreate, openDetail, close };
})();
