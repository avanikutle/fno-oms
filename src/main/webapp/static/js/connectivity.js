/**
 * connectivity.js — Broker connectivity check screen
 * Works with the static connectivity.jsp structure.
 * Updates #connectivity-tbody, #connectivity-banner, #connectivity-last-run.
 */
'use strict';

const Connectivity = {
  autoRefreshTimer: null,
  isRefreshing: false,

  load() {
    this.testAll();
  },

  async testAll() {
    if (this.isRefreshing) return;
    this.isRefreshing = true;

    const tbody  = document.getElementById('connectivity-tbody');
    const banner = document.getElementById('connectivity-banner');
    const icon   = document.getElementById('connectivity-icon');
    const title  = document.getElementById('connectivity-title');
    const sub    = document.getElementById('connectivity-subtitle');
    const lastRun= document.getElementById('connectivity-last-run');

    if (tbody) tbody.innerHTML = `<tr><td colspan="5" style="padding:24px;text-align:center">
      <div style="display:inline-flex;gap:8px;align-items:center;color:var(--text-muted)">
        <div class="spin"></div> Testing brokers…
      </div></td></tr>`;

    if (icon)  icon.textContent  = '🔄';
    if (title) title.textContent = 'Testing…';
    if (sub)   sub.textContent   = 'Pinging all configured broker endpoints';

    try {
      const res = await API.get('/api/connectivity');
      const brokers = Array.isArray(res.data) ? res.data : [];

      if (!brokers.length) {
        if (tbody) tbody.innerHTML = `<tr><td colspan="5" style="padding:32px;text-align:center;color:var(--text-muted)">
          No brokers configured —
          <a onclick="navigate('settings')" style="color:var(--amber);cursor:pointer">Go to Settings</a>
          to add your first broker
        </td></tr>`;
        if (icon)  icon.textContent  = '🔌';
        if (title) title.textContent = 'No brokers configured';
        if (sub)   sub.textContent   = 'Add a broker in Settings to start testing';
      } else {
        const allOk = brokers.every(b => b.status === 'CONNECTED' || b.status === 'PASS');
        const anyOk = brokers.some(b  => b.status === 'CONNECTED' || b.status === 'PASS');

        if (icon)  icon.textContent  = allOk ? '✅' : anyOk ? '⚠️' : '❌';
        if (title) title.textContent = allOk ? 'All systems operational'
                                     : anyOk ? 'Partial connectivity'
                                             : 'Connection failed';
        if (sub) sub.textContent = `${brokers.length} broker(s) tested — ${
          brokers.filter(b => b.status === 'CONNECTED' || b.status === 'PASS').length} connected`;

        if (tbody) tbody.innerHTML = brokers.map(b => this.renderRow(b)).join('');
      }

      if (lastRun) lastRun.textContent = 'Last tested: ' + new Date().toLocaleTimeString('en-IN');

    } catch (e) {
      if (tbody) tbody.innerHTML = `<tr><td colspan="5" style="padding:24px;text-align:center;color:var(--text-red)">
        ⚠ Test failed: ${escHtml(e.message)}</td></tr>`;
      if (icon)  icon.textContent  = '⚠️';
      if (title) title.textContent = 'Test failed';
      if (sub)   sub.textContent   = e.message;
    } finally {
      this.isRefreshing = false;
    }
  },

  renderRow(b) {
    const ok = b.status === 'CONNECTED' || b.status === 'PASS';
    const latencyMs = b.latencyMs ?? -1;

    const statusBadge = ok
      ? `<span class="badge badge-green">● Connected</span>`
      : b.status === 'NOT_CONFIGURED'
        ? `<span class="badge badge-muted">⊘ Not configured</span>`
        : `<span class="badge badge-red">✗ ${escHtml(b.status || 'Failed')}</span>`;

    const latencyDisplay = latencyMs < 0 ? '—'
      : latencyMs < 300 ? `<span class="text-green">${latencyMs}ms ⚡</span>`
      : latencyMs < 800 ? `<span class="text-amber">${latencyMs}ms</span>`
      : `<span class="text-red">${latencyMs}ms 🐢</span>`;

    return `<tr>
      <td>
        <div style="font-weight:600">${escHtml(b.displayName || b.brokerType)}</div>
        <div style="font-size:11px;color:var(--text-muted)">${b.active ? '★ Active' : ''}</div>
      </td>
      <td style="font-family:monospace;font-size:12px;color:var(--text-muted)">
        ${escHtml(b.endpoint || 'api.mstock.trade/openapi/typea')}
      </td>
      <td>${statusBadge}</td>
      <td>${latencyDisplay}</td>
      <td style="font-size:11px;color:var(--text-muted)">${new Date().toLocaleTimeString('en-IN')}</td>
    </tr>
    ${b.error ? `<tr><td colspan="5" style="padding:4px 12px 8px;font-size:11px;color:var(--text-red)">
      ↳ ${escHtml(b.error)}</td></tr>` : ''}`;
  },

  toggleAutoRefresh(enabled) {
    if (this.autoRefreshTimer) { clearInterval(this.autoRefreshTimer); this.autoRefreshTimer = null; }
    if (enabled) {
      this.autoRefreshTimer = setInterval(() => this.testAll(), 30_000);
      Toast.info('Auto-refresh enabled', 'Brokers tested every 30s');
    }
  },
};
