/**
 * quotes.js — Live quote polling with 2s interval + animated ticker
 */
'use strict';

const Quotes = {
  symbols: ['NSE:NIFTY 50', 'NSE:NIFTY BANK', 'NSE:INDIA VIX'],
  watchlist: [],          // user-added option symbols
  pollTimer: null,
  prevPrices: {},         // for flash animation

  init() {
    this.renderWatchlistControls();
    this.startPolling();
  },

  stop() {
    if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null; }
  },

  startPolling() {
    this.stop();
    this.fetchAll();
    this.pollTimer = setInterval(() => this.fetchAll(), 2000);
  },

  allSymbols() {
    return [...new Set([...this.symbols, ...this.watchlist])];
  },

  async fetchAll() {
    const syms = this.allSymbols();
    if (!syms.length) return;
    try {
      const res = await API.get('/api/quote?symbols=' + encodeURIComponent(syms.join(',')) + '&persist=true');
      if (res.success && res.data) {
        this.updateTicker(res.data);
        this.updateWatchlistTable(res.data);
      }
    } catch (e) { /* silent — don't spam console on network hiccup */ }
  },

  // ── Ticker Bar ──────────────────────────────────────────────────
  updateTicker(data) {
    const track = document.getElementById('ticker-track');
    if (!track) return;

    const items = Object.entries(data).map(([key, q]) => {
      const chg   = q.changePct || 0;
      const dir   = chg >= 0 ? 'up' : 'down';
      const arrow = chg >= 0 ? '▲' : '▼';
      return `
        <div class="ticker-item" onclick="Quotes.addToWatchlist('${key}')">
          <span class="ticker-symbol">${q.symbol || key.split(':')[1]}</span>
          <span class="ticker-price" id="tick-${key.replace(/[^a-z0-9]/gi,'_')}">${fmt(q.ltp)}</span>
          <span class="ticker-change ${dir}">${arrow}${fmt(Math.abs(chg))}%</span>
        </div>`;
    }).join('');

    // Duplicate for infinite scroll
    track.innerHTML = items + items;
  },

  // ── Watchlist Table ─────────────────────────────────────────────
  updateWatchlistTable(data) {
    const tbody = document.getElementById('watchlist-tbody');
    if (!tbody) return;

    const rows = Object.entries(data).map(([key, q]) => {
      const chg     = q.change    || 0;
      const chgPct  = q.changePct || 0;
      const dir     = chg >= 0 ? 'positive' : 'negative';
      const prev    = this.prevPrices[key];
      let flashClass = '';
      if (prev != null) {
        if (q.ltp > prev) flashClass = 'flash-up';
        else if (q.ltp < prev) flashClass = 'flash-down';
      }
      this.prevPrices[key] = q.ltp;

      return `<tr class="${flashClass}">
        <td class="symbol">${escHtml(q.symbol || key.split(':')[1])}</td>
        <td>${escHtml(q.exchange)}</td>
        <td class="text-mono">${fmt(q.ltp)}</td>
        <td class="text-mono">${fmt(q.open)}</td>
        <td class="text-mono">${fmt(q.high)}</td>
        <td class="text-mono">${fmt(q.low)}</td>
        <td class="text-mono ${dir}">${chg >= 0 ? '+' : ''}${fmt(chg)} (${chg >= 0 ? '+' : ''}${fmt(chgPct)}%)</td>
        <td class="text-mono">${q.oi ? Number(q.oi).toLocaleString('en-IN') : '—'}</td>
        <td>
          <div class="flex gap-2">
            <button class="btn btn-buy btn-sm" onclick="Orders.prefill('${key}','BUY')">B</button>
            <button class="btn btn-sell btn-sm" onclick="Orders.prefill('${key}','SELL')">S</button>
          </div>
        </td>
      </tr>`;
    }).join('');

    tbody.innerHTML = rows || `<tr><td colspan="9" style="text-align:center;color:var(--text-muted);padding:32px">
      No symbols in watchlist. Add below.
    </td></tr>`;
  },

  renderWatchlistControls() {
    const el = document.getElementById('watchlist-add');
    if (!el) return;
    el.innerHTML = `
      <div class="flex gap-2">
        <input class="form-control" id="wl-input"
          placeholder="e.g. NFO:NIFTY24JUL24000CE"
          style="max-width:320px"
          onkeydown="if(event.key==='Enter')Quotes.addFromInput()">
        <button class="btn btn-ghost" onclick="Quotes.addFromInput()">Add to Watchlist</button>
      </div>
    `;
  },

  addFromInput() {
    const input = document.getElementById('wl-input');
    const raw   = (input?.value || '').trim().toUpperCase();
    if (!raw) return;
    if (!raw.includes(':')) { Toast.error('Invalid format', 'Use EXCHANGE:SYMBOL format, e.g. NFO:NIFTY24JUL24000CE'); return; }
    this.addToWatchlist(raw);
    if (input) input.value = '';
  },

  addToWatchlist(symbol) {
    if (!this.watchlist.includes(symbol)) {
      this.watchlist.push(symbol);
      Toast.info('Added', symbol + ' added to watchlist');
    }
  },

  removeFromWatchlist(symbol) {
    this.watchlist = this.watchlist.filter(s => s !== symbol);
  },
};
