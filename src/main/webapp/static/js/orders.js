/**
 * orders.js — Order placement form + live order book
 */
'use strict';

const Orders = {
  currentSide: 'BUY',
  refreshTimer: null,
  allOrders: [],
  currentTab: 'open',

  load() {
    // Only render dynamic form if JSP static form is not present
    if (!document.getElementById('order-symbol')) {
      this.renderForm();
    } else {
      this.initJspForm();
    }
    this.loadOrderBook();
    if (this.orderBookTimer) clearInterval(this.orderBookTimer);
    this.orderBookTimer = setInterval(() => this.loadOrderBook(), 5000);
  },

  // Initialise the static JSP form on first load
  initJspForm() {
    this.setSide('BUY');
    this.onTypeChange();
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
    // JSP field divs
    const pf = document.getElementById('price-field');
    const tf = document.getElementById('trigger-field');
    // Dynamic form field groups
    const pg = document.getElementById('price-group');
    const tg = document.getElementById('trigger-group');
    const showPrice   = type === 'LIMIT' || type === 'SL';
    const showTrigger = type === 'SL'    || type === 'SL-M';
    if (pf) pf.style.display = showPrice   ? 'block' : 'none';
    if (tf) tf.style.display = showTrigger ? 'block' : 'none';
    if (pg) pg.style.display = showPrice   ? 'block' : 'none';
    if (tg) tg.style.display = showTrigger ? 'block' : 'none';
  },

  // Called by JSP Clear button
  resetForm() {
    ['order-symbol','order-qty','order-price','order-trigger'].forEach(id => {
      const el = document.getElementById(id); if (el) el.value = '';
    });
    this.setSide('BUY');
    const ot = document.getElementById('order-type'); if (ot) ot.value = 'MARKET';
    this.onTypeChange();
  },

  // Called by JSP Place Order button — reads from static JSP form
  async placeOrder() {
    const side = this.currentSide;
    const body = {
      exchange:        document.getElementById('order-exchange')?.value || 'NFO',
      symbol:          (document.getElementById('order-symbol')?.value  || '').trim().toUpperCase(),
      transactionType: side,
      orderType:       document.getElementById('order-type')?.value    || 'MARKET',
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
  },

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
      const res = await API.get('/api/orders');
      this.allOrders = res.data || [];
      this.renderOrders();
    } catch(e) {
      const el = document.getElementById('orderbook-tbody');
      if (el) el.innerHTML = `<tr><td colspan="9" style="color:var(--red);padding:16px">${escHtml(e.message)}</td></tr>`;
    }
  },

  setTab(tab) {
    this.currentTab = tab;
    document.getElementById('tab-open').className = 'btn btn-sm ' + (tab === 'open' ? '' : 'btn-ghost');
    document.getElementById('tab-open').style.background = tab === 'open' ? 'var(--bg-card)' : 'transparent';
    document.getElementById('tab-history').className = 'btn btn-sm ' + (tab === 'history' ? '' : 'btn-ghost');
    document.getElementById('tab-history').style.background = tab === 'history' ? 'var(--bg-card)' : 'transparent';
    this.renderOrders();
  },

  renderOrders() {
    const el = document.getElementById('orderbook-tbody');
    if (!el) return;

    if (!this.allOrders.length) {
      el.innerHTML = `<tr><td colspan="9">${emptyState('📋', 'No orders today', 'Place an order using the form')}</td></tr>`;
      return;
    }

    const filtered = this.allOrders.filter(o => {
      const isPending = o.status && (o.status.toUpperCase().includes('OPEN') || o.status.toUpperCase().includes('PENDING'));
      return this.currentTab === 'open' ? isPending : !isPending;
    });

    if (!filtered.length) {
      el.innerHTML = `<tr><td colspan="9">${emptyState('📋', 'No orders found', 'Try checking the other tab')}</td></tr>`;
      return;
    }

    el.innerHTML = filtered.map(o => {
        const side  = (o.transactionType || '').toUpperCase();
        const stCls = ({'COMPLETE':'complete','O-Completed':'complete','OPEN':'open','O-Pending':'open','REJECTED':'rejected','CANCELLED':'cancelled','O-Cancelled':'cancelled'}[o.status] || '');
        const isCancellable = (o.status && (o.status.toUpperCase().includes('OPEN') || o.status.toUpperCase().includes('PENDING')));
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
      `Close position: ${data.side} ${data.quantity} ${data.symbol}\n\nEnter Limit Price (Leave blank for MARKET order):`
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
