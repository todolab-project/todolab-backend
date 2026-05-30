// src/main/resources/static/js/task-ui.js
(() => {
  const TaskUI = {};

  /* -----------------------------
   * utils
   * ----------------------------- */
  TaskUI.escapeHtml = (v) => {
    if (v === null || v === undefined) return '';
    return String(v)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  };

  TaskUI.toDate = (iso) => (iso ? String(iso).split('T')[0] : null);

  TaskUI.toTimeHM = (iso) => {
    if (!iso) return null;
    const t = String(iso).split('T')[1] || '';
    return (t.substring(0, 5) || null);
  };

  /**
   * 카드 우측에 보여줄 "시간 텍스트" 생성
   * - 종일이면 "종일"
   * - start~end 둘다 있으면 "HH:mm ~ HH:mm"
   * - start만 있으면 "HH:mm"
   */
  TaskUI.formatRightTime = (task) => {
    if (!task) return '';
    if (task.allDay) return '종일';

    const st = TaskUI.toTimeHM(task.startAt);
    const et = TaskUI.toTimeHM(task.endAt);

    if (st && et) return `${st} ~ ${et}`;
    if (st) return `${st}`;
    return '';
  };

  /* -----------------------------
   * renderTaskCard (모든 페이지가 이걸로 통일)
   *
   * options:
   * - showRightTime: boolean (default false)
   * - rightText: string | null (우측 표시를 강제로 지정, showRightTime보다 우선)
   * - metaText: string | null (예: "등록일 · 2026-01-31")
   * - barColor: string | null (default indigo)
   * - showDesc: boolean (default true)
   * ----------------------------- */
  TaskUI.renderTaskCard = (task, options = {}) => {
    if (!task) return '';

    const title = TaskUI.escapeHtml(task.title || '(제목 없음)');
    const desc  = TaskUI.escapeHtml((task.description || '').trim());
    const cat   = TaskUI.escapeHtml((task.category || '').trim());

    const metaText = TaskUI.escapeHtml((options.metaText || '').trim());
    const showDesc = (options.showDesc !== false);

    // ✅ 우측 시간/텍스트는 옵션으로만 노출
    let right = '';
    if (
      options.rightText !== undefined &&
      options.rightText !== null &&
      String(options.rightText).trim() !== ''
    ) {
      right = TaskUI.escapeHtml(options.rightText);
    } else if (options.showRightTime) {
      right = TaskUI.escapeHtml(TaskUI.formatRightTime(task) || '');
    }

    // ✅ 좌측 바 컬러 (task.color가 있으면 우선)
    const barColor = TaskUI.escapeHtml(task.color || options.barColor || 'rgba(99, 102, 241, 0.55)');

    const checkHtml = options.completeAction
      ? `<button type="button"
                 class="check-box hover:bg-emerald-50 hover:text-emerald-700"
                 data-action="complete-task"
                 data-task-id="${TaskUI.escapeHtml(task.id)}"
                 aria-label="완료 처리">✓</button>`
      : `<div class="check-box">✓</div>`;

    const actionsHtml = options.carryOverAction
      ? `<div class="px-4 pb-4 flex justify-end">
           <button type="button"
                   class="rounded-lg border border-gray-200 bg-white px-3 py-2 text-[12px] font-extrabold text-gray-700 hover:bg-gray-50"
                   data-action="carry-over-task"
                   data-task-id="${TaskUI.escapeHtml(task.id)}">
             내일로
           </button>
         </div>`
      : '';

    return `
<div class="task-card task-card-clickable cursor-pointer hover:bg-gray-50 active:scale-[0.995]"
     data-task-id="${TaskUI.escapeHtml(task.id)}">
  <div class="task-row">
    <div class="task-left-bar" style="background:${barColor};"></div>
    ${checkHtml}

    <div class="min-w-0 flex-1">
      <div class="flex items-center gap-2 min-w-0">
        <div class="text-[16px] font-black text-gray-900 truncate">${title}</div>
        ${cat ? `<span class="task-badge">${cat}</span>` : ``}
      </div>

      ${metaText ? `<div class="mt-1 text-[12px] text-gray-500 font-semibold">${metaText}</div>` : ``}

      ${showDesc && desc ? `<div class="mt-2 text-[13px] text-gray-600 leading-snug whitespace-pre-wrap break-words line-clamp-3">${desc}</div>` : ``}
    </div>

    ${right ? `<div class="task-right">${right}</div>` : ``}
  </div>
  ${actionsHtml}
</div>`.trim();
  };

  /* -----------------------------
   * presets (페이지별 옵션 통일)
   * ----------------------------- */

  // ✅ Seeds(unscheduled): 우측 시간 X, 등록일 meta O
  TaskUI.renderSeedCard = (t) => {
    const created = TaskUI.toDate(t?.createdAt);
    const meta = created ? `등록일 · ${created}` : null;

    return TaskUI.renderTaskCard(t, {
      showRightTime: false,
      metaText: meta
    });
  };

  TaskUI.renderInboxCard = (t) => {
    const created = TaskUI.toDate(t?.createdAt);
    const meta = created ? `기록일 · ${created}` : null;
    const base = TaskUI.renderTaskCard(t, {
      showRightTime: false,
      metaText: meta,
      barColor: 'rgba(59, 130, 246, 0.55)'
    });

    return `
<div class="inbox-task-item" data-inbox-task-id="${TaskUI.escapeHtml(t?.id)}">
  ${base}
  <div class="mt-2 flex justify-end">
    <button type="button"
            class="rounded-lg border border-gray-200 bg-white px-3 py-2 text-[12px] font-extrabold text-gray-700 hover:bg-gray-50"
            data-action="move-to-today"
            data-task-id="${TaskUI.escapeHtml(t?.id)}">
      오늘 할 일로
    </button>
  </div>
</div>`.trim();
  };

  // ✅ Today: 우측 시간 O, meta X
  TaskUI.renderTodayCard = (t) => {
    return TaskUI.renderTaskCard(t, {
      showRightTime: true,
      metaText: null,
      completeAction: true,
      carryOverAction: true
    });
  };

  TaskUI.renderDoneCard = (t) => {
    const completedTime = TaskUI.toTimeHM(t?.completedAt);
    const meta = completedTime ? `완료 · ${completedTime}` : '완료';

    return TaskUI.renderTaskCard(t, {
      showRightTime: false,
      metaText: meta,
      barColor: 'rgba(16, 185, 129, 0.55)'
    });
  };

  // ✅ Week: 우측 시간 O, meta X
  TaskUI.renderWeekCard = (t) => {
    return TaskUI.renderTaskCard(t, {
      showRightTime: true,
      metaText: null
    });
  };

  /* -----------------------------
   * open helpers
   * ----------------------------- */
  TaskUI.openCreate = (preset = {}) => {
    if (window.TaskModal?.openCreate) {
      window.TaskModal.openCreate(preset);
      return true;
    }
    console.warn('[TaskUI] TaskModal not loaded');
    return false;
  };

  TaskUI.openDetail = (taskId) => {
    if (!taskId) return false;
    if (window.TaskModal?.openDetail) {
      window.TaskModal.openDetail(taskId);
      return true;
    }
    console.warn('[TaskUI] TaskModal not loaded');
    return false;
  };

  /* -----------------------------
   * 카드 클릭 위임 → 상세
   * ----------------------------- */
  document.addEventListener('click', (e) => {
    const card = e.target.closest('[data-task-id]');
    if (!card) return;

    // 내부 버튼/링크/폼 클릭 제외
    if (e.target.closest('button,a,input,textarea,select,label')) return;

    const id = card.getAttribute('data-task-id');
    if (!id) return;

    TaskUI.openDetail(id);
  });

  /* -----------------------------
   * 생성 버튼 트리거
   * ----------------------------- */
  document.addEventListener('click', (e) => {
    const btn = e.target.closest('[data-action="open-create"], #openCreateBtn');
    if (!btn) return;
    e.preventDefault();
    TaskUI.openCreate();
  });

  window.TaskUI = TaskUI;
})();
