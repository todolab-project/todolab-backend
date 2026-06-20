// src/main/resources/static/js/dday.js
(() => {
  const root = document.getElementById('dday-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const $form = document.getElementById('ddayForm');
  const $title = document.getElementById('ddayTitle');
  const $targetDate = document.getElementById('ddayTargetDate');
  const $submit = document.getElementById('ddaySubmit');
  const $loading = document.getElementById('dday-loading');
  const $error = document.getElementById('dday-error');
  const $empty = document.getElementById('dday-empty');
  const $card = document.getElementById('dday-card');
  const $list = document.getElementById('dday-list');
  const $count = document.getElementById('dday-count');

  let selectedGoalId = null;
  let selectedGoalTasks = [];
  let goalTaskCounts = new Map();

  async function request(path, options = {}) {
    const res = await fetch(path, {
      ...options,
      headers: {
        'Accept': 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(options.headers || {})
      }
    });

    const body = await res.json().catch(() => null);
    if (!res.ok || body?.status === 'fail') {
      throw new Error(body?.error?.message || body?.message || `HTTP ${res.status}`);
    }

    return body?.data ?? null;
  }

  function todayYmd() {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    const d = String(now.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function escapeHtml(v) {
    if (v === null || v === undefined) return '';
    return String(v)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function ddayLabel(daysLeft) {
    if (window.TaskUI?.formatDdayLabel) {
      return TaskUI.formatDdayLabel(daysLeft);
    }
    const n = Number(daysLeft);
    if (n === 0) return 'D-Day';
    if (n > 0) return `D-${n}`;
    return `D+${Math.abs(n)}`;
  }

  function showError(message) {
    $loading?.classList.add('hidden');
    $card?.classList.add('hidden');
    $empty?.classList.add('hidden');
    if ($error) {
      $error.textContent = message;
      $error.classList.remove('hidden');
    }
  }

  function renderLinkedTasks() {
    const tasks = Array.isArray(selectedGoalTasks) ? selectedGoalTasks : [];
    if (!tasks.length) {
      return `
        <div class="app-empty mt-3">
          아직 연결된 오늘 할 일이 없어요
        </div>
      `;
    }

    return `
      <div class="mt-3 space-y-2">
        ${tasks.map(task => {
          const plannedDate = window.TaskUI?.plannedDate?.(task) || task.plannedDate || task.targetDate;
          const plannedDateLabel = plannedDate
            ? (window.TaskUI?.formatDateKorean?.(plannedDate) || plannedDate)
            : null;
          if (window.TaskUI?.renderTaskCard) {
            return TaskUI.renderTaskCard(task, {
              showRightTime: true,
              metaText: plannedDateLabel ? `할 날짜 · ${plannedDateLabel}` : null,
              barColor: 'rgba(99, 102, 241, 0.55)',
              showDesc: false
            });
          }

          return `
            <div class="rounded-lg border border-slate-200 bg-white px-4 py-3">
              <div class="truncate text-[14px] font-black text-gray-900">${escapeHtml(task.title)}</div>
              ${plannedDateLabel ? `<div class="mt-1 text-[12px] font-semibold text-gray-500">할 날짜 · ${escapeHtml(plannedDateLabel)}</div>` : ''}
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  function render(goals) {
    const items = Array.isArray(goals) ? goals : [];
    $loading?.classList.add('hidden');
    $error?.classList.add('hidden');

    if ($count) $count.textContent = `${items.length}개`;

    if (!items.length) {
      $card?.classList.add('hidden');
      $empty?.classList.remove('hidden');
      return;
    }

    $empty?.classList.add('hidden');
    $card?.classList.remove('hidden');
    $list.innerHTML = items.map(goal => {
      const selected = String(goal.id) === String(selectedGoalId);
      const counts = goalTaskCounts.get(String(goal.id)) || { today: 0, total: 0 };
      const targetDateLabel = window.TaskUI?.formatDateKorean?.(goal.targetDate, { includeYear: true })
        || goal.targetDate;
      return `
        <div class="dday-goal-card ${selected ? 'dday-goal-card-selected' : ''}"
             data-action="select-dday"
             data-dday-id="${escapeHtml(goal.id)}"
             role="button"
             tabindex="0"
             aria-expanded="${selected ? 'true' : 'false'}">
          <div class="flex items-center justify-between gap-3">
            <div class="min-w-0">
              <div class="truncate text-[16px] font-black text-gray-900">${escapeHtml(goal.title)}</div>
              <div class="mt-1 text-[12px] font-semibold text-gray-500">${escapeHtml(targetDateLabel)}</div>
              <div class="dday-goal-meta">
                <span class="dday-goal-task-count">오늘 할 일 ${counts.today}개</span>
                ${counts.total > counts.today
                  ? `<span class="dday-goal-task-total">전체 연결 ${counts.total}개</span>`
                  : ''}
              </div>
            </div>
            <div class="dday-goal-side">
              <div class="dday-goal-side-top">
                <div class="text-[18px] font-black text-blue-700">${escapeHtml(ddayLabel(goal.daysLeft))}</div>
                <div class="dday-goal-expand" aria-hidden="true">
                  <svg viewBox="0 0 20 20" fill="none">
                    <path d="M5.5 8 10 12.5 14.5 8"
                          stroke="currentColor"
                          stroke-width="2"
                          stroke-linecap="round"
                          stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
              <button type="button"
                      class="dday-goal-delete"
                      data-action="delete-dday"
                      data-dday-id="${escapeHtml(goal.id)}"
                      data-dday-title="${escapeHtml(goal.title)}">
                삭제
              </button>
            </div>
          </div>

          ${selected ? `
            <form class="mt-4 grid grid-cols-[1fr_auto] gap-2"
                  data-action="add-dday-task"
                  data-dday-id="${escapeHtml(goal.id)}">
              <input type="text"
                     name="title"
                     maxlength="30"
                     class="min-w-0 rounded-lg border border-slate-200 bg-white px-4 py-3 text-[14px] font-semibold text-gray-900 outline-none"
                     placeholder="오늘 할 일" />
              <button type="submit"
                      class="app-primary-btn rounded-lg px-4 py-3 text-[13px]">
                추가
              </button>
            </form>
            ${renderLinkedTasks()}
          ` : ''}
        </div>
      `;
    }).join('');
  }

  async function loadGoalTaskCounts(goals) {
    const items = Array.isArray(goals) ? goals : [];
    const entries = await Promise.all(items.map(async goal => {
      const id = String(goal.id);
      const tasks = await request(`/api/ddays/${encodeURIComponent(id)}/tasks`);
      if (String(selectedGoalId) === id) {
        selectedGoalTasks = tasks;
      }
      const today = tasks.filter(task => String(task?.status || '') === 'TODAY').length;
      return [id, { today, total: tasks.length }];
    }));

    goalTaskCounts = new Map(entries);
    if (!selectedGoalId) {
      selectedGoalTasks = [];
    }
  }

  async function load() {
    try {
      $loading?.classList.remove('hidden');
      $error?.classList.add('hidden');
      const goals = await request('/api/ddays');
      if (selectedGoalId && !goals.some(goal => String(goal.id) === String(selectedGoalId))) {
        selectedGoalId = null;
        selectedGoalTasks = [];
      }
      await loadGoalTaskCounts(goals);
      render(goals);
    } catch (err) {
      showError(`D-Day 로딩 실패: ${err.message}`);
    }
  }

  if ($targetDate && !$targetDate.value) {
    $targetDate.value = todayYmd();
  }

  $form?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const title = ($title?.value || '').trim();
    const targetDate = ($targetDate?.value || '').trim();
    if (!title) {
      $title?.focus();
      return;
    }
    if (!targetDate) {
      $targetDate?.focus();
      return;
    }

    try {
      if ($submit) $submit.disabled = true;
      await request('/api/ddays', {
        method: 'POST',
        headers: { 'X-Requested-With': 'fetch' },
        body: JSON.stringify({ title, targetDate })
      });
      $title.value = '';
      await load();
    } catch (err) {
      showError(`D-Day 추가 실패: ${err.message}`);
    } finally {
      if ($submit) $submit.disabled = false;
    }
  });

  $list?.addEventListener('click', async (e) => {
    const deleteBtn = e.target.closest('[data-action="delete-dday"]');
    if (deleteBtn) {
      e.preventDefault();
      e.stopPropagation();

      const id = deleteBtn.getAttribute('data-dday-id');
      if (!id) return;
      const title = (deleteBtn.getAttribute('data-dday-title') || '이 D-Day').trim();
      const confirmed = await window.AppFeedback?.confirm?.({
        title: 'D-Day를 삭제할까요?',
        message: `'${title}'을(를) 삭제합니다.\n연결된 오늘 할 일은 삭제되지 않고 D-Day 연결만 해제돼요.`,
        confirmText: '삭제',
        danger: true
      });
      if (!confirmed) return;

      try {
        deleteBtn.disabled = true;
        await request(`/api/ddays/${encodeURIComponent(id)}`, {
          method: 'DELETE',
          headers: { 'X-Requested-With': 'fetch' }
        });
        if (String(selectedGoalId) === String(id)) {
          selectedGoalId = null;
          selectedGoalTasks = [];
        }
        await load();
      } catch (err) {
        showError(`D-Day 삭제 실패: ${err.message}`);
      } finally {
        deleteBtn.disabled = false;
      }
      return;
    }

    const card = e.target.closest('[data-action="select-dday"]');
    if (!card || e.target.closest('button,input,textarea,select,label')) return;

    e.preventDefault();
    const id = card.getAttribute('data-dday-id');
    selectedGoalId = String(selectedGoalId) === String(id) ? null : id;

    try {
      await load();
    } catch (err) {
      showError(`D-Day 할 일 로딩 실패: ${err.message}`);
    }
  });

  $list?.addEventListener('keydown', async (e) => {
    if (e.key !== 'Enter' && e.key !== ' ') return;

    const card = e.target.closest('[data-action="select-dday"]');
    if (!card || e.target !== card) return;

    e.preventDefault();
    const id = card.getAttribute('data-dday-id');
    selectedGoalId = String(selectedGoalId) === String(id) ? null : id;

    try {
      await load();
    } catch (err) {
      showError(`D-Day 할 일 로딩 실패: ${err.message}`);
    }
  });

  $list?.addEventListener('submit', async (e) => {
    const form = e.target.closest('[data-action="add-dday-task"]');
    if (!form) return;

    e.preventDefault();
    const ddayGoalId = form.getAttribute('data-dday-id');
    const input = form.querySelector('input[name="title"]');
    const button = form.querySelector('button[type="submit"]');
    const title = (input?.value || '').trim();
    if (!title) {
      input?.focus();
      return;
    }

    try {
      if (button) button.disabled = true;
      const task = await TaskApi.createTask({
        title,
        description: '',
        category: '',
        type: 'TODO',
        allDay: false,
        startAt: null,
        endAt: null
      });
      await TaskApi.moveToToday(task.id, todayYmd());
      await TaskApi.connectDdayGoal(task.id, ddayGoalId);
      input.value = '';
      selectedGoalId = ddayGoalId;
      await load();
    } catch (err) {
      showError(`오늘 할 일 추가 실패: ${err.message}`);
    } finally {
      if (button) button.disabled = false;
    }
  });

  load();
})();
