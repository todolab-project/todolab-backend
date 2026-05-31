// src/main/resources/static/js/task-api.js
(() => {
  const TaskApi = {};

  function buildQuery(params = {}) {
    const search = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        search.set(key, value);
      }
    });
    const qs = search.toString();
    return qs ? `?${qs}` : '';
  }

  function unwrap(body) {
    if (!body) throw new Error('응답이 비어있음');

    if (body.status && body.status !== 'success') {
      throw new Error(body.error?.message || body.message || 'API 실패');
    }

    if (body.success === false) {
      throw new Error(body.error?.message || body.message || 'API 실패');
    }

    return body.data ?? null;
  }

  async function request(path, options = {}) {
    const { headers = {}, ...rest } = options;

    const res = await fetch(path, {
      ...rest,
      headers: {
        'Accept': 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...headers
      }
    });

    const body = await res.json().catch(() => null);
    if (!res.ok) {
      const message = body?.error?.message || body?.message || `HTTP ${res.status}`;
      throw new Error(message);
    }

    return unwrap(body);
  }

  TaskApi.getTasks = (type, date, taskType) => {
    return request(`/api/tasks${buildQuery({ type, date, taskType })}`);
  };

  TaskApi.getGroupedTasks = (type, date, taskType) => {
    return request(`/api/tasks/grouped${buildQuery({ type, date, taskType })}`);
  };

  TaskApi.getInboxTasks = () => {
    return request('/api/tasks/inbox');
  };

  TaskApi.getTodayTasks = (date) => {
    return request(`/api/tasks/today${buildQuery({ date })}`);
  };

  TaskApi.getDoneTasks = (date) => {
    return request(`/api/tasks/done${buildQuery({ date })}`);
  };

  TaskApi.moveToToday = (id, date) => {
    return request(`/api/tasks/${encodeURIComponent(id)}/today${buildQuery({ date })}`, {
      method: 'PATCH',
      headers: { 'X-Requested-With': 'fetch' }
    });
  };

  TaskApi.completeTask = (id) => {
    return request(`/api/tasks/${encodeURIComponent(id)}/done`, {
      method: 'PATCH',
      headers: { 'X-Requested-With': 'fetch' }
    });
  };

  TaskApi.carryOver = (id, date) => {
    return request(`/api/tasks/${encodeURIComponent(id)}/carry-over${buildQuery({ date })}`, {
      method: 'PATCH',
      headers: { 'X-Requested-With': 'fetch' }
    });
  };

  TaskApi.connectDdayGoal = (id, ddayGoalId) => {
    return request(`/api/tasks/${encodeURIComponent(id)}/dday-goal${buildQuery({ ddayGoalId })}`, {
      method: 'PATCH',
      headers: { 'X-Requested-With': 'fetch' }
    });
  };

  TaskApi.disconnectDdayGoal = (id) => {
    return request(`/api/tasks/${encodeURIComponent(id)}/dday-goal`, {
      method: 'DELETE',
      headers: { 'X-Requested-With': 'fetch' }
    });
  };

  TaskApi.getWeekTasks = (date) => TaskApi.getTasks('WEEK', date);

  TaskApi.getMonthTasks = (yearMonth) => TaskApi.getTasks('MONTH', yearMonth);

  TaskApi.getUnscheduledTasks = () => {
    return request('/api/tasks/unscheduled');
  };

  TaskApi.getGroupedUnscheduledTasks = () => {
    return request('/api/tasks/unscheduled/grouped');
  };

  TaskApi.getTask = (id) => {
    return request(`/api/tasks/${encodeURIComponent(id)}`);
  };

  TaskApi.createTask = (payload) => {
    return request('/api/tasks', {
      method: 'POST',
      headers: { 'X-Requested-With': 'fetch' },
      body: JSON.stringify(payload)
    });
  };

  TaskApi.updateTask = (id, payload) => {
    return request(`/api/tasks/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'X-Requested-With': 'fetch' },
      body: JSON.stringify(payload)
    });
  };

  TaskApi.deleteTask = (id) => {
    return request(`/api/tasks/${encodeURIComponent(id)}`, {
      method: 'DELETE',
      headers: { 'X-Requested-With': 'fetch' }
    });
  };

  window.TaskApi = TaskApi;
})();
