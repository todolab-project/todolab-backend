// src/main/resources/static/js/unscheduled.js
(() => {
  const root = document.getElementById('unscheduled-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const $loading = document.getElementById('unscheduled-loading');
  const $error   = document.getElementById('unscheduled-error');
  const $empty   = document.getElementById('unscheduled-empty');
  const $card    = document.getElementById('unscheduled-card');
  const $list    = document.getElementById('unscheduled-list');
  const $count   = document.getElementById('unscheduled-count');

  function todayYmd() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function hideAll() {
    $loading?.classList.add('hidden');
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
    if ($count) $count.classList.add('hidden');
  }

  function showEmpty() {
    hideAll();
    $empty?.classList.remove('hidden');
    if ($count) $count.classList.add('hidden');
  }

  function showList(n) {
    hideAll();
    $card?.classList.remove('hidden');

    if ($count) {
      $count.textContent = `${n}개`;
      $count.classList.remove('hidden');
    }
  }

  function sortTasks(tasks) {
    return [...tasks].sort((a, b) => {
      const ca = a.createdAt ?? '';
      const cb = b.createdAt ?? '';
      if (ca !== cb) return ca < cb ? 1 : -1;

      const at = (a.title ?? '').toLowerCase();
      const bt = (b.title ?? '').toLowerCase();
      return at.localeCompare(bt);
    });
  }

  function render(tasks) {
    if (!Array.isArray(tasks) || tasks.length === 0) {
      showEmpty();
      return;
    }

    if (!window.TaskUI || typeof window.TaskUI.renderInboxCard !== 'function') {
      showError('렌더 실패: TaskUI.renderInboxCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
      return;
    }

    showList(tasks.length);
    $list.innerHTML = tasks.map(TaskUI.renderInboxCard).join('');
  }

  async function load() {
    try {
      $loading?.classList.remove('hidden');
      $error?.classList.add('hidden');

      const raw = (await TaskApi.getInboxTasks()) ?? [];
      render(sortTasks(raw));
    } catch (e) {
      showError(`Inbox 로딩 실패: ${e.message}`);
    } finally {
      $loading?.classList.add('hidden');
    }
  }

  $list?.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="move-to-today"]');
    if (!btn) return;

    e.preventDefault();
    e.stopPropagation();

    const id = btn.getAttribute('data-task-id');
    if (!id) return;

    try {
      btn.disabled = true;
      await TaskApi.moveToToday(id, todayYmd());
      await load();
    } catch (err) {
      showError(`Today 이동 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  load();
})();
