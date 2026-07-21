/**
 * orders.js — Order placement form + live order book
 */
'use strict';

const Orders = {
  currentSide: 'BUY',
  refreshTimer: null,
  allOrders: [],
  allAlgos: [],
  currentTab: 'algos',
  watchlist: [],
  searchTimeout: null,

  load() {
    // Only render dynamic form if JSP static form is not present
    if (!document.getElementById('order-symbol')) {
      this.renderForm();
    } else {
      this.initJspForm();
    }
    this.initWatchlist();
    this.loadOrderBook();
  },

  // Initialise the static JSP form on first load
  initJspForm() {
    this.setSide('BUY');
    this.onTypeChange();
    this.setTab('algos');
  },

  // Called by JSP BUY/SELL buttons
  setSide(side) {
    this.currentSide = side;
    // Dynamic form buttons
    const buyTab  = document.getElementById('tab-buy');
    const sellTab = document.getElementById('tab-sell');
    if (buyTab)  buyTab.className  = 'order-tab' + (side === 'BUY'  ? ' active-buy'  : '');
    if (sellTab) sellTab.className = 'order-tab' + (side === 'SELL' ? ' active-sell' : '');
    // JSP form buttons
    const btnBuy  = document.getElementById('btn-buy');
    const btnSell = document.getElementById('btn-sell');
    if (btnBuy)  { btnBuy.className  = side === 'BUY'  ? 'btn btn-success' : 'btn btn-ghost'; }
    if (btnSell) { btnSell.className = side === 'SELL' ? 'btn btn-danger'  : 'btn btn-ghost'; }
  },

  // Called by JSP order-type select onchange
  onTypeChange() {
    const typeEl = document.getElementById('order-type') || document.getElementById('ord-order-type');
    const type   = typeEl?.value || 'MARKET';
    
    // Core fields
    const pf = document.getElementById('price-field');
    const tf = document.getElementById('trigger-field');
    
    // ALGO fields
    const targetF = document.getElementById('algo-target-field');
    const slF     = document.getElementById('algo-sl-field');
    const trailF  = document.getElementById('algo-trail-field');
    
    const showPrice   = type === 'LIMIT' || type === 'SL' || type === 'ALGO';
    const showTrigger = type === 'SL'    || type === 'SL-M';
    const isAlgo      = type === 'ALGO';

    if (pf) {
        pf.style.display = showPrice ? 'block' : 'none';
        const lbl = document.getElementById('lbl-price');
        if (lbl) lbl.textContent = isAlgo ? 'Entry Price' : 'Price';
    }
    if (tf) tf.style.display = showTrigger ? 'block' : 'none';
    
    if (targetF) targetF.style.display = isAlgo ? 'block' : 'none';
    if (slF) slF.style.display = isAlgo ? 'block' : 'none';
    if (trailF) trailF.style.display = isAlgo ? 'block' : 'none';
  },

  // Called by JSP Clear button
  resetForm() {
    ['order-symbol','order-qty','order-price','order-trigger','algo-target','algo-sl','algo-trail'].forEach(id => {
      const el = document.getElementById(id); if (el) el.value = '';
    });
    this.setSide('BUY');
    const ot = document.getElementById('order-type'); if (ot) ot.value = 'MARKET';
    this.onTypeChange();
  },

  // Called by JSP Place Order button — reads from static JSP form
  async placeOrder() {
    const side = this.currentSide;
    const orderType = document.getElementById('order-type')?.value || 'MARKET';
    
    if (orderType === 'ALGO') {
      const body = {
        exchange:        document.getElementById('order-exchange')?.value || 'NFO',
        symbol:          (document.getElementById('order-symbol')?.value  || '').trim().toUpperCase(),
        transaction_type: side,
        product:         document.getElementById('order-product')?.value  || 'MIS',
        quantity:        parseInt(document.getElementById('order-qty')?.value) || 0,
        entry_price:     parseFloat(document.getElementById('order-price')?.value) || 0,
        target_price:    parseFloat(document.getElementById('algo-target')?.value) || 0,
        stop_loss:       parseFloat(document.getElementById('algo-sl')?.value) || 0,
        trailing_sl_points: parseFloat(document.getElementById('algo-trail')?.value) || 0,
      };
      
      
      if (!body.symbol)       { Toast.error('Validation', 'Symbol is required'); return; }
      if (body.quantity <= 0) { Toast.error('Validation', 'Enter a valid quantity'); return; }
      if (body.entry_price <= 0) { Toast.error('Validation', 'Enter entry price'); return; }
      
      // Default Target & SL to 5% if not provided
      if (body.target_price <= 0) {
        body.target_price = side === 'BUY' ? body.entry_price * 1.05 : body.entry_price * 0.95;
      }
      if (body.stop_loss <= 0) {
        body.stop_loss = side === 'BUY' ? body.entry_price * 0.95 : body.entry_price * 1.05;
      }

      // Format to 2 decimal places to avoid floating point issues
      body.target_price = parseFloat(body.target_price.toFixed(2));
      body.stop_loss = parseFloat(body.stop_loss.toFixed(2));

      // Validation logic
      if (side === 'BUY') {
        if (body.target_price <= body.entry_price) {
          Toast.error('Validation', 'For BUY orders, Target Price must be greater than Entry Price');
          return;
        }
        if (body.stop_loss >= body.entry_price) {
          Toast.error('Validation', 'For BUY orders, Stop Loss must be less than Entry Price');
          return;
        }
      } else { // SELL
        if (body.target_price >= body.entry_price) {
          Toast.error('Validation', 'For SELL orders, Target Price must be less than Entry Price');
          return;
        }
        if (body.stop_loss <= body.entry_price) {
          Toast.error('Validation', 'For SELL orders, Stop Loss must be greater than Entry Price');
          return;
        }
      }
      
      try {
        const result = await API.post('/api/strategies/add', body);
        if (result.status === 'success') {
          Toast.success('Algo Strategy Placed ✓', `${side} ${body.quantity} ${body.symbol}`);
          this.resetForm();
          this.loadOrderBook();
        } else {
          Toast.error('Order Failed', result.message);
        }
      } catch(e) { Toast.error('Network Error', e.message); }
      
    } else {
      const body = {
        exchange:        document.getElementById('order-exchange')?.value || 'NFO',
        symbol:          (document.getElementById('order-symbol')?.value  || '').trim().toUpperCase(),
        transactionType: side,
        orderType:       orderType,
        product:         document.getElementById('order-product')?.value  || 'MIS',
        quantity:        parseInt(document.getElementById('order-qty')?.value) || 0,
        price:           parseFloat(document.getElementById('order-price')?.value)   || null,
        triggerPrice:    parseFloat(document.getElementById('order-trigger')?.value) || null,
        validity:        'DAY',
      };
      if (!body.symbol)       { Toast.error('Validation', 'Symbol is required'); return; }
      if (body.quantity <= 0) { Toast.error('Validation', 'Enter a valid quantity'); return; }
      try {
        const result = await API.post('/api/orders', body);
        if (result.status === 'success') {
          Toast.success('Order Placed ✓', `${side} ${body.quantity} ${body.symbol} | ID: ${result.data?.brokerOrderId}`);
          this.resetForm();
          this.loadOrderBook();
        } else {
          Toast.error('Order Failed', result.message);
        }
      } catch(e) { Toast.error('Network Error', e.message); }
    }
  },

  // --- Watchlist Logic ---

  initWatchlist() {
    const saved = localStorage.getItem('fno_watchlist');
    if (saved) {
      try { this.watchlist = JSON.parse(saved); } catch (e) { this.watchlist = []; }
    }
    this.renderWatchlist();
    this.refreshWatchlistPrices();

    const searchInput = document.getElementById('watchlist-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        clearTimeout(this.searchTimeout);
        const val = e.target.value.trim();
        this.searchTimeout = setTimeout(() => this.searchInstruments(val), 300);
      });
      searchInput.addEventListener('focus', (e) => {
        const val = e.target.value.trim();
        if (val.length === 0) {
           this.searchInstruments('');
        }
      });
    }
  },

  async searchInstruments(query) {
    const resBox = document.getElementById('watchlist-search-results');
    try {
      const result = await API.get('/api/instruments/search?q=' + encodeURIComponent(query));
      if (result.success && result.data.length > 0) {
        resBox.innerHTML = result.data.map(item => `
          <div style="padding:10px;border-bottom:1px solid var(--border);cursor:pointer;display:flex;justify-content:space-between;align-items:center" 
               onclick="Orders.addToWatchlist('${encodeURIComponent(JSON.stringify(item))}')">
            <div>
              <div style="font-weight:600">${item.symbol}</div>
              <div style="font-size:11px;color:var(--text-muted)">${item.expiry} | Strike: ${item.strike} ${item.type}</div>
            </div>
            <span class="nav-icon" style="color:var(--amber)">+</span>
          </div>
        `).join('');
        resBox.style.display = 'block';
      } else {
        resBox.innerHTML = `<div style="padding:10px;color:var(--text-muted)">No results found.</div>`;
        resBox.style.display = 'block';
      }
    } catch (e) {
      console.error(e);
    }
  },

  clearWatchlistSearch() {
    const s = document.getElementById('watchlist-search');
    if (s) s.value = '';
    const resBox = document.getElementById('watchlist-search-results');
    if (resBox) resBox.style.display = 'none';
  },

  addToWatchlist(itemStr) {
    if (this.watchlist.length >= 6) {
      Toast.error('Watchlist Full', 'You can only add up to 6 instruments to the watchlist.');
      return;
    }
    try {
      const item = JSON.parse(decodeURIComponent(itemStr));
      if (!this.watchlist.find(x => x.token === item.token)) {
        item.ltp = 0; // initialize
        this.watchlist.push(item);
        this.saveWatchlist();
        this.renderWatchlist();
        this.refreshWatchlistPrices();
      }
      this.clearWatchlistSearch();
    } catch(e) { console.error(e); }
  },

  removeFromWatchlist(token) {
    this.watchlist = this.watchlist.filter(x => x.token !== token);
    this.saveWatchlist();
    this.renderWatchlist();
  },

  saveWatchlist() {
    localStorage.setItem('fno_watchlist', JSON.stringify(this.watchlist));
  },

  renderWatchlist() {
    const tbody = document.getElementById('watchlist-tbody');
    if (!tbody) return;
    
    if (this.watchlist.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="padding:16px;text-align:center;color:var(--text-muted)">Watchlist is empty. Search to add up to 6 instruments.</td></tr>`;
      return;
    }

    tbody.innerHTML = this.watchlist.map(item => `
      <tr style="cursor:pointer" onclick="Orders.prefillFromWatchlist('${item.symbol}', '${item.exchange}', ${item.ltp})">
        <td style="font-weight:600;color:var(--text-primary)">${escHtml(item.symbol)}</td>
        <td>${item.expiry}</td>
        <td>${item.strike}</td>
        <td><span class="badge ${item.type === 'CE' ? 'badge-green' : 'badge-red'}">${item.type}</span></td>
        <td style="font-family:var(--font-mono);font-weight:600" id="wl-price-${item.token}">${item.ltp > 0 ? fmt(item.ltp) : '—'}</td>
        <td>
          <button class="btn btn-icon btn-ghost" onclick="event.stopPropagation(); Orders.removeFromWatchlist('${item.token}')">×</button>
        </td>
      </tr>
    `).join('');
  },

  async refreshWatchlistPrices() {
    if (this.watchlist.length === 0) return;
    
    const symbols = this.watchlist.map(i => i.exchange + ':' + i.symbol).join(',');
    try {
      const res = await API.get('/api/quote?symbols=' + encodeURIComponent(symbols));
      if (res.success && res.data) {
        this.watchlist.forEach(item => {
          const key = item.exchange + ':' + item.symbol;
          if (res.data[key]) {
            item.ltp = res.data[key].ltp;
          }
        });
        this.saveWatchlist();
        this.renderWatchlist();
      }
    } catch(e) {
      console.error('Failed to fetch watchlist prices', e);
    }
  },

  prefillFromWatchlist(symbol, exchange, ltp) {
    const ex = document.getElementById('order-exchange'); if (ex) ex.value = exchange || 'NFO';
    const sy = document.getElementById('order-symbol');   if (sy) sy.value = symbol;
    
    // For ALGO type, we could optionally set entry price = LTP
    const typeEl = document.getElementById('order-type');
    if (typeEl && typeEl.value === 'ALGO' && ltp > 0) {
       const pr = document.getElementById('order-price');
       if (pr) pr.value = ltp;
    }
    
    // Scroll down to the order panel slightly for UX
    const formPanel = document.getElementById('order-form-panel');
    if (formPanel) {
        formPanel.scrollIntoView({ behavior: 'smooth', block: 'center' });
        // briefly highlight it
        formPanel.style.transition = 'box-shadow 0.3s';
        formPanel.style.boxShadow = '0 0 0 4px var(--amber-glow)';
        setTimeout(() => { formPanel.style.boxShadow = 'none'; }, 1000);
    }
  },

  // --- End Watchlist Logic ---

  prefill(symbol, side) {
    navigate('orders');
    setTimeout(() => {
      const parts = symbol.split(':');
      // JSP form
      const ex = document.getElementById('order-exchange'); if (ex) ex.value = parts[0] || 'NFO';
      const sy = document.getElementById('order-symbol');   if (sy) sy.value = parts[1] || symbol;
      // Dynamic form
      const dex = document.getElementById('ord-exchange'); if (dex) dex.value = parts[0] || 'NFO';
      const dsy = document.getElementById('ord-symbol');   if (dsy) dsy.value = parts[1] || symbol;
      this.setSide(side);
    }, 50);
  },

  renderForm() {
    const formEl = document.getElementById('order-form-panel');
    if (!formEl) return;
    formEl.innerHTML = `
      <!-- Side Toggle -->
      <div class="order-type-tabs">
        <button class="order-tab active-buy" id="tab-buy"  onclick="Orders.setSide('BUY')">BUY</button>
        <button class="order-tab"            id="tab-sell" onclick="Orders.setSide('SELL')">SELL</button>
      </div>

      <!-- Symbol -->
      <div class="form-group">
        <label class="form-label">Exchange</label>
        <select class="form-control" id="ord-exchange">
          <option value="NFO">NFO</option>
          <option value="NSE">NSE</option>
          <option value="BSE">BSE</option>
          <option value="MCX">MCX</option>
          <option value="BFO">BFO</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">Symbol (Trading Symbol)</label>
        <input class="form-control" id="ord-symbol" placeholder="e.g. NIFTY24JUL24000CE">
      </div>

      <!-- Order Type -->
      <div class="form-group">
        <label class="form-label">Order Type</label>
        <select class="form-control" id="ord-order-type" onchange="Orders.onOrderTypeChange()">
          <option value="MARKET">MARKET</option>
          <option value="LIMIT">LIMIT</option>
          <option value="SL">SL (Stop-Loss Limit)</option>
          <option value="SL-M">SL-M (Stop-Loss Market)</option>
        </select>
      </div>

      <!-- Price fields (conditionally shown) -->
      <div class="form-group" id="price-group" style="display:none">
        <label class="form-label">Price (₹)</label>
        <input class="form-control" id="ord-price" type="number" step="0.05" placeholder="0.00">
      </div>
      <div class="form-group" id="trigger-group" style="display:none">
        <label class="form-label">Trigger Price (₹)</label>
        <input class="form-control" id="ord-trigger" type="number" step="0.05" placeholder="0.00">
      </div>

      <!-- Qty & Product -->
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
        <div class="form-group">
          <label class="form-label">Quantity (Lots × Lot Size)</label>
          <input class="form-control" id="ord-quantity" type="number" min="1" placeholder="50">
        </div>
        <div class="form-group">
          <label class="form-label">Product</label>
          <select class="form-control" id="ord-product">
            <option value="NRML">NRML (Normal / Overnight)</option>
            <option value="MIS">MIS (Intraday)</option>
            <option value="CNC">CNC (Delivery)</option>
          </select>
        </div>
      </div>

      <!-- Validity -->
      <div class="form-group">
        <label class="form-label">Validity</label>
        <select class="form-control" id="ord-validity">
          <option value="DAY">DAY</option>
          <option value="IOC">IOC (Immediate or Cancel)</option>
        </select>
      </div>

      <!-- Tag -->
      <div class="form-group">
        <label class="form-label">Tag (optional)</label>
        <input class="form-control" id="ord-tag" placeholder="e.g. hedge, straddle-entry">
      </div>

      <!-- Submit -->
      <button class="btn btn-buy w-full" id="ord-submit" onclick="Orders.submit()"
              style="margin-top:8px;padding:12px;font-size:14px">
        Place Order
      </button>
      <div id="ord-result" style="margin-top:12px"></div>
    `;
  },

  onOrderTypeChange() {
    const type    = document.getElementById('ord-order-type').value;
    const priceGr = document.getElementById('price-group');
    const trigGr  = document.getElementById('trigger-group');
    if (priceGr) priceGr.style.display = (type === 'LIMIT' || type === 'SL') ? 'block' : 'none';
    if (trigGr)  trigGr.style.display  = (type === 'SL' || type === 'SL-M')  ? 'block' : 'none';
  },

  async submit() {
    const btn  = document.getElementById('ord-submit');
    const res  = document.getElementById('ord-result');
    const side = this.currentSide;

    const body = {
      exchange:        document.getElementById('ord-exchange').value,
      symbol:          (document.getElementById('ord-symbol').value || '').trim().toUpperCase(),
      transactionType: side,
      orderType:       document.getElementById('ord-order-type').value,
      product:         document.getElementById('ord-product').value,
      validity:        document.getElementById('ord-validity').value,
      quantity:        parseInt(document.getElementById('ord-quantity').value) || 0,
      tag:             document.getElementById('ord-tag').value || null,
    };

    const priceEl   = document.getElementById('ord-price');
    const trigEl    = document.getElementById('ord-trigger');
    if (priceEl?.closest('[style*="block"]')) body.price       = parseFloat(priceEl.value) || null;
    if (trigEl?.closest('[style*="block"]'))  body.triggerPrice = parseFloat(trigEl.value) || null;

    if (!body.symbol)        { Toast.error('Validation', 'Symbol is required'); return; }
    if (body.quantity <= 0)  { Toast.error('Validation', 'Enter a valid quantity'); return; }

    btn.disabled = true;
    btn.innerHTML = `<div class="spin"></div> Placing…`;

    try {
      const result = await API.post('/api/orders', body);
      if (result.success) {
        Toast.success('Order Placed ✓',
          `${side} ${body.quantity} ${body.symbol} @ ${body.orderType} | ID: ${result.data.brokerOrderId}`);
        res.innerHTML = `<div class="badge badge-green" style="padding:8px 16px;font-size:12px">
          ✓ Order ID: ${escHtml(result.data.brokerOrderId)}</div>`;
        this.loadOrderBook();
      } else {
        Toast.error('Order Failed', result.message);
        res.innerHTML = `<div class="badge badge-red" style="padding:8px 16px;font-size:12px">✗ ${escHtml(result.message)}</div>`;
      }
    } catch (e) {
      Toast.error('Network Error', e.message);
    } finally {
      btn.disabled = false;
      btn.textContent = 'Place Order';
    }
  },

  async loadOrderBook() {
    const el = document.getElementById('orderbook-tbody');
    if (!el) return;

    try {
      const [resOrders, resAlgos] = await Promise.all([
        API.get('/api/orders').catch(() => ({ data: [] })),
        API.get('/api/strategies').catch(() => ({ data: [] }))
      ]);
      this.allOrders = resOrders.data || [];
      this.allAlgos = resAlgos.data || [];
      this.renderOrders();
    } catch(e) {
      if (el) el.innerHTML = `<tr><td colspan="9" style="color:var(--red);padding:16px">${escHtml(e.message)}</td></tr>`;
    }
  },

  setTab(tab) {
    this.currentTab = tab;
    ['broker', 'algos'].forEach(t => {
      const btn = document.getElementById('tab-' + t);
      if (btn) {
        btn.className = 'btn btn-sm ' + (tab === t ? '' : 'btn-ghost');
        btn.style.background = tab === t ? 'var(--amber)' : 'transparent';
        btn.style.color = tab === t ? '#000' : 'var(--text-muted)';
      }
    });
    
    // Update headers based on tab
    const thead = document.getElementById('orderbook-thead');
    if (thead) {
        if (tab === 'broker') {
            thead.innerHTML = `
            <tr>
              <th>Symbol</th><th>Side</th><th>Type</th><th>Qty</th><th>Price</th><th>Avg</th><th>Status</th><th>Order ID</th><th>Action</th>
            </tr>`;
        } else {
            thead.innerHTML = `
            <tr>
              <th>Symbol</th><th>Side</th><th>Entry Cond.</th><th>Target</th><th>SL</th><th>Trail</th><th>Status</th><th>State</th><th>Action</th>
            </tr>`;
        }
    }
    
    this.renderOrders();
  },

  renderOrders() {
    const el = document.getElementById('orderbook-tbody');
    if (!el) return;

    if (this.currentTab === 'broker') {
        if (!this.allOrders.length) {
          el.innerHTML = `<tr><td colspan="9">${emptyState('📋', 'No broker orders found', 'Place an order using the form')}</td></tr>`;
          return;
        }
        el.innerHTML = this.allOrders.map(o => {
            const side  = (o.transactionType || '').toUpperCase();
        const stCls = ({'COMPLETE':'complete','O-Completed':'complete','OPEN':'open','O-Pending':'open','REJECTED':'rejected','CANCELLED':'cancelled','O-Cancelled':'cancelled'}[o.status] || '');
        const isCancellable = (o.status && (o.status.toUpperCase().includes('OPEN') || o.status.toUpperCase().includes('PENDING') || o.status.toUpperCase().includes('WAITING')));
        const isCloseable   = (o.status && (o.status.toUpperCase().includes('COMPLETE') || o.status.toUpperCase().includes('EXECUTED')));

        let actions = '—';
        if (isCancellable) {
          actions = `<button class="btn btn-danger btn-sm" onclick="Orders.cancel('${escHtml(o.brokerOrderId)}')">Cancel</button>`;
        } else if (isCloseable) {
          // Pass necessary data to close the position
          const closeData = {
            exchange: o.exchange,
            symbol: o.symbol,
            side: side === 'BUY' ? 'SELL' : 'BUY',
            quantity: o.quantity,
            product: o.product
          };
          actions = `<button class="btn btn-warning btn-sm" onclick="Orders.close('${encodeURIComponent(JSON.stringify(closeData))}')">Close</button>`;
        }

        return `<tr>
          <td class="symbol">${escHtml(o.symbol)}</td>
          <td class="${side === 'BUY' ? 'buy' : 'sell'}">${side}</td>
          <td>${escHtml(o.orderType)}</td>
          <td class="text-mono">${o.quantity}</td>
          <td class="text-mono">${o.price ? fmt(o.price) : 'MKT'}</td>
          <td class="text-mono">${o.averagePrice ? fmt(o.averagePrice) : '—'}</td>
          <td><span class="badge ${stCls === 'complete' ? 'badge-green' : stCls === 'open' ? 'badge-amber' : 'badge-muted'}">${escHtml(o.status)}</span></td>
          <td class="text-mono" style="font-size:11px">${escHtml(o.brokerOrderId || '—')}</td>
          <td>${actions}</td>
        </tr>`;
      }).join('');
    } else {
        // ALGO Tabs
        const filtered = this.allAlgos;
        
        if (!filtered.length) {
          el.innerHTML = `<tr><td colspan="9">${emptyState('⚙️', 'No algo strategies found', 'Place an algo order using the form')}</td></tr>`;
          return;
        }
        
        el.innerHTML = filtered.map(a => {
            const c = a.config || {};
            const s = a.state || {};
            const side = (c.transactionType || 'BUY').toUpperCase();
            const color = side === 'BUY' ? 'var(--green)' : 'var(--red)';
            
            let statusHtml = '';
            if (s.exited) {
                statusHtml = `<span class="badge badge-amber">Exited</span>`;
            } else if (s.entered) {
                statusHtml = `<span class="badge badge-green">Active (Entry: ₹${s.entryPrice})</span>`;
            } else {
                statusHtml = `<span class="badge badge-blue">Waiting</span>`;
            }
            
            return `<tr>
              <td><strong>${escHtml(c.symbol)}</strong></td>
              <td><span style="color:${color};font-weight:600">${side}</span></td>
              <td>${c.entryCondition} ${c.entryPrice}</td>
              <td>₹${c.targetPrice}</td>
              <td>₹${s.currentStopLoss > 0 ? s.currentStopLoss : c.stopLossPrice}</td>
              <td>${c.trailingSlPoints} pts</td>
              <td>${statusHtml}</td>
              <td>
                <div style="font-size:11px;color:var(--text-muted)">
                  Entry: ${s.entryOrderId || '--'}<br>
                  Exit: ${s.exitOrderId || '--'}
                </div>
              </td>
              <td>
                 ${s.exited ? '<span style="color:var(--text-muted)">—</span>' : `<button class="btn btn-danger btn-sm btn-ghost" onclick="Orders.cancelStrategy(${c.strategyId})">Cancel</button>`}
              </td>
            </tr>`;
        }).join('');
    }
  },

  async cancel(orderId) {
    const confirmed = await Modal.confirm('Cancel Order', 'Cancel order ' + orderId + '?');
    if (!confirmed) return;
    const res = await API.delete('/api/orders/' + orderId);
    if (res.status === 'success') {
      Toast.info('Order Cancelled', orderId);
      this.loadOrderBook();
    } else {
      Toast.error('Cancel Failed', res.message || 'Unknown error');
    }
  },

  async cancelStrategy(id) {
    const confirmed = await Modal.confirm('Cancel Strategy', 'Cancel strategy ' + id + '?');
    if (!confirmed) return;
    try {
        const res = await API.delete('/api/strategies/' + encodeURIComponent(id));
        if (res.success || res.status === 'success') {
          Toast.info('Strategy Cancelled', 'ID: ' + id);
          this.loadOrderBook();
        } else {
          Toast.error('Cancel Failed', res.message || 'Unknown error');
        }
    } catch (e) {
        Toast.error('Error', e.message);
    }
  },

  async close(dataStr) {
    let data;
    try {
      data = JSON.parse(decodeURIComponent(dataStr));
    } catch(e) {
      Toast.error('Error', 'Invalid order data for closing');
      return;
    }
    
    const priceStr = await Modal.prompt(
      'Close Position', 
      `Close position: ${data.side} ${data.quantity} ${data.symbol}\n\nEnter Limit Price (Leave blank for MARKET order):`,
      'Leave blank for MARKET'
    );
    if (priceStr === null) return; // User cancelled the prompt

    const isLimit = priceStr.trim() !== "";
    const price = isLimit ? parseFloat(priceStr) : null;
    
    if (isLimit && isNaN(price)) {
      Toast.error('Invalid Price', 'Please enter a valid numeric price');
      return;
    }
    
    const body = {
      exchange: data.exchange,
      symbol: data.symbol,
      transactionType: data.side,
      orderType: isLimit ? 'LIMIT' : 'MARKET',
      product: data.product,
      validity: 'DAY',
      quantity: data.quantity,
      tag: 'close_position'
    };
    
    if (isLimit) {
      body.price = price;
    }

    try {
      const res = await API.post('/api/orders', body);
      if (res.success) {
        const orderTypeStr = isLimit ? `LIMIT @ ₹${price}` : 'MARKET';
        Toast.success('Position Closed ✓', `${data.side} ${body.quantity} ${body.symbol} ${orderTypeStr}`);
        this.loadOrderBook();
      } else {
        Toast.error('Close Failed', res.message);
      }
    } catch(e) {
      Toast.error('Network Error', e.message);
    }
  },
};
