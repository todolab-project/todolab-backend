// src/main/resources/static/js/week.js
(() => {
  const root = document.getElementById('week-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const strip = document.getElementById('weekStrip');
  const prevBtn = document.getElementById('weekPrevHint');
  const nextBtn = document.getElementById('weekNextHint');

  const $error = document.getElementById('week-error');
  const $empty = document.getElementById('week-empty');
  const $card  = document.getElementById('week-card');
  const $list  = document.getElementById('week-list');
  const $doneEmpty = document.getElementById('week-done-empty');
  const $doneCard = document.getElementById('week-done-card');
  const $doneList = document.getElementById('week-done-list');
  const $doneCount = document.getElementById('week-done-count');

  const selectedDate = (root.dataset.selectedDate || '').trim(); // yyyy-MM-dd
  const weekStart = (root.dataset.weekStart || '').trim();       // yyyy-MM-dd

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

  function showList() {
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

  function gotoWeek(deltaWeeks) {
    if (navLocked) return;
    lockNav();

    const base = weekStart || selectedDate;
    const url = `/tasks/week?move=${deltaWeeks < 0 ? 'prev' : 'next'}&date=${encodeURIComponent(base)}`;
    window.location.href = url;
  }

  function gotoPrevWeek() { gotoWeek(-1); }
  function gotoNextWeek() { gotoWeek(1); }

  prevBtn?.addEventListener('click', (e) => { e.preventDefault(); gotoPrevWeek(); });
  nextBtn?.addEventListener('click', (e) => { e.preventDefault(); gotoNextWeek(); });

  function todayYmd() {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function applyTodayRing() {
    const today = todayYmd();

    const nodes = document.querySelectorAll('#weekStrip a[data-date]');
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

      const date = selectedDate || weekStart;
      const [tasks, doneTasks] = await Promise.all([
        TaskApi.getWeekTasks(date),
        selectedDate ? TaskApi.getDoneTasks(selectedDate) : Promise.resolve([])
      ]);
      renderDone(doneTasks);

      const filtered = Array.isArray(tasks)
        ? tasks.filter(t => {
            if (!selectedDate) return true;
            const s = (t.startAt || '').split('T')[0];
            const e = (t.endAt || '').split('T')[0];
            if (!s) return false;
            if (!e) return s === selectedDate;
            return (s <= selectedDate && selectedDate <= e);
          })
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

      showList();
      $list.innerHTML = filtered.map(TaskUI.renderWeekCard).join('');
    } catch (e) {
      showError(`Week 로딩 실패: ${e.message}`);
      showEmpty();
    }
  }

  if (strip) {
    strip.addEventListener('wheel', (e) => {
      const dx = e.deltaX;
      const dy = e.deltaY;

      const absX = Math.abs(dx);
      const absY = Math.abs(dy);

      if (absX < 20 || absX < absY) return;

      e.preventDefault();
      if (dx > 0) gotoNextWeek();
      else gotoPrevWeek();
    }, { passive: false });
  }

  // drag gesture (가로 드래그로 주 이동)
  let startX = 0;
  let dragging = false;

  function onDragStart(clientX) {
    dragging = true;
    startX = clientX;
  }

  function onDragEnd(clientX) {
    if (!dragging) return;
    dragging = false;

    const dx = clientX - startX;
    const TH = 40;
    if (Math.abs(dx) < TH) return;

    if (dx < 0) gotoNextWeek();
    else gotoPrevWeek();
  }

  if (strip) {
    strip.addEventListener('mousedown', (e) => onDragStart(e.clientX));
    window.addEventListener('mouseup', (e) => onDragEnd(e.clientX));

    strip.addEventListener('touchstart', (e) => {
      if (!e.touches || e.touches.length === 0) return;
      onDragStart(e.touches[0].clientX);
    }, { passive: true });

    strip.addEventListener('touchend', (e) => {
      const t = e.changedTouches && e.changedTouches[0];
      if (!t) return;
      onDragEnd(t.clientX);
    }, { passive: true });
  }

  load();
})();
