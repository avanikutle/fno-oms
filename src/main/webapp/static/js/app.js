/**
 * app.js — Core SPA router, navigation, toast system, global state
 */
'use strict';

// ── Global App State ─────────────────────────────────────────────
window.App = {
  currentPage:   'dashboard',
  activeBroker:  null,
  brokerConnected: false,
};

// ── Page Navigation ───────────────────────────────────────────────
function navigate(page) {
  document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

  const section = document.getElementById('page-' + page);
  const navItem = document.querySelector('[data-page="' + page + '"]');
  if (section) section.classList.add('active');
  if (navItem) navItem.classList.add('active');

  App.currentPage = page;

  // Trigger page-specific load
  switch (page) {
    case 'dashboard':    Dashboard.load();     break;
    case 'orders':       Orders.load();        break;
    case 'portfolio':    Portfolio.load();     break;
    case 'connectivity': Connectivity.load();  break;
    case 'settings':     Settings.load();      break;
  }
}

// ── Toast Notifications ───────────────────────────────────────────
const Toast = {
  container: null,

  init() {
    this.container = document.getElementById('toast-container');
  },

  show(type, title, message, durationMs = null) {
    if (durationMs === null) {
      durationMs = type === 'error' ? 12000 : 4000;
    }
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <div style="flex-grow: 1;">
        <div class="toast-title">${escHtml(title)}</div>
        ${message ? `<div class="toast-message">${escHtml(message)}</div>` : ''}
      </div>
      <button style="background:none;border:none;color:inherit;cursor:pointer;opacity:0.7;font-size:16px;" onclick="this.parentElement.remove()">×</button>
    `;
    this.container.appendChild(toast);
    setTimeout(() => {
      if(toast.parentElement) {
        toast.style.animation = 'toast-in 0.3s ease reverse';
        setTimeout(() => toast.remove(), 280);
      }
    }, durationMs);
  },

  success(title, msg) { this.show('success', title, msg); },
  error(title, msg)   { this.show('error',   title, msg); },
  info(title, msg)    { this.show('info',     title, msg); },
};

// ── Custom Modal ──────────────────────────────────────────────────
const Modal = {
  createOverlay() {
    const overlay = document.createElement('div');
    overlay.style.position = 'fixed';
    overlay.style.top = '0'; overlay.style.left = '0';
    overlay.style.width = '100vw'; overlay.style.height = '100vh';
    overlay.style.backgroundColor = 'rgba(0,0,0,0.5)';
    overlay.style.zIndex = '9999';
    overlay.style.display = 'flex';
    overlay.style.alignItems = 'center';
    overlay.style.justifyContent = 'center';
    return overlay;
  },
  createBox(contentHtml) {
    const box = document.createElement('div');
    box.style.background = 'var(--bg-card)';
    box.style.border = '1px solid var(--border)';
    box.style.borderRadius = 'var(--radius-md)';
    box.style.padding = '20px';
    box.style.minWidth = '300px';
    box.style.boxShadow = '0 10px 40px rgba(0,0,0,0.5)';
    box.innerHTML = contentHtml;
    return box;
  },
  confirm(title, message) {
    return new Promise(resolve => {
      const overlay = this.createOverlay();
      const box = this.createBox(`
        <h3 style="margin-top:0;margin-bottom:12px;font-size:16px">${escHtml(title)}</h3>
        <p style="margin-bottom:20px;font-size:14px;color:var(--text-secondary)">${escHtml(message)}</p>
        <div style="display:flex;justify-content:flex-end;gap:10px">
          <button id="modal-cancel" class="btn btn-ghost">Cancel</button>
          <button id="modal-ok" class="btn btn-danger">OK</button>
        </div>
      `);
      overlay.appendChild(box);
      document.body.appendChild(overlay);

      const close = (val) => { overlay.remove(); resolve(val); };
      box.querySelector('#modal-cancel').onclick = () => close(false);
      box.querySelector('#modal-ok').onclick = () => close(true);
      
      // Auto focus OK button so spacebar triggers it
      setTimeout(() => box.querySelector('#modal-ok').focus(), 50);
    });
  },
  prompt(title, message) {
    return new Promise(resolve => {
      const overlay = this.createOverlay();
      const box = this.createBox(`
        <h3 style="margin-top:0;margin-bottom:12px;font-size:16px">${escHtml(title)}</h3>
        <p style="margin-bottom:12px;font-size:14px;color:var(--text-secondary)">${escHtml(message)}</p>
        <input type="number" id="modal-input" class="form-input" style="width:100%;margin-bottom:20px" placeholder="Leave blank for MARKET">
        <div style="display:flex;justify-content:flex-end;gap:10px">
          <button id="modal-cancel" class="btn btn-ghost">Cancel</button>
          <button id="modal-ok" class="btn btn-primary">OK</button>
        </div>
      `);
      overlay.appendChild(box);
      document.body.appendChild(overlay);

      const input = box.querySelector('#modal-input');
      const close = (val) => { overlay.remove(); resolve(val); };
      
      box.querySelector('#modal-cancel').onclick = () => close(null);
      box.querySelector('#modal-ok').onclick = () => close(input.value);
      input.onkeydown = (e) => { if (e.key === 'Enter') close(input.value); };
      
      setTimeout(() => input.focus(), 50);
    });
  }
};

// ── API Helpers ───────────────────────────────────────────────────
const API = {
  base: document.querySelector('meta[name="ctx-path"]')?.content || '',

  async get(path) {
    const res = await fetch(this.base + path);
    return res.json();
  },

  async post(path, body) {
    const res = await fetch(this.base + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    return res.json();
  },

  async put(path, body) {
    const res = await fetch(this.base + path, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    return res.json();
  },

  async delete(path) {
    const res = await fetch(this.base + path, { method: 'DELETE' });
    return res.json();
  },
};

// ── Utilities ─────────────────────────────────────────────────────
function escHtml(str) {
  if (!str) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;')
                    .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function fmt(num, dec = 2) {
  if (num == null || isNaN(num)) return '—';
  return Number(num).toFixed(dec);
}

function fmtChange(val, pct) {
  if (val == null) return '';
  const sign = val >= 0 ? '+' : '';
  const cls  = val >= 0 ? 'text-green' : 'text-red';
  return `<span class="${cls}">${sign}${fmt(val)} (${sign}${fmt(pct)}%)</span>`;
}

function spinner(label = 'Loading…') {
  return `<div class="flex items-center gap-2 text-muted" style="padding:32px;justify-content:center">
    <div class="spin"></div><span>${label}</span></div>`;
}

function emptyState(icon, title, sub = '') {
  return `<div style="text-align:center;padding:48px;color:var(--text-muted)">
    <div style="font-size:36px;margin-bottom:12px">${icon}</div>
    <div style="font-weight:600;margin-bottom:4px">${title}</div>
    ${sub ? `<div style="font-size:12px">${sub}</div>` : ''}
  </div>`;
}

// ── Active Broker Status ──────────────────────────────────────────
async function loadBrokerStatus() {
  try {
    const res = await API.get('/api/connectivity');
    if (res.success && Array.isArray(res.data)) {
      const active = res.data.find(b => b.isActive);
      if (active) {
        App.activeBroker = active;
        App.brokerConnected = active.connected;
        const dot  = document.getElementById('broker-dot');
        const name = document.getElementById('broker-name');
        const stat = document.getElementById('broker-status');
        if (dot)  dot.className = 'broker-dot ' + (active.connected ? 'connected' : 'error');
        if (name) name.textContent = active.displayName || active.brokerType;
        if (stat) stat.textContent = active.connected ? 'Connected' : active.status;
      }
    }
  } catch (e) { /* sidebar just stays grey */ }
}

// ── Dashboard Module ──────────────────────────────────────────────
const Dashboard = {
  async load() {
    // Quick stats from order book + portfolio
    const statsEl = document.getElementById('dashboard-stats');
    if (statsEl) statsEl.innerHTML = spinner('Loading dashboard…');
    try {
      const [orders, positions] = await Promise.all([
        API.get('/api/orders/local'),
        API.get('/api/portfolio/positions').catch(() => ({ data: [] })),
      ]);
      this.render(orders.data || [], positions.data || [], statsEl);
    } catch(e) {
      if (statsEl) statsEl.innerHTML = emptyState('⚠️', 'Could not load dashboard', e.message);
    }
  },

  render(orders, positions, el) {
    const todayOrders  = orders.length;
    const openOrders   = orders.filter(o => o.status === 'OPEN' || o.status === 'TRIGGER PENDING').length;
    const totalPnl     = positions.reduce((s, p) => s + (p.pnl || 0), 0);
    const unrealised   = positions.reduce((s, p) => s + (p.unrealisedPnl || 0), 0);

    if (el) el.innerHTML = `
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">Today's Orders</div>
          <div class="stat-value">${todayOrders}</div>
          <div class="stat-change">${openOrders} open</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Net P&amp;L</div>
          <div class="stat-value ${totalPnl >= 0 ? 'positive' : 'negative'}">
            ₹${fmt(Math.abs(totalPnl))}
          </div>
          <div class="stat-change">${totalPnl >= 0 ? '▲' : '▼'} today</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Unrealised P&amp;L</div>
          <div class="stat-value ${unrealised >= 0 ? 'positive' : 'negative'}">
            ₹${fmt(Math.abs(unrealised))}
          </div>
          <div class="stat-change">Live MTM</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Active Positions</div>
          <div class="stat-value">${positions.filter(p => p.quantity !== 0).length}</div>
          <div class="stat-change">net positions</div>
        </div>
      </div>
    `;
  }
};

// ── Settings Module ───────────────────────────────────────────────
const Settings = {
  async load() {
    // Load existing broker cards into #settings-content
    const el = document.getElementById('settings-content');
    if (!el) return;
    el.innerHTML = spinner();
    try {
      const res = await API.get('/api/broker-config');
      this.renderCards(res.data || [], el);
    } catch(e) {
      el.innerHTML = emptyState('⚠️','Failed to load settings', e.message);
    }
    // Pre-fill token expiry to tonight 18:30
    const exp = document.getElementById('cfg-token-expiry');
    if (exp && !exp.value) {
      const d = new Date(); d.setHours(18,30,0,0);
      exp.value = d.toISOString().slice(0,16);
    }
  },

  renderCards(brokers, el) {
    if (!brokers.length) {
      el.innerHTML = emptyState('🔌','No brokers yet','Add one using the form below');
      return;
    }
    el.innerHTML = `<div class="connectivity-grid">
      ${brokers.map(b => `
        <div class="broker-card ${b.active ? 'status-connected' : ''}" id="broker-card-${b.id}">
          <div class="broker-card-header">
            <div>
              <div class="broker-name">${escHtml(b.displayName)}</div>
              <div class="broker-type-tag">${b.brokerType}</div>
            </div>
            <div class="flex gap-2">
              ${b.active
                ? '<span class="badge badge-green">Active</span>'
                : `<button class="btn btn-ghost btn-sm" onclick="Settings.activate(${b.id})">Set Active</button>`}
            </div>
          </div>
          <div class="connectivity-metric">
            <span class="metric-label">API Key</span>
            <span class="metric-value text-mono">${escHtml(b.apiKey)}</span>
          </div>
          <div class="connectivity-metric">
            <span class="metric-label">Token</span>
            <span class="metric-value text-mono">${escHtml(b.accessToken)}</span>
          </div>
          <div class="connectivity-metric">
            <span class="metric-label">Expires</span>
            <span class="metric-value">${b.tokenExpiry ? new Date(b.tokenExpiry).toLocaleString('en-IN') : '—'}</span>
          </div>
          <div class="flex gap-2 mt-4">
            <button class="btn btn-ghost btn-sm" onclick="Settings.showTokenForm(${b.id})">Update Token</button>
            <button class="btn btn-danger btn-sm" onclick="Settings.delete(${b.id})">Remove</button>
          </div>
        </div>`).join('')}
    </div>`;
  },

  async addBroker() {
    // Reads from settings.jsp static form fields (cfg-* IDs)
    const body = {
      brokerType:  document.getElementById('cfg-broker-type')?.value  || 'MSTOCK',
      displayName: document.getElementById('cfg-display-name')?.value || '',
      apiKey:      document.getElementById('cfg-api-key')?.value       || '',
      accessToken: document.getElementById('cfg-access-token')?.value  || '',
      tokenExpiry: document.getElementById('cfg-token-expiry')?.value  || '',
      clientId:    document.getElementById('cfg-client-id')?.value     || null,
    };
    if (!body.displayName || !body.apiKey || !body.accessToken) {
      Toast.error('Validation', 'Display name, API key, and access token are required');
      return;
    }
    // Convert local datetime string to ISO-8601
    if (body.tokenExpiry) body.tokenExpiry = new Date(body.tokenExpiry).toISOString();
    const res = await API.post('/api/broker-config', body);
    if (res.status === 'success') {
      Toast.success('Broker Added', body.displayName + ' configured successfully');
      this.clearForm();
      this.load();
    } else {
      Toast.error('Failed', res.message || 'Unknown error');
    }
  },

  clearForm() {
    ['cfg-display-name','cfg-api-key','cfg-access-token','cfg-client-id'].forEach(id => {
      const el = document.getElementById(id); if (el) el.value = '';
    });
    const exp = document.getElementById('cfg-token-expiry');
    if (exp) { const d = new Date(); d.setHours(18,30,0,0); exp.value = d.toISOString().slice(0,16); }
  },

  async activate(id) {
    const res = await API.put('/api/broker-config/' + id + '/activate', {});
    if (res.status === 'success') {
      Toast.success('Broker Activated', 'Active broker updated');
      this.load(); loadBrokerStatus();
    } else {
      Toast.error('Failed', res.message);
    }
  },

  showTokenForm(id) {
    const token = prompt('Paste new access token (from mStock portal):');
    if (!token) return;
    API.post('/api/auth/token', { configId: id, accessToken: token }).then(res => {
      if (res.status === 'success') { Toast.success('Token Updated', 'Valid till 18:30'); this.load(); }
      else Toast.error('Failed', res.message);
    });
  },

  async delete(id) {
    if (!confirm('Remove this broker configuration?')) return;
    const res = await API.delete('/api/broker-config/' + id);
    if (res.status === 'success') { Toast.info('Removed', 'Broker configuration deleted'); this.load(); }
    else Toast.error('Failed', res.message);
  }
};

// ── App Initialisation ────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  Toast.init();

  // Wire up nav
  document.querySelectorAll('.nav-item[data-page]').forEach(el => {
    el.addEventListener('click', () => navigate(el.dataset.page));
  });

  // Load broker status every 60s
  loadBrokerStatus();
  setInterval(loadBrokerStatus, 60_000);

  // Default page
  navigate('dashboard');
});
