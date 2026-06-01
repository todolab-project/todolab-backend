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
  const $empty = document.getElementById('month-empty');
  const $card  = document.getElementById('month-card');
  const $list  = document.getElementById('month-list');
  const $doneEmpty = document.getElementById('month-done-empty');
  const $doneCard = document.getElementById('month-done-card');
  const $doneList = document.getElementById('month-done-list');
  const $doneCount = document.getElementById('month-done-count');

  const selectedDate = (root.dataset.selectedDate || '').trim(); // yyyy-MM-dd
  const monthLabel   = (root.dataset.monthLabel || '').trim();   // yyyy-MM
  const monthStart   = (root.dataset.monthStart || '').trim();   // yyyy-MM-dd
  const currentDate  = (root.dataset.currentDate || '').trim();

  let navLocked = false;
  const NAV_LOCK_MS = 650;

  function lockNav() {
    navLocked = true;
    setTimeout(() => (navLocked = false), NAV_LOCK_MS);
  }

  function hideAll() {
    $error?.classList.add('hidden');
    $empty?.classList.add('hidden');
    $card?.classList.add('hidden');
  }

  function showError(msg) {
    hideAll();
    if ($error) {
      $error.textContent = msg;
      $error.classList.remove('hidden');
    }
  }

  function showEmpty() {
    hideAll();
    $empty?.classList.remove('hidden');
  }

  function showList(n) {
    hideAll();
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

  function taskDate(t) {
    return (t?.startAt || t?.targetDate || '').split('T')[0];
  }

  function isTaskOnDate(t, date) {
    if (!date) return true;
    const s = taskDate(t);
    const e = (t?.endAt || '').split('T')[0];
    if (!s) return false;
    if (!e) return s === date;
    return (s <= date && date <= e);
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
    try {
      $error?.classList.add('hidden');
      applyTodayRing();

      const ym = monthLabel || (monthStart ? monthStart.slice(0, 7) : '');
      const [tasks, todayTasks, doneTasks] = await Promise.all([
        TaskApi.getMonthTasks(ym),
        selectedDate ? TaskApi.getTodayTasks(selectedDate) : Promise.resolve([]),
        selectedDate ? TaskApi.getDoneTasks(selectedDate) : Promise.resolve([])
      ]);
      renderDone(doneTasks);

      // 하단 리스트는 selectedDate 기준
      const mergedTasks = mergeTasks(tasks, todayTasks);
      const filtered = Array.isArray(tasks)
        ? mergedTasks.filter(t => isTaskOnDate(t, selectedDate))
        : [];

      if (!filtered.length) {
        showEmpty();
        return;
      }

      if (!window.TaskUI || typeof window.TaskUI.renderWeekCard !== 'function') {
        showError('렌더 실패: TaskUI.renderWeekCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
        showEmpty();
        return;
      }

      showList(filtered.length);
      $list.innerHTML = filtered.map(TaskUI.renderWeekCard).join('');
    } catch (e) {
      showError(`Month 로딩 실패: ${e.message}`);
      showEmpty();
    }
  }

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
