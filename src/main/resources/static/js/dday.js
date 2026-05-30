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
    $list.innerHTML = items.map(goal => `
      <div class="rounded-2xl border border-gray-200 bg-white px-5 py-4 shadow-sm">
        <div class="flex items-center justify-between gap-3">
          <div class="min-w-0">
            <div class="truncate text-[16px] font-black text-gray-900">${escapeHtml(goal.title)}</div>
            <div class="mt-1 text-[12px] font-semibold text-gray-500">${escapeHtml(goal.targetDate)}</div>
          </div>
          <div class="shrink-0 text-right">
            <div class="text-[18px] font-black text-indigo-600">${escapeHtml(ddayLabel(goal.daysLeft))}</div>
            <button type="button"
                    class="mt-2 text-[12px] font-extrabold text-gray-400 hover:text-red-600"
                    data-action="delete-dday"
                    data-dday-id="${escapeHtml(goal.id)}">
              삭제
            </button>
          </div>
        </div>
      </div>
    `).join('');
  }

  async function load() {
    try {
      $loading?.classList.remove('hidden');
      $error?.classList.add('hidden');
      const goals = await request('/api/ddays');
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
    const btn = e.target.closest('[data-action="delete-dday"]');
    if (!btn) return;

    e.preventDefault();
    const id = btn.getAttribute('data-dday-id');
    if (!id) return;

    try {
      btn.disabled = true;
      await request(`/api/ddays/${encodeURIComponent(id)}`, {
        method: 'DELETE',
        headers: { 'X-Requested-With': 'fetch' }
      });
      await load();
    } catch (err) {
      showError(`D-Day 삭제 실패: ${err.message}`);
    } finally {
      btn.disabled = false;
    }
  });

  load();
})();
