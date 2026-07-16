/**
 * strategies.js — Manages Options Lookup, Strategy Watchlist, and Active Strategies
 */
'use strict';

const Strategies = {
  watchlist: [],
  debounceTimer: null,

  init() {
    this.loadWatchlist();
    this.loadActiveStrategies();
  },

  searchOptions(query) {
    if (this.debounceTimer) clearTimeout(this.debounceTimer);
    if (!query || query.length < 3) {
      document.getElementById('opt-search-results').style.display = 'none';
      return;
    }
    
    this.debounceTimer = setTimeout(async () => {
      try {
        const res = await API.get('/api/strategies/search?q=' + encodeURIComponent(query));
        if (res.success && res.data) {
          const resultsDiv = document.getElementById('opt-search-results');
          if (res.data.length === 0) {
            resultsDiv.innerHTML = '<div style="padding:8px 12px;color:var(--text-muted)">No options found</div>';
          } else {
            resultsDiv.innerHTML = res.data.map(opt => `
              <div style="padding:8px 12px;cursor:pointer;border-bottom:1px solid var(--border);" 
                   hover="background:var(--bg-hover);" 
                   onclick="Strategies.addToWatchlist('${opt.symbol}')">
                <div style="font-weight:600;font-size:13px">${opt.symbol}</div>
              </div>
            `).join('');
          }
          resultsDiv.style.display = 'block';
        }
      } catch (e) { console.error(e); }
    }, 300);
  },

  async addToWatchlist(symbol) {
    document.getElementById('opt-search-results').style.display = 'none';
    document.getElementById('opt-search-input').value = '';
    
    try {
      const res = await API.post('/api/strategies/watchlist', { symbol });
      if (res.success) {
        toast.success(`Added ${symbol} to watchlist & subscribed!`);
        this.loadWatchlist();
      }
    } catch (e) {
      toast.error('Failed to add to watchlist');
    }
  },

  async loadWatchlist() {
    try {
      const res = await API.get('/api/strategies/watchlist');
      if (res.success && res.data) {
        this.watchlist = res.data.split(',').filter(s => s.trim().length > 0);
        this.renderWatchlist();
      } else {
        this.watchlist = [];
        this.renderWatchlist();
      }
    } catch (e) { console.error(e); }
  },

  renderWatchlist() {
    const tbody = document.getElementById('strategy-watchlist-tbody');
    if (!this.watchlist.length) {
      tbody.innerHTML = '<tr><td colspan="2" style="text-align:center;color:var(--text-muted)">Search and add options above</td></tr>';
      return;
    }

    tbody.innerHTML = this.watchlist.map(sym => `
      <tr>
        <td style="font-family:monospace;font-weight:600;">${sym}</td>
        <td>
          <button class="btn btn-primary" style="padding:4px 8px;font-size:11px;" onclick="Strategies.openConfigForm('${sym}')">Configure</button>
        </td>
      </tr>
    `).join('');
  },

  openConfigForm(symbol) {
    document.getElementById('strategy-config-form').style.display = 'block';
    document.getElementById('strat-symbol').value = symbol;
    document.getElementById('strat-symbol-label').innerText = symbol;
    document.getElementById('strat-entry').value = '';
    document.getElementById('strat-target').value = '';
    document.getElementById('strat-sl').value = '';
    document.getElementById('strat-qty').value = '15'; // Default NIFTY qty
  },

  async submitStrategy() {
    const payload = {
      symbol: document.getElementById('strat-symbol').value,
      entry_price: parseFloat(document.getElementById('strat-entry').value),
      target_price: parseFloat(document.getElementById('strat-target').value),
      stop_loss: parseFloat(document.getElementById('strat-sl').value),
      quantity: parseInt(document.getElementById('strat-qty').value)
    };

    try {
      const res = await API.post('/api/strategies/add', payload);
      if (res.success) {
        toast.success(`Strategy activated for ${payload.symbol}`);
        document.getElementById('strategy-config-form').style.display = 'none';
        this.loadActiveStrategies();
      } else {
        toast.error(res.message || 'Failed to activate');
      }
    } catch (e) {
      toast.error('Error submitting strategy');
    }
  },

  async loadActiveStrategies() {
    try {
      const res = await API.get('/api/strategies');
      const tbody = document.getElementById('active-strategies-tbody');
      
      if (res.success && res.data && res.data.length > 0) {
        tbody.innerHTML = res.data.map(strat => `
          <tr>
            <td style="font-family:monospace;font-weight:600;">${strat.symbol}</td>
            <td>${strat.entryPrice}</td>
            <td>${strat.targetPrice}</td>
            <td>${strat.stopLossPrice}</td>
            <td><span class="badge badge-green">Monitoring</span></td>
            <td>
              <button class="btn btn-ghost" style="padding:4px 8px;font-size:11px;" onclick="Strategies.resetState('${strat.symbol}')">↻ Reset</button>
            </td>
          </tr>
        `).join('');
      } else {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--text-muted)">No active strategies</td></tr>';
      }
    } catch (e) { console.error(e); }
  },

  async resetState(symbol) {
    try {
      const res = await API.post('/api/strategies/reset', { symbol });
      if (res.success) {
        Toast.success(`Reset entry/exit state for ${symbol}`);
      } else {
        Toast.error('Failed to reset');
      }
    } catch(e) {
      Toast.error('Error resetting');
    }
  }
};
