<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ STRATEGIES (OPTIONS) SECTION ════════════════════════════════════ --%>
<section class="page-section" id="page-strategies" style="display:none;">
  <div class="section-header">
    <div>
      <div class="section-title">Options Strategies</div>
      <div class="section-subtitle">Search options, listen to live prices, and configure algorithmic triggers.</div>
    </div>
  </div>

  <div style="display:grid;grid-template-columns:1fr 2fr;gap:24px;">
    <!-- LEFT COLUMN: Search & Watchlist -->
    <div style="display:flex;flex-direction:column;gap:16px;">
      <div class="card p-4">
        <h3 style="margin-bottom:12px;font-size:14px;font-weight:600;">Options Lookup</h3>
        <div style="display:flex;gap:8px;position:relative;">
          <input type="text" id="opt-search-input" class="form-input" placeholder="Search NIFTY CE..." autocomplete="off" oninput="Strategies.searchOptions(this.value)" style="flex:1;">
          <div id="opt-search-results" class="card" style="position:absolute;top:40px;left:0;right:0;max-height:200px;overflow-y:auto;z-index:10;display:none;background:var(--bg-card);border:1px solid var(--border);"></div>
        </div>
      </div>

      <div class="card p-4">
        <h3 style="margin-bottom:12px;font-size:14px;font-weight:600;">Strategy Watchlist</h3>
        <div class="table-wrap" style="max-height:300px;overflow-y:auto;">
          <table>
            <thead>
              <tr><th>Symbol</th><th>Action</th></tr>
            </thead>
            <tbody id="strategy-watchlist-tbody">
              <tr><td colspan="2" class="text-center">No options watched yet</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- RIGHT COLUMN: Configure & Active Strategies -->
    <div style="display:flex;flex-direction:column;gap:16px;">
      <div class="card p-4" id="strategy-config-form" style="display:none; border: 1px solid var(--primary-light);">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3 style="font-size:15px;font-weight:600;color:var(--primary);">Configure Strategy for <span id="strat-symbol-label"></span></h3>
          <button class="btn btn-ghost" onclick="document.getElementById('strategy-config-form').style.display='none'">✕</button>
        </div>
        <form onsubmit="event.preventDefault(); Strategies.submitStrategy();">
          <input type="hidden" id="strat-symbol">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px;">
            <div>
              <label style="font-size:12px;color:var(--text-muted);">Entry Price</label>
              <input type="number" step="0.05" id="strat-entry" class="form-input" required>
            </div>
            <div>
              <label style="font-size:12px;color:var(--text-muted);">Quantity</label>
              <input type="number" id="strat-qty" class="form-input" required>
            </div>
            <div>
              <label style="font-size:12px;color:var(--text-muted);">Target Price</label>
              <input type="number" step="0.05" id="strat-target" class="form-input" required>
            </div>
            <div>
              <label style="font-size:12px;color:var(--text-muted);">Stop Loss</label>
              <input type="number" step="0.05" id="strat-sl" class="form-input" required>
            </div>
          </div>
          <button type="submit" class="btn btn-primary" style="width:100%;">🚀 Activate Algo Strategy</button>
        </form>
      </div>

      <div class="card p-4">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
          <h3 style="font-size:14px;font-weight:600;">Active Strategies</h3>
          <button class="btn btn-ghost" onclick="Strategies.loadActiveStrategies()">↺ Refresh</button>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Symbol</th>
                <th>Entry</th>
                <th>Target</th>
                <th>SL</th>
                <th>State</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody id="active-strategies-tbody">
              <tr><td colspan="6" class="text-center">No active strategies</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</section>
