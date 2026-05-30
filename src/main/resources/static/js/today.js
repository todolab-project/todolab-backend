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

  function setDoneCount(n) {
    if (!$doneCount) return;
    $doneCount.textContent = `${n}개`;
  }

  function setInboxCount(n) {
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
    $doneList.innerHTML = doneTasks.map(TaskUI.renderDoneCard).join('');
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

      const [todayTasks, inboxTasks, doneTasks] = await Promise.all([
        TaskApi.getTodayTasks(date),
        TaskApi.getInboxTasks(),
        TaskApi.getDoneTasks(date)
      ]);

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
    } catch (err) {
      showError(`기록 실패: ${err.message}`);
    } finally {
      if ($quickSubmit) $quickSubmit.disabled = false;
    }
  });

  $list?.addEventListener('click', async (e) => {
    const btn = e.target.closest('[data-action="complete-task"]');
    if (!btn) return;

    e.preventDefault();
    e.stopPropagation();

    const id = btn.getAttribute('data-task-id');
    if (!id) return;

    try {
      btn.disabled = true;
      await TaskApi.completeTask(id);
      await load();
    } catch (err) {
      showError(`완료 처리 실패: ${err.message}`);
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
    } catch (err) {
      showError(`오늘 할 일 이동 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  load();
})();
