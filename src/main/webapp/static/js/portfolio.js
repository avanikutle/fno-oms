/**
 * portfolio.js — Live positions and holdings display
 */
'use strict';

const Portfolio = {
  async load() {
    const posEl  = document.getElementById('positions-tbody');
    const holdEl = document.getElementById('holdings-tbody');
    const sumEl  = document.getElementById('portfolio-summary');

    if (posEl) posEl.innerHTML  = `<tr><td colspan="9">${spinner()}</td></tr>`;
    if (holdEl) holdEl.innerHTML = `<tr><td colspan="7">${spinner()}</td></tr>`;

    const [posRes, holdRes] = await Promise.allSettled([
      API.get('/api/portfolio/positions'),
      API.get('/api/portfolio/holdings'),
    ]);

    const positions = posRes.status === 'fulfilled' ? (posRes.value.data || []) : [];
    const holdings  = holdRes.status === 'fulfilled' ? (holdRes.value.data || []) : [];

    this.renderSummary(positions, sumEl);
    this.renderPositions(positions, posEl);
    this.renderHoldings(holdings, holdEl);
  },

  renderSummary(positions, el) {
    if (!el) return;
    const active  = positions.filter(p => p.quantity !== 0);
    const totalPnl = positions.reduce((s, p) => s + (parseFloat(p.pnl) || 0), 0);
    const realised = positions.reduce((s, p) => s + (parseFloat(p.realisedPnl) || 0), 0);
    const unreal   = positions.reduce((s, p) => s + (parseFloat(p.unrealisedPnl) || 0), 0);

    el.innerHTML = `
      <div class="stats-grid" style="margin-bottom:0">
        <div class="stat-card">
          <div class="stat-label">Active Positions</div>
          <div class="stat-value">${active.length}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Net P&amp;L</div>
          <div class="stat-value ${totalPnl >= 0 ? 'positive' : 'negative'}">
            ${totalPnl >= 0 ? '+' : ''}₹${fmt(totalPnl)}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Realised P&amp;L</div>
          <div class="stat-value ${realised >= 0 ? 'positive' : 'negative'}">
            ${realised >= 0 ? '+' : ''}₹${fmt(realised)}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Unrealised P&amp;L</div>
          <div class="stat-value ${unreal >= 0 ? 'positive' : 'negative'}">
            ${unreal >= 0 ? '+' : ''}₹${fmt(unreal)}
          </div>
        </div>
      </div>
    `;
  },

  renderPositions(positions, el) {
    if (!el) return;
    if (!positions.length) {
      el.innerHTML = `<tr><td colspan="9">${emptyState('📊', 'No positions', 'Your net positions will appear here')}</td></tr>`;
      return;
    }

    el.innerHTML = positions.map(p => {
      const pnl  = parseFloat(p.pnl) || 0;
      const qty  = parseInt(p.quantity) || 0;
      return `<tr>
        <td class="symbol">${escHtml(p.symbol)}</td>
        <td>${escHtml(p.exchange)}</td>
        <td>${escHtml(p.product)}</td>
        <td class="text-mono ${qty > 0 ? 'buy' : qty < 0 ? 'sell' : ''}">${qty >= 0 ? '+' : ''}${qty}</td>
        <td class="text-mono">${fmt(p.averagePrice)}</td>
        <td class="text-mono">${fmt(p.lastPrice)}</td>
        <td class="text-mono ${pnl >= 0 ? 'positive' : 'negative'}">
          ${pnl >= 0 ? '+' : ''}₹${fmt(pnl)}
        </td>
        <td class="text-mono">${fmt(p.realisedPnl)}</td>
        <td class="text-mono ${parseFloat(p.unrealisedPnl) >= 0 ? 'positive' : 'negative'}">
          ${fmt(p.unrealisedPnl)}
        </td>
      </tr>`;
    }).join('');
  },

  renderHoldings(holdings, el) {
    if (!el) return;
    if (!holdings.length) {
      el.innerHTML = `<tr><td colspan="7">${emptyState('📦', 'No holdings', 'Your CNC/delivery positions appear here')}</td></tr>`;
      return;
    }

    el.innerHTML = holdings.map(h => {
      const pnl = parseFloat(h.pnl) || 0;
      return `<tr>
        <td class="symbol">${escHtml(h.symbol)}</td>
        <td>${escHtml(h.exchange)}</td>
        <td class="text-mono">${h.quantity}</td>
        <td class="text-mono">${h.t1Quantity || 0}</td>
        <td class="text-mono">${fmt(h.averagePrice)}</td>
        <td class="text-mono">${fmt(h.lastPrice)}</td>
        <td class="text-mono ${pnl >= 0 ? 'positive' : 'negative'}">
          ${pnl >= 0 ? '+' : ''}₹${fmt(pnl)}
          <span style="font-size:10px;color:var(--text-muted)">
            (${fmt(h.pnlPct)}%)
          </span>
        </td>
      </tr>`;
    }).join('');
  },

  // Toggle between Positions and Holdings tabs (called from portfolio.jsp)
  showTab(tab) {
    const posPanel  = document.getElementById('tab-panel-positions');
    const holdPanel = document.getElementById('tab-panel-holdings');
    const posBtn    = document.getElementById('tab-positions');
    const holdBtn   = document.getElementById('tab-holdings');

    if (tab === 'positions') {
      if (posPanel)  posPanel.style.display  = 'block';
      if (holdPanel) holdPanel.style.display = 'none';
      if (posBtn)  { posBtn.className  = 'btn btn-primary'; }
      if (holdBtn) { holdBtn.className = 'btn btn-ghost'; }
    } else {
      if (posPanel)  posPanel.style.display  = 'none';
      if (holdPanel) holdPanel.style.display = 'block';
      if (posBtn)  { posBtn.className  = 'btn btn-ghost'; }
      if (holdBtn) { holdBtn.className = 'btn btn-primary'; }
    }
  },
};

