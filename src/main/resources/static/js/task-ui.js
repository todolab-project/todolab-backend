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

  TaskUI.formatDdayLabel = (daysLeft) => {
    if (daysLeft === null || daysLeft === undefined || daysLeft === '') return null;
    const n = Number(daysLeft);
    if (Number.isNaN(n)) return null;
    if (n === 0) return 'D-Day';
    if (n > 0) return `D-${n}`;
    return `D+${Math.abs(n)}`;
  };

  TaskUI.formatDdayMeta = (task) => {
    if (!task?.ddayGoalTitle) return null;
    const label = TaskUI.formatDdayLabel(task.ddayDaysLeft);
    return label ? `목표 · ${task.ddayGoalTitle} ${label}` : `목표 · ${task.ddayGoalTitle}`;
  };

  TaskUI.deferReasons = [
    { value: 'TOO_BIG', label: '너무 큼' },
    { value: 'NOT_NEEDED_NOW', label: '지금 필요 없음' },
    { value: 'AVOIDING', label: '하기 싫음' },
    { value: 'NO_DEADLINE', label: '마감 없음' },
    { value: 'WAITING_OTHER', label: '다른 사람 대기' },
    { value: 'ETC', label: '기타' }
  ];

  TaskUI.formatCarryOverMeta = (task) => {
    const n = Number(task?.carryOverCount || 0);
    if (!Number.isFinite(n) || n <= 0) return null;
    if (task?.staleCarryOver || n >= 3) {
      const reason = (task?.deferReasonLabel || '').trim();
      return reason ? `${n}회 이월 · ${reason}` : `${n}회 이월 · 다시 정리 필요`;
    }
    return `${n}회 이월`;
  };

  TaskUI.joinMeta = (...items) => {
    return items
      .map(v => (v === null || v === undefined ? '' : String(v).trim()))
      .filter(Boolean)
      .join(' · ');
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
    const staleCarryOver = Boolean(task?.staleCarryOver || Number(task?.carryOverCount || 0) >= 3);

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
                 class="check-box task-complete-action"
                 data-action="complete-task"
                 data-task-id="${TaskUI.escapeHtml(task.id)}"
                 aria-label="완료 처리">✓</button>`
      : `<div class="check-box">✓</div>`;

    const deferReasonHtml = options.deferReasonAction
      ? `<label class="sr-only" for="defer-reason-${TaskUI.escapeHtml(task.id)}">미룬 이유</label>
         <select id="defer-reason-${TaskUI.escapeHtml(task.id)}"
                 class="task-action-select"
                 data-action="set-defer-reason"
                 data-task-id="${TaskUI.escapeHtml(task.id)}">
           <option value="">미룬 이유 선택</option>
           ${TaskUI.deferReasons.map(reason => `
             <option value="${TaskUI.escapeHtml(reason.value)}" ${task.deferReason === reason.value ? 'selected' : ''}>
               ${TaskUI.escapeHtml(reason.label)}
             </option>`).join('')}
         </select>`
      : '';

    const carryOverHtml = options.carryOverAction
      ? `<button type="button"
                 class="task-secondary-action"
                 data-action="carry-over-task"
                 data-task-id="${TaskUI.escapeHtml(task.id)}">
           내일로
         </button>`
      : '';

    const actionsHtml = (options.carryOverAction || options.deferReasonAction)
      ? `<div class="task-actions">
           <div class="task-actions-meta">
             ${staleCarryOver ? '다시 정리 필요' : '오늘 못 하면'}
           </div>
           <div class="task-actions-controls">
             ${deferReasonHtml}
           </div>
           ${carryOverHtml ? `<div class="task-actions-carry">${carryOverHtml}</div>` : ''}
         </div>`
      : '';

    return `
<div class="task-card task-card-clickable ${staleCarryOver ? 'task-card-stale' : ''}"
     data-task-id="${TaskUI.escapeHtml(task.id)}">
  <div class="task-row">
    <div class="task-left-bar" style="background:${barColor};"></div>
    ${checkHtml}

    <div class="min-w-0 flex-1">
      <div class="flex items-center gap-2 min-w-0">
        <div class="task-title truncate">${title}</div>
        ${cat ? `<span class="task-badge">${cat}</span>` : ``}
      </div>

      ${metaText ? `<div class="task-meta mt-1">${metaText}</div>` : ``}

      ${showDesc && desc ? `<div class="task-desc mt-2 whitespace-pre-wrap break-words line-clamp-3">${desc}</div>` : ``}
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
            class="rounded-lg border border-slate-200 bg-white px-3 py-2 text-[12px] font-extrabold text-slate-700 hover:bg-slate-50"
            data-action="move-to-today"
            data-task-id="${TaskUI.escapeHtml(t?.id)}">
      오늘 할 일로
    </button>
  </div>
</div>`.trim();
  };

  // ✅ Today: 우측 시간 O, meta X
  TaskUI.renderTodayCard = (t) => {
    const staleCarryOver = Boolean(t?.staleCarryOver || Number(t?.carryOverCount || 0) >= 3);
    return TaskUI.renderTaskCard(t, {
      showRightTime: true,
      metaText: TaskUI.joinMeta(TaskUI.formatDdayMeta(t), TaskUI.formatCarryOverMeta(t)),
      barColor: staleCarryOver ? 'rgba(245, 158, 11, 0.75)' : null,
      completeAction: true,
      carryOverAction: true,
      deferReasonAction: staleCarryOver
    });
  };

  TaskUI.renderDoneCard = (t, options = {}) => {
    const completedTime = TaskUI.toTimeHM(t?.completedAt);
    const meta = completedTime ? `완료 · ${completedTime}` : '완료';

    const base = TaskUI.renderTaskCard(t, {
      showRightTime: false,
      metaText: meta,
      barColor: 'rgba(16, 185, 129, 0.55)'
    });

    if (!options.reopenAction) {
      return base;
    }

    return `
<div class="done-task-item" data-done-task-id="${TaskUI.escapeHtml(t?.id)}">
  ${base}
  <div class="done-actions">
    <button type="button"
            class="task-secondary-action"
            data-action="reopen-today-task"
            data-task-id="${TaskUI.escapeHtml(t?.id)}">
      되돌리기
    </button>
  </div>
</div>`.trim();
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
