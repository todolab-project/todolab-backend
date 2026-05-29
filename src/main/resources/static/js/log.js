// src/main/resources/static/js/log.js
(() => {
  const root = document.getElementById('log-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const date = root.dataset.date;

  const $loading = document.getElementById('log-loading');
  const $error = document.getElementById('log-error');
  const $empty = document.getElementById('log-empty');
  const $card = document.getElementById('log-card');
  const $list = document.getElementById('log-list');
  const $count = document.getElementById('log-count');
  const $dateText = document.getElementById('logDateText');
  const $prevLink = document.getElementById('logPrevLink');
  const $nextLink = document.getElementById('logNextLink');

  function shiftDate(yyyyMmDd, days) {
    const [y, m, d] = yyyyMmDd.split('-').map(Number);
    const dt = new Date(y, m - 1, d);
    dt.setDate(dt.getDate() + days);
    const yy = dt.getFullYear();
    const mm = String(dt.getMonth() + 1).padStart(2, '0');
    const dd = String(dt.getDate()).padStart(2, '0');
    return `${yy}-${mm}-${dd}`;
  }

  function fmtDowKorean(yyyyMmDd) {
    try {
      const [y, m, d] = yyyyMmDd.split('-').map(Number);
      const dt = new Date(y, m - 1, d);
      const map = ['일', '월', '화', '수', '목', '금', '토'];
      return map[dt.getDay()];
    } catch {
      return '';
    }
  }

  function hideAll() {
    $loading?.classList.add('hidden');
    $error?.classList.add('hidden');
    $empty?.classList.add('hidden');
    $card?.classList.add('hidden');
  }

  function setCount(n) {
    if (!$count) return;
    if (n <= 0) {
      $count.classList.add('hidden');
      $count.textContent = '';
      return;
    }
    $count.textContent = `${n}개`;
    $count.classList.remove('hidden');
  }

  function showError(msg) {
    hideAll();
    if ($error) {
      $error.textContent = msg;
      $error.classList.remove('hidden');
    }
    setCount(0);
  }

  function showEmpty() {
    hideAll();
    $empty?.classList.remove('hidden');
    setCount(0);
  }

  function showList(n) {
    hideAll();
    $card?.classList.remove('hidden');
    setCount(n);
  }

  function render(tasks) {
    if (!Array.isArray(tasks) || tasks.length === 0) {
      showEmpty();
      return;
    }

    if (!window.TaskUI || typeof window.TaskUI.renderDoneCard !== 'function') {
      showError('렌더 실패: TaskUI.renderDoneCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
      return;
    }

    showList(tasks.length);
    $list.innerHTML = tasks.map(TaskUI.renderDoneCard).join('');
  }

  async function load() {
    try {
      $loading?.classList.remove('hidden');
      $error?.classList.add('hidden');

      if ($dateText && date) {
        const dow = fmtDowKorean(date);
        $dateText.textContent = dow ? `${date} (${dow})` : date;
      }

      if ($prevLink) $prevLink.href = `/tasks/log?date=${encodeURIComponent(shiftDate(date, -1))}`;
      if ($nextLink) $nextLink.href = `/tasks/log?date=${encodeURIComponent(shiftDate(date, 1))}`;

      const tasks = await TaskApi.getDoneTasks(date);
      render(tasks ?? []);
    } catch (e) {
      showError(`완료 로그 로딩 실패: ${e.message}`);
    } finally {
      $loading?.classList.add('hidden');
    }
  }

  load();
})();
