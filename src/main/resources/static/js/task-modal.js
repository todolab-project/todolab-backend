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
  const $startAt = document.getElementById('tmStartAt');
  const $endAt = document.getElementById('tmEndAt');

  const $meta = document.getElementById('tmMeta');
  const $createdAt = document.getElementById('tmCreatedAt');
  const $updatedAt = document.getElementById('tmUpdatedAt');
  const $ddaySection = document.getElementById('tmDdaySection');
  const $ddaySelect = document.getElementById('tmDdaySelect');
  const $ddayCurrent = document.getElementById('tmDdayCurrent');
  const $ddayDisconnect = document.getElementById('tmDdayDisconnectBtn');

  const $primary = document.getElementById('tmPrimaryBtn');
  const $delete = document.getElementById('tmDeleteBtn');

  let mode = 'create'; // create | detail | edit
  let currentId = null;
  let currentType = null;
  let currentTask = null;
  let ddayGoals = [];
  let ddayLoading = null;
  let reloadOnClose = false;

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
    const shouldReload = reloadOnClose;
    reset();
    if (shouldReload) location.reload();
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

  function formatDdayLabel(daysLeft) {
    if (window.TaskUI?.formatDdayLabel) return TaskUI.formatDdayLabel(daysLeft);
    if (daysLeft === null || daysLeft === undefined || daysLeft === '') return null;
    const n = Number(daysLeft);
    if (Number.isNaN(n)) return null;
    if (n === 0) return 'D-Day';
    if (n > 0) return `D-${n}`;
    return `D+${Math.abs(n)}`;
  }

  function setReadOnly(ro) {
    $title.readOnly = ro;
    $desc.readOnly = ro;
    $category.readOnly = ro;

    // checkbox + datetime-local은 disabled 처리
    $unscheduled.disabled = ro;
    $allDay.disabled = ro;
    $startAt.disabled = ro || $unscheduled.checked;
    $endAt.disabled = ro || $unscheduled.checked;
  }

  function syncDateDisabled() {
    const dis = $unscheduled.checked;
    $startAt.disabled = dis || mode === 'detail';
    $endAt.disabled = dis || mode === 'detail';
    if (dis) {
      $startAt.value = '';
      $endAt.value = '';
    }
  }

  $unscheduled.addEventListener('change', syncDateDisabled);

  function reset() {
    mode = 'create';
    currentId = null;
    currentType = null;
    currentTask = null;
    reloadOnClose = false;

    $title.value = '';
    $desc.value = '';
    $category.value = '';
    $unscheduled.checked = false;
    $allDay.checked = false;
    $startAt.value = '';
    $endAt.value = '';

    $meta.classList.add('hidden');
    $createdAt.textContent = '-';
    $updatedAt.textContent = '-';
    $ddaySection?.classList.add('hidden');
    if ($ddaySelect) $ddaySelect.innerHTML = '<option value="">연결된 목표 없음</option>';
    if ($ddayCurrent) $ddayCurrent.textContent = '-';
    $ddayDisconnect?.classList.add('hidden');

    $delete.classList.add('hidden');
    $primary.textContent = '항목 등록';
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

    $unscheduled.checked = !!task.unscheduled;
    $allDay.checked = !!task.allDay;

    $startAt.value = toInputLocal(task.startAt);
    $endAt.value = toInputLocal(task.endAt);

    $createdAt.textContent = fmtIso(task.createdAt);
    $updatedAt.textContent = fmtIso(task.updatedAt || task.timestamp);

    syncDateDisabled();
  }

  async function loadDdayGoals() {
    if (!ddayLoading) {
      ddayLoading = TaskApi.getDdayGoals()
        .then((goals) => {
          ddayGoals = Array.isArray(goals) ? goals : [];
          return ddayGoals;
        })
        .finally(() => {
          ddayLoading = null;
        });
    }
    return ddayLoading;
  }

  function renderDdayOptions(task) {
    if (!$ddaySelect) return;

    const selectedId = task?.ddayGoalId == null ? '' : String(task.ddayGoalId);
    const options = ['<option value="">연결된 목표 없음</option>']
      .concat(ddayGoals.map((goal) => {
        const label = formatDdayLabel(goal.daysLeft);
        const text = label ? `${goal.title} ${label}` : goal.title;
        const selected = String(goal.id) === selectedId ? ' selected' : '';
        return `<option value="${String(goal.id).replaceAll('"', '&quot;')}"${selected}>${String(text)
          .replaceAll('&', '&amp;')
          .replaceAll('<', '&lt;')
          .replaceAll('>', '&gt;')
          .replaceAll('"', '&quot;')}</option>`;
      }));

    $ddaySelect.innerHTML = options.join('');
  }

  function renderDdayCurrent(task) {
    if (!$ddayCurrent || !$ddayDisconnect) return;

    if (!task?.ddayGoalId) {
      $ddayCurrent.textContent = '연결된 목표가 없습니다.';
      $ddayDisconnect.classList.add('hidden');
      return;
    }

    const label = formatDdayLabel(task.ddayDaysLeft);
    const title = task.ddayGoalTitle || 'D-Day 목표';
    $ddayCurrent.textContent = label ? `현재 연결 · ${title} ${label}` : `현재 연결 · ${title}`;
    $ddayDisconnect.classList.remove('hidden');
  }

  async function showDdayControl(task) {
    if (!$ddaySection || !$ddaySelect) return;

    $ddaySection.classList.remove('hidden');
    $ddaySelect.disabled = true;
    if ($ddayCurrent) $ddayCurrent.textContent = 'D-Day 목표를 불러오는 중...';

    try {
      await loadDdayGoals();
      renderDdayOptions(task);
      renderDdayCurrent(task);
    } catch (e) {
      if ($ddayCurrent) $ddayCurrent.textContent = `D-Day 목표 로딩 실패: ${e.message || e}`;
    } finally {
      $ddaySelect.disabled = false;
    }
  }

  function payloadFromForm() {
    const startAt = $startAt.value || null;
    const endAt = $endAt.value || null;
    const isUnscheduled = !!$unscheduled.checked;
    const hasSchedule = !isUnscheduled && !!(startAt || endAt);
    const type = hasSchedule ? 'SCHEDULE' : (currentType === 'IDEA' ? 'IDEA' : 'TODO');

    return {
      title: ($title.value || '').trim(),
      description: $desc.value || '',
      category: $category.value || '',
      type,
      unscheduled: isUnscheduled,
      allDay: !!$allDay.checked,
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
    $primary.textContent = '항목 등록';
    $delete.classList.add('hidden');
    $meta.classList.add('hidden');
    $ddaySection?.classList.add('hidden');

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
    showDdayControl(task);
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
    showDdayControl(currentTask);
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
    if (!confirm('정말 삭제하시겠어요?')) return;

    try {
      await TaskApi.deleteTask(currentId);
      close();
      location.reload();
    } catch (e) {
      alert(`${e.message || e}`);
    }
  });

  $ddaySelect?.addEventListener('change', async () => {
    if (!currentId || !currentTask) return;

    const value = $ddaySelect.value;
    $ddaySelect.disabled = true;

    try {
      const updated = value
        ? await TaskApi.connectDdayGoal(currentId, value)
        : await TaskApi.disconnectDdayGoal(currentId);
      reloadOnClose = true;
      fill(updated);
      await showDdayControl(updated);
    } catch (e) {
      alert(`D-Day 연결 변경 실패: ${e.message || e}`);
      renderDdayOptions(currentTask);
      renderDdayCurrent(currentTask);
    } finally {
      $ddaySelect.disabled = false;
    }
  });

  $ddayDisconnect?.addEventListener('click', async () => {
    if (!currentId || !currentTask?.ddayGoalId) return;

    try {
      $ddayDisconnect.disabled = true;
      const updated = await TaskApi.disconnectDdayGoal(currentId);
      reloadOnClose = true;
      fill(updated);
      await showDdayControl(updated);
    } catch (e) {
      alert(`D-Day 연결 해제 실패: ${e.message || e}`);
    } finally {
      $ddayDisconnect.disabled = false;
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
