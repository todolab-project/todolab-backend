// src/main/resources/static/js/today.js
(() => {
  const root = document.getElementById('today-page');
  if (!root) return;

  if (root.dataset.bound === '1') return;
  root.dataset.bound = '1';

  const date = root.dataset.date; // yyyy-MM-dd

  const $loading  = document.getElementById('today-loading');
  const $error    = document.getElementById('today-error');
  const $empty    = document.getElementById('today-empty');
  const $card     = document.getElementById('today-card');
  const $list     = document.getElementById('today-list');
  const $count    = document.getElementById('today-count');
  const $dateText = document.getElementById('todayDateText');
  const $quickForm = document.getElementById('todayQuickForm');
  const $quickTitle = document.getElementById('todayQuickTitle');
  const $quickSubmit = document.getElementById('todayQuickSubmit');
  const $summaryToday = document.getElementById('today-summary-today');
  const $summaryInbox = document.getElementById('today-summary-inbox');
  const $summaryDone = document.getElementById('today-summary-done');
  const $overdueSection = document.getElementById('today-overdue-section');
  const $overdueList = document.getElementById('today-overdue-list');
  const $overdueCount = document.getElementById('today-overdue-count');
  const $recommendationSection = document.getElementById('today-recommendation-section');
  const $recommendationList = document.getElementById('today-recommendation-list');
  const $recommendationCount = document.getElementById('today-recommendation-count');
  const $inboxEmpty = document.getElementById('today-inbox-empty');
  const $inboxCard = document.getElementById('today-inbox-card');
  const $inboxList = document.getElementById('today-inbox-list');
  const $inboxCount = document.getElementById('today-inbox-count');
  const $doneEmpty = document.getElementById('today-done-empty');
  const $doneCard = document.getElementById('today-done-card');
  const $doneList = document.getElementById('today-done-list');
  const $doneCount = document.getElementById('today-done-count');

  function hideAll() {
    $loading?.classList.add('hidden');
    $error?.classList.add('hidden');
    $empty?.classList.add('hidden');
    $card?.classList.add('hidden');
  }

  function setCount(n) {
    if ($summaryToday) $summaryToday.textContent = String(Math.max(0, n || 0));
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
    $overdueSection?.classList.add('hidden');
    $recommendationSection?.classList.add('hidden');
    if ($error) {
      $error.textContent = msg;
      $error.classList.remove('hidden');
    }
    setCount(0);
  }

  function showActionSuccess(msg) {
    window.AppFeedback?.success(msg);
  }

  function showActionError(msg) {
    if (window.AppFeedback?.error) {
      window.AppFeedback.error(msg);
      return;
    }
    if ($error) {
      $error.textContent = msg;
      $error.classList.remove('hidden');
    }
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

  function shiftDate(yyyyMmDd, days) {
    const [y, m, d] = yyyyMmDd.split('-').map(Number);
    const dt = new Date(y, m - 1, d);
    dt.setDate(dt.getDate() + days);
    const yy = dt.getFullYear();
    const mm = String(dt.getMonth() + 1).padStart(2, '0');
    const dd = String(dt.getDate()).padStart(2, '0');
    return `${yy}-${mm}-${dd}`;
  }

  function setDoneCount(n) {
    if ($summaryDone) $summaryDone.textContent = String(Math.max(0, n || 0));
    if (!$doneCount) return;
    $doneCount.textContent = `${n}개`;
  }

  function setInboxCount(n) {
    if ($summaryInbox) $summaryInbox.textContent = String(Math.max(0, n || 0));
    if (!$inboxCount) return;
    $inboxCount.textContent = `${n}개`;
  }

  function renderToday(tasks) {
    if (!Array.isArray(tasks) || tasks.length === 0) {
      showEmpty();
      return;
    }

    if (!window.TaskUI || typeof window.TaskUI.renderTodayCard !== 'function') {
      showError('렌더 실패: TaskUI.renderTodayCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
      return;
    }

    showList(tasks.length);
    $list.innerHTML = tasks.map(TaskUI.renderTodayCard).join('');
  }

  function renderOverdue(tasks) {
    const overdueTasks = Array.isArray(tasks) ? tasks : [];

    if (overdueTasks.length === 0) {
      $overdueSection?.classList.add('hidden');
      if ($overdueList) $overdueList.innerHTML = '';
      if ($overdueCount) $overdueCount.textContent = '0개';
      return;
    }

    if (!window.TaskUI || typeof window.TaskUI.renderOverdueCard !== 'function') {
      showError('렌더 실패: TaskUI.renderOverdueCard를 찾을 수 없습니다. (task-ui.js 로드 순서 확인)');
      return;
    }

    if ($overdueCount) $overdueCount.textContent = `${overdueTasks.length}개`;
    if ($overdueList) {
      $overdueList.innerHTML = overdueTasks
        .map(task => TaskUI.renderOverdueCard(task, date))
        .join('');
    }
    $overdueSection?.classList.remove('hidden');
  }

  function renderRecommendations(recommendations) {
    const items = Array.isArray(recommendations) ? recommendations : [];
    if (items.length === 0) {
      $recommendationSection?.classList.add('hidden');
      if ($recommendationList) $recommendationList.innerHTML = '';
      if ($recommendationCount) $recommendationCount.textContent = '0개';
      return;
    }

    if (!window.TaskUI || typeof window.TaskUI.renderRecommendationCard !== 'function') {
      return;
    }

    if ($recommendationCount) $recommendationCount.textContent = `${items.length}개`;
    if ($recommendationList) {
      $recommendationList.innerHTML = items.map(TaskUI.renderRecommendationCard).join('');
    }
    $recommendationSection?.classList.remove('hidden');
  }

  function renderDone(tasks) {
    const doneTasks = Array.isArray(tasks) ? tasks : [];
    setDoneCount(doneTasks.length);

    if (!window.TaskUI || typeof window.TaskUI.renderDoneCard !== 'function') {
      return;
    }

    if (doneTasks.length === 0) {
      $doneCard?.classList.add('hidden');
      $doneEmpty?.classList.remove('hidden');
      return;
    }

    $doneEmpty?.classList.add('hidden');
    $doneCard?.classList.remove('hidden');
    $doneList.innerHTML = doneTasks.map(t => TaskUI.renderDoneCard(t, { reopenAction: true })).join('');
  }

  function renderInbox(tasks) {
    const inboxTasks = Array.isArray(tasks) ? tasks : [];
    setInboxCount(inboxTasks.length);

    if (!window.TaskUI || typeof window.TaskUI.renderInboxCard !== 'function') {
      return;
    }

    if (inboxTasks.length === 0) {
      $inboxCard?.classList.add('hidden');
      $inboxEmpty?.classList.remove('hidden');
      return;
    }

    $inboxEmpty?.classList.add('hidden');
    $inboxCard?.classList.remove('hidden');
    $inboxList.innerHTML = inboxTasks.map(TaskUI.renderInboxCard).join('');
  }

  async function load() {
    try {
      // 로딩 시작
      $loading?.classList.remove('hidden');
      $error?.classList.add('hidden');

      // 날짜 라벨에 요일 붙이기
      if ($dateText && date) {
        const dow = fmtDowKorean(date);
        $dateText.textContent = dow ? `${date} (${dow})` : date;
      }

      const [todayTasks, overdueTasks, recommendations, inboxTasks, doneTasks] = await Promise.all([
        TaskApi.getTodayTasks(date),
        TaskApi.getOverdueTasks(date),
        TaskApi.getTodayRecommendations(date),
        TaskApi.getInboxTasks(),
        TaskApi.getDoneTasks(date)
      ]);

      renderOverdue(overdueTasks ?? []);
      renderRecommendations(recommendations ?? []);
      renderToday(todayTasks ?? []);
      renderInbox(inboxTasks ?? []);
      renderDone(doneTasks ?? []);
    } catch (e) {
      showError(`Today 로딩 실패: ${e.message}`);
    } finally {
      // ✅ 항상 로딩 종료
      $loading?.classList.add('hidden');
    }
  }

  async function createInboxTask(title) {
    await TaskApi.createTask({
      title,
      description: '',
      category: '',
      type: 'TODO',
      allDay: false,
      startAt: null,
      endAt: null
    });
  }

  $quickForm?.addEventListener('submit', async (e) => {
    e.preventDefault();

    const title = ($quickTitle?.value || '').trim();
    if (!title) {
      $quickTitle?.focus();
      return;
    }

    try {
      if ($quickSubmit) $quickSubmit.disabled = true;
      await createInboxTask(title);
      $quickTitle.value = '';
      await load();
      showActionSuccess('기록함에 추가했어요.');
    } catch (err) {
      showActionError(`기록 실패: ${err.message}`);
    } finally {
      if ($quickSubmit) $quickSubmit.disabled = false;
    }
  });

  $list?.addEventListener('click', async (e) => {
    const btn = e.target.closest(
      '[data-action="complete-task"], [data-action="carry-over-task"], [data-action="move-to-inbox"], ' +
      '[data-action="today-order-up"], [data-action="today-order-down"]'
    );
    if (!btn) return;

    e.preventDefault();
    e.stopPropagation();

    const id = btn.getAttribute('data-task-id');
    if (!id) return;

    if (btn.dataset.action === 'move-to-inbox') {
      if (!confirm('날짜와 시간을 제거하고 기록함으로 이동할까요?')) return;

      try {
        btn.disabled = true;
        await TaskApi.moveToInbox(id);
        await load();
        showActionSuccess('일정을 제거하고 기록함으로 이동했어요.');
      } catch (err) {
        showActionError(`기록함 이동 실패: ${err.message}`);
      } finally {
        btn.disabled = false;
      }
      return;
    }

    if (btn.dataset.action === 'today-order-up' || btn.dataset.action === 'today-order-down') {
      try {
        btn.disabled = true;
        const direction = btn.dataset.action === 'today-order-up' ? 'UP' : 'DOWN';
        await TaskApi.reorderToday(id, date, direction);
        await load();
      } catch (err) {
        showActionError(`순서 변경 실패: ${err.message}`);
      } finally {
        btn.disabled = false;
      }
      return;
    }

    try {
      btn.disabled = true;
      if (btn.dataset.action === 'carry-over-task') {
        await TaskApi.carryOver(id, shiftDate(date, 1));
      } else {
        await TaskApi.completeTask(id);
      }
      await load();
      showActionSuccess(
        btn.dataset.action === 'carry-over-task'
          ? '내일 할 일로 옮겼어요.'
          : '완료했어요.'
      );
    } catch (err) {
      const actionLabel = btn.dataset.action === 'carry-over-task' ? '이월 처리' : '완료 처리';
      showActionError(`${actionLabel} 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  $list?.addEventListener('change', async (e) => {
    const select = e.target.closest('[data-action="set-defer-reason"]');
    if (!select) return;

    e.preventDefault();
    e.stopPropagation();

    const id = select.getAttribute('data-task-id');
    if (!id) return;

    try {
      select.disabled = true;
      const reason = (select.value || '').trim();
      if (reason) {
        await TaskApi.setDeferReason(id, reason);
      } else {
        await TaskApi.clearDeferReason(id);
      }
      await load();
      showActionSuccess(reason ? '미룬 이유를 저장했어요.' : '미룬 이유를 지웠어요.');
    } catch (err) {
      showActionError(`미룬 이유 저장 실패: ${err.message}`);
    } finally {
      select.disabled = false;
    }
  });

  $overdueList?.addEventListener('click', async (e) => {
    const menuToggle = e.target.closest('[data-action="toggle-overdue-menu"]');
    if (menuToggle) {
      e.preventDefault();
      e.stopPropagation();

      const card = menuToggle.closest('.task-card');
      const menu = card?.querySelector('[data-role="overdue-menu"]');
      if (!menu) return;

      const shouldOpen = menu.hidden;
      $overdueList.querySelectorAll('[data-role="overdue-menu"]').forEach(otherMenu => {
        otherMenu.hidden = true;
      });
      $overdueList.querySelectorAll('[data-action="toggle-overdue-menu"]').forEach(otherToggle => {
        otherToggle.setAttribute('aria-expanded', 'false');
      });

      menu.hidden = !shouldOpen;
      menuToggle.setAttribute('aria-expanded', String(shouldOpen));
      return;
    }

    const btn = e.target.closest(
      '[data-action="overdue-today"], [data-action="overdue-tomorrow"], ' +
      '[data-action="overdue-inbox"], [data-action="overdue-complete"], ' +
      '[data-action="overdue-reschedule"], [data-action="overdue-delete"]'
    );
    if (!btn) {
      if (e.target.closest('.task-overdue-actions')) {
        e.stopPropagation();
      }
      return;
    }

    e.preventDefault();
    e.stopPropagation();

    const id = btn.getAttribute('data-task-id');
    if (!id) return;

    const action = btn.dataset.action;
    if (action === 'overdue-inbox') {
      if (!confirm('날짜와 시간을 제거하고 기록함으로 이동할까요?')) return;
    }
    if (action === 'overdue-delete') {
      const title = (btn.dataset.taskTitle || '이 할 일').trim();
      if (!confirm(`'${title}'을(를) 삭제하시겠어요?`)) return;
    }

    const rescheduleDate = action === 'overdue-reschedule'
      ? btn.closest('.task-card')?.querySelector('[data-role="overdue-date"]')?.value
      : null;
    if (action === 'overdue-reschedule' && !rescheduleDate) {
      showActionError('다시 정할 날짜를 선택해주세요.');
      return;
    }
    if (action === 'overdue-reschedule' && rescheduleDate < date) {
      showActionError(`${date} 이후 날짜를 선택해주세요.`);
      return;
    }

    try {
      btn.disabled = true;
      if (action === 'overdue-today') {
        await TaskApi.carryOver(id, date);
      } else if (action === 'overdue-tomorrow') {
        await TaskApi.carryOver(id, shiftDate(date, 1));
      } else if (action === 'overdue-inbox') {
        await TaskApi.moveToInbox(id);
      } else if (action === 'overdue-reschedule') {
        await TaskApi.carryOver(id, rescheduleDate);
      } else if (action === 'overdue-delete') {
        await TaskApi.deleteTask(id);
      } else {
        await TaskApi.completeTask(id);
      }
      await load();
      const successMessages = {
        'overdue-today': '오늘 할 일로 옮겼어요.',
        'overdue-tomorrow': '내일 할 일로 옮겼어요.',
        'overdue-inbox': '일정을 제거하고 기록함으로 이동했어요.',
        'overdue-complete': '완료했어요.',
        'overdue-reschedule': '실행 날짜를 변경했어요.',
        'overdue-delete': '할 일을 삭제했어요.'
      };
      showActionSuccess(successMessages[action] || '지난 미완료를 정리했어요.');
    } catch (err) {
      const labels = {
        'overdue-today': '오늘 이동',
        'overdue-tomorrow': '내일 이동',
        'overdue-inbox': '기록함 이동',
        'overdue-complete': '완료 처리',
        'overdue-reschedule': '날짜 재설정',
        'overdue-delete': '삭제'
      };
      showActionError(`${labels[action] || '지난 미완료 정리'} 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  $overdueList?.addEventListener('change', async (e) => {
    const select = e.target.closest('[data-action="set-defer-reason"]');
    if (!select) return;

    e.preventDefault();
    e.stopPropagation();

    const id = select.getAttribute('data-task-id');
    if (!id) return;

    try {
      select.disabled = true;
      const reason = (select.value || '').trim();
      if (reason) {
        await TaskApi.setDeferReason(id, reason);
      } else {
        await TaskApi.clearDeferReason(id);
      }
      await load();
      showActionSuccess(reason ? '미룬 이유를 저장했어요.' : '미룬 이유를 지웠어요.');
    } catch (err) {
      showActionError(`미룬 이유 저장 실패: ${err.message}`);
    } finally {
      select.disabled = false;
    }
  });

  $doneList?.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="reopen-today-task"]');
    if (!btn) return;

    e.preventDefault();
    e.stopPropagation();

    const id = btn.getAttribute('data-task-id');
    if (!id) return;

    try {
      btn.disabled = true;
      await TaskApi.reopenToday(id, date);
      await load();
      showActionSuccess('오늘 할 일로 되돌렸어요.');
    } catch (err) {
      showActionError(`완료 취소 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  $inboxList?.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="move-to-today"]');
    if (!btn) return;

    e.preventDefault();
    e.stopPropagation();

    const id = btn.getAttribute('data-task-id');
    if (!id) return;

    try {
      btn.disabled = true;
      await TaskApi.moveToToday(id, date);
      await load();
      showActionSuccess('오늘 할 일로 옮겼어요.');
    } catch (err) {
      showActionError(`오늘 할 일 이동 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  $recommendationList?.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="move-to-today"]');
    if (!btn) return;

    e.preventDefault();
    e.stopPropagation();

    const id = btn.getAttribute('data-task-id');
    if (!id) return;

    try {
      btn.disabled = true;
      await TaskApi.moveToToday(id, date);
      await load();
      showActionSuccess('추천한 일을 오늘 할 일로 옮겼어요.');
    } catch (err) {
      showActionError(`추천 이동 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  load();
})();
