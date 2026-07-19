<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ ORDERS SECTION ═════════════════════════════════════════════ --%>
<section class="page-section" id="page-orders">
  <div class="section-header">
    <div>
      <div class="section-title">Orders</div>
      <div class="section-subtitle">Place and manage your trades</div>
    </div>
    <button class="btn btn-ghost" onclick="Orders.loadOrderBook()">↺ Refresh</button>
  </div>

  <div class="order-panel">

    <%-- ── 1. Watchlist Row ──────────────────────────── --%>
    <div class="card">
      <div class="card-header">
        <div class="card-title">Options Watchlist</div>
        <div style="display:flex;gap:8px">
          <input type="text" id="watchlist-search" class="form-control" style="width:250px" placeholder="Search NIFTY, BANKNIFTY..." autocomplete="off">
          <button class="btn btn-ghost" onclick="Orders.clearWatchlistSearch()">Clear</button>
        </div>
      </div>
      <div style="position:relative">
        <div id="watchlist-search-results" class="search-dropdown" style="display:none;position:absolute;top:0;right:0;background:var(--bg-card);border:1px solid var(--border);border-radius:4px;width:320px;max-height:300px;overflow-y:auto;z-index:100;box-shadow:var(--shadow-card)"></div>
      </div>
      <div class="table-wrap">
        <table id="watchlist-table">
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Expiry</th>
              <th>Strike</th>
              <th>Type</th>
              <th>LTP (Price)</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody id="watchlist-tbody">
            <tr><td colspan="6" style="padding:16px;text-align:center;color:var(--text-muted)">Watchlist is empty. Search to add up to 6 instruments.</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <%-- ── 2. Place Order Form ──────────────────────────── --%>
    <div class="card">
      <div class="card-header">
        <div class="card-title">Place Order</div>
        <div style="display:flex;gap:6px">
          <button id="btn-buy"  class="btn btn-success active-side" onclick="Orders.setSide('BUY')">BUY</button>
          <button id="btn-sell" class="btn btn-danger"              onclick="Orders.setSide('SELL')">SELL</button>
        </div>
      </div>

      <div style="padding:16px;display:grid;grid-template-columns:1fr 1fr;gap:12px" id="order-form-panel">
        <div>
          <label class="form-label">Order Type</label>
          <select id="order-type" class="form-input" onchange="Orders.onTypeChange()">
            <option value="ALGO" selected>ALGO (Strategy)</option>
            <option value="MARKET">MARKET</option>
            <option value="LIMIT">LIMIT</option>
            <option value="SL">SL</option>
            <option value="SL-M">SL-M</option>
          </select>
        </div>
        <div>
          <label class="form-label">Symbol</label>
          <input id="order-symbol" class="form-input" placeholder="e.g. NIFTY24JUL24000CE">
        </div>
        <div>
          <label class="form-label">Exchange</label>
          <select id="order-exchange" class="form-input">
            <option value="NFO">NFO</option>
            <option value="NSE">NSE</option>
            <option value="BSE">BSE</option>
            <option value="MCX">MCX</option>
          </select>
        </div>
        <div>
          <label class="form-label">Product</label>
          <select id="order-product" class="form-input">
            <option value="NRML" selected>NRML (Carry-forward)</option>
            <option value="MIS">MIS (Intraday)</option>
            <option value="CNC">CNC (Delivery)</option>
          </select>
        </div>
        <div>
          <label class="form-label">Quantity</label>
          <input id="order-qty" class="form-input" type="number" min="1" value="1">
        </div>
        <div id="price-field">
          <label class="form-label" id="lbl-price">Price</label>
          <input id="order-price" class="form-input" type="number" step="0.05" placeholder="0.00">
        </div>
        <div id="trigger-field" style="display:none">
          <label class="form-label">Trigger Price</label>
          <input id="order-trigger" class="form-input" type="number" step="0.05" placeholder="0.00">
        </div>
        
        <!-- ALGO specific fields -->
        <div id="algo-target-field" style="display:none">
          <label class="form-label">Target Price</label>
          <input id="algo-target" class="form-input" type="number" step="0.05" placeholder="0.00">
        </div>
        <div id="algo-sl-field" style="display:none">
          <label class="form-label">Stop Loss</label>
          <input id="algo-sl" class="form-input" type="number" step="0.05" placeholder="0.00">
        </div>
        <div id="algo-trail-field" style="display:none">
          <label class="form-label">Trailing SL Points</label>
          <input id="algo-trail" class="form-input" type="number" step="0.05" placeholder="0.00">
        </div>
        <div style="grid-column:1/-1;display:flex;gap:8px;justify-content:flex-end;padding-top:4px">
          <button class="btn btn-ghost" onclick="Orders.resetForm()">Clear</button>
          <button class="btn btn-primary" onclick="Orders.placeOrder()">Place Order</button>
        </div>
      </div>
    </div>

    <%-- ── 3. Order Book ────────────────────────────────── --%>
    <div class="card">
      <div class="card-header">
        <div class="card-title">Order Book</div>
        <div style="display:flex;gap:12px;align-items:center">
          <div class="tabs" style="display:flex;gap:4px;background:var(--bg-hover);padding:4px;border-radius:4px">
            <button id="tab-algos" class="btn btn-sm" style="background:var(--bg-card)" onclick="Orders.setTab('algos')">Algo Strategies</button>
            <button id="tab-broker" class="btn btn-sm btn-ghost" onclick="Orders.setTab('broker')">Live Broker Orders</button>
          </div>
          <button class="btn btn-ghost btn-sm" onclick="Orders.loadOrderBook()">↺ Refresh</button>
        </div>
      </div>
      <div class="table-wrap">
        <table id="orderbook-table">
          <thead id="orderbook-thead">
            <tr>
              <th>Symbol</th>
              <th>Side</th>
              <th>Type</th>
              <th>Qty</th>
              <th>Price</th>
              <th>Avg</th>
              <th>Status</th>
              <th>Order ID</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody id="orderbook-tbody">
            <tr><td colspan="9" style="padding:32px;text-align:center;color:var(--text-muted)">Loading…</td></tr>
          </tbody>
        </table>
      </div>
    </div>

  </div>
</section>
