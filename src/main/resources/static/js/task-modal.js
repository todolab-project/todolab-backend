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
  const $startLabel = document.getElementById('tmStartLabel');
  const $endLabel = document.getElementById('tmEndLabel');
  const $scheduleFields = document.getElementById('tmScheduleFields');
  const $executionDate = document.getElementById('tmExecutionDate');
  const $executionDateValue = document.getElementById('tmExecutionDateValue');

  const $meta = document.getElementById('tmMeta');
  const $createdAt = document.getElementById('tmCreatedAt');
  const $updatedAt = document.getElementById('tmUpdatedAt');

  const $primary = document.getElementById('tmPrimaryBtn');
  const $delete = document.getElementById('tmDeleteBtn');

  let mode = 'create'; // create | detail | edit
  let currentId = null;
  let currentType = null;
  let currentTask = null;

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

  function syncExecutionDate(task = null) {
    const targetDate = task?.status === 'TODAY' ? String(task.targetDate || '').trim() : '';
    $executionDate?.classList.toggle('hidden', !targetDate);
    if ($executionDateValue) {
      $executionDateValue.textContent = targetDate || '-';
    }
  }

  function setScheduleInputMode() {
    const isAllDay = !!$allDay.checked;
    const startValue = $startAt.value;
    const endValue = $endAt.value;
    const startDate = datePart(startValue);
    const endDate = datePart(endValue);

    $startAt.type = isAllDay ? 'date' : 'datetime-local';
    $endAt.type = isAllDay ? 'date' : 'datetime-local';

    $startLabel.textContent = isAllDay ? '시작일' : '시작';
    $endLabel.textContent = isAllDay ? '종료일' : '종료';
    $scheduleFields?.classList.toggle('is-all-day', isAllDay);
    $allDayLabel?.classList.toggle('is-active', isAllDay);

    if (isAllDay) {
      $startAt.value = startDate;
      $endAt.value = endDate;
      return;
    }

    $startAt.value = startValue && !startValue.includes('T') ? (dateTimeAtStartOfDay(startDate) || '') : startValue;
    $endAt.value = endValue && !endValue.includes('T') ? (dateTimeAtStartOfDay(endDate) || '') : endValue;
  }

  function normalizeAllDayRange() {
    if (!$allDay.checked) return null;

    setScheduleInputMode();

    const startDate = datePart($startAt.value) || baseScheduleDate();
    const endDate = datePart($endAt.value) || nextDate(startDate);
    const normalizedEndDate = endDate <= startDate ? nextDate(startDate) : endDate;

    $startAt.value = startDate;
    $endAt.value = normalizedEndDate;

    return {
      startAt: dateTimeAtStartOfDay(startDate),
      endAt: dateTimeAtStartOfDay(normalizedEndDate),
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
    if (!hasSchedule && !$allDay.checked) {
      $allDay.checked = false;
    }
    $scheduleFields?.classList.toggle('task-modal-date-grid-empty', !hasSchedule);

    $dateHint.textContent = hasSchedule
      ? ($allDay.checked ? '캘린더에 표시할 종일 일정' : '캘린더에 표시할 날짜와 시간')
      : '일정 없음 · 기록함에서 관리';
    $primary.textContent = basePrimaryText();
  }

  $startAt.addEventListener('input', syncDateDisabled);
  $endAt.addEventListener('input', syncDateDisabled);
  $startAt.addEventListener('change', () => syncDateDisabled({ normalize: true }));
  $endAt.addEventListener('change', () => syncDateDisabled({ normalize: true }));
  $allDay.addEventListener('change', () => syncDateDisabled({ normalize: true }));

  function reset() {
    mode = 'create';
    currentId = null;
    currentType = null;
    currentTask = null;

    $title.value = '';
    $desc.value = '';
    $category.value = '';
    $unscheduled.checked = true;
    $allDay.checked = false;
    $startAt.value = '';
    $endAt.value = '';
    syncExecutionDate();

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
    $allDay.checked = !!task.allDay;

    $startAt.value = toInputLocal(task.startAt);
    $endAt.value = toInputLocal(task.endAt);
    syncExecutionDate(task);

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
    if (preset.startAt != null) $startAt.value = preset.startAt;
    if (preset.endAt != null) $endAt.value = preset.endAt;

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
