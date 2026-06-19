// src/main/resources/static/js/month.js
(() => {
  const root = document.getElementById('month-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const grid = document.getElementById('monthGrid');
  const prevBtn = document.getElementById('monthPrevHint');
  const nextBtn = document.getElementById('monthNextHint');

  const $error = document.getElementById('month-error');
  const $errorMessage = document.getElementById('month-error-message');
  const $retry = document.getElementById('month-retry');
  const $empty = document.getElementById('month-empty');
  const $card  = document.getElementById('month-card');
  const $list  = document.getElementById('month-list');
  const $doneEmpty = document.getElementById('month-done-empty');
  const $doneCard = document.getElementById('month-done-card');
  const $doneList = document.getElementById('month-done-list');
  const $doneCount = document.getElementById('month-done-count');
  const $selectedDateTitle = document.getElementById('month-selected-date-title');
  const $selectedDateText = document.getElementById('month-selected-date-text');

  const selectedDate = (root.dataset.selectedDate || '').trim(); // yyyy-MM-dd
  const monthLabel   = (root.dataset.monthLabel || '').trim();   // yyyy-MM
  const monthStart   = (root.dataset.monthStart || '').trim();   // yyyy-MM-dd
  const currentDate  = (root.dataset.currentDate || '').trim();

  let navLocked = false;
  const NAV_LOCK_MS = 650;
  const FILTER_STORAGE_KEY = 'todolab.calendar.filters';
  const FILTER_KINDS = ['task', 'done', 'stale', 'dday'];

  function lockNav() {
    navLocked = true;
    setTimeout(() => (navLocked = false), NAV_LOCK_MS);
  }

  function hideFeedback() {
    $error?.classList.add('hidden');
    $empty?.classList.add('hidden');
  }

  function showError(msg) {
    $empty?.classList.add('hidden');
    if ($errorMessage) $errorMessage.textContent = msg;
    $error?.classList.remove('hidden');
  }

  function showEmpty() {
    hideFeedback();
    $card?.classList.add('hidden');
    $empty?.classList.remove('hidden');
  }

  function showList() {
    hideFeedback();
    $card?.classList.remove('hidden');
  }

  function renderDone(tasks) {
    const doneTasks = Array.isArray(tasks) ? tasks : [];
    if ($doneCount) $doneCount.textContent = `${doneTasks.length}개`;

    if (!window.TaskUI || typeof window.TaskUI.renderDoneCard !== 'function') {
      return;
    }

    if (!doneTasks.length) {
      $doneCard?.classList.add('hidden');
      $doneEmpty?.classList.remove('hidden');
      return;
    }

    $doneEmpty?.classList.add('hidden');
    $doneCard?.classList.remove('hidden');
    $doneList.innerHTML = doneTasks.map(TaskUI.renderDoneCard).join('');
  }

  function isTaskOnDate(t, date) {
    if (!date) return true;
    const dayStart = new Date(`${date}T00:00:00`);
    const dayEnd = new Date(dayStart);
    dayEnd.setDate(dayEnd.getDate() + 1);
    const start = t?.startAt ? new Date(t.startAt) : null;
    const end = t?.endAt ? new Date(t.endAt) : null;
    if (!start) return false;
    return start < dayEnd && (!end || end > dayStart);
  }

  function mergeTasks(...groups) {
    const seen = new Set();
    return groups
      .flatMap(group => Array.isArray(group) ? group : [])
      .filter(task => {
        const id = String(task?.id || '');
        if (!id || seen.has(id)) return false;
        seen.add(id);
        return true;
      });
  }

  function gotoMonth(move) {
    if (navLocked) return;
    lockNav();

    const base = monthStart || selectedDate || currentDate;
    const url = `/tasks/month?move=${move}&date=${encodeURIComponent(base)}`;
    window.location.href = url;
  }

  prevBtn?.addEventListener('click', (e) => { e.preventDefault(); gotoMonth('prev'); });
  nextBtn?.addEventListener('click', (e) => { e.preventDefault(); gotoMonth('next'); });

  function readCalendarFilters() {
    const filters = Object.fromEntries(FILTER_KINDS.map(kind => [kind, true]));
    try {
      const saved = JSON.parse(localStorage.getItem(FILTER_STORAGE_KEY) || '{}');
      FILTER_KINDS.forEach(kind => {
        if (typeof saved[kind] === 'boolean') filters[kind] = saved[kind];
      });
    } catch (_) {
      // Ignore broken localStorage values and fall back to all visible.
    }
    return filters;
  }

  function writeCalendarFilters(filters) {
    try {
      localStorage.setItem(FILTER_STORAGE_KEY, JSON.stringify(filters));
    } catch (_) {
      // The current page can still apply the filter even if persistence fails.
    }
  }

  function applyCalendarFilters(filters) {
    FILTER_KINDS.forEach(kind => {
      root.classList.toggle(`calendar-filter-hide-${kind}`, !filters[kind]);
    });

    root.querySelectorAll('[data-calendar-filter]').forEach(button => {
      const kind = button.dataset.calendarFilter;
      const isOn = filters[kind] !== false;
      button.classList.toggle('is-off', !isOn);
      button.setAttribute('aria-pressed', String(isOn));
    });
  }

  function initCalendarFilters() {
    let filters = readCalendarFilters();
    applyCalendarFilters(filters);

    root.querySelectorAll('[data-calendar-filter]').forEach(button => {
      button.addEventListener('click', () => {
        const kind = button.dataset.calendarFilter;
        if (!FILTER_KINDS.includes(kind)) return;

        filters = { ...filters, [kind]: filters[kind] === false };
        writeCalendarFilters(filters);
        applyCalendarFilters(filters);
      });
    });
  }

  function todayYmd() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function applyTodayRing() {
    const today = todayYmd();
    const nodes = document.querySelectorAll('#monthGrid a[data-date]');
    nodes.forEach(a => {
      a.classList.remove('ring-2', 'ring-gray-300/70', 'bg-white/40');

      const date = a.getAttribute('data-date');
      if (!date) return;

      if (date === selectedDate) return;

      if (date === today) {
        a.classList.add('ring-2', 'ring-gray-300/70', 'bg-white/40');
      }
    });
  }

  async function load() {
    $retry?.setAttribute('disabled', '');

    try {
      $error?.classList.add('hidden');
      applyTodayRing();
      const selectedDateLabel = window.TaskUI?.formatDateKorean?.(selectedDate) || selectedDate;
      if ($selectedDateTitle) $selectedDateTitle.textContent = `${selectedDateLabel}의 할 일`;
      if ($selectedDateText) $selectedDateText.textContent = selectedDateLabel;

      const ym = monthLabel || (monthStart ? monthStart.slice(0, 7) : '');
      const [tasks, plannedTasks, doneTasks] = await Promise.all([
        TaskApi.getMonthTasks(ym),
        selectedDate ? TaskApi.getTodayTasks(selectedDate) : Promise.resolve([]),
        selectedDate ? TaskApi.getDoneTasks(selectedDate) : Promise.resolve([])
      ]);
      renderDone(doneTasks);

      // 하단 리스트는 selectedDate 기준
      const filtered = mergeTasks(tasks, plannedTasks)
        .filter(t => isTaskOnDate(t, selectedDate));

      if (!filtered.length) {
        showEmpty();
        return;
      }

      if (!window.TaskUI || typeof window.TaskUI.renderWeekCard !== 'function') {
        showError('렌더 실패: TaskUI.renderWeekCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
        return;
      }

      $list.innerHTML = filtered.map(TaskUI.renderWeekCard).join('');
      showList();
    } catch (e) {
      showError(`Month 로딩 실패: ${e.message}`);
    } finally {
      $retry?.removeAttribute('disabled');
    }
  }

  $retry?.addEventListener('click', load);
  initCalendarFilters();

  // (선택) 가로 휠로 월 이동 유지
  if (grid) {
    grid.addEventListener('wheel', (e) => {
      const dx = e.deltaX;
      const dy = e.deltaY;

      const absX = Math.abs(dx);
      const absY = Math.abs(dy);

      if (absX < 20 || absX < absY) return;

      e.preventDefault();
      if (dx > 0) gotoMonth('next');
      else gotoMonth('prev');
    }, { passive: false });
  }

  load();
})();
