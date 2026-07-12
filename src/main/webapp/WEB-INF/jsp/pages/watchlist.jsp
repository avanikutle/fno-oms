<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ WATCHLIST SECTION ══════════════════════════════════════════ --%>
<section class="page-section" id="page-watchlist">
  <div class="section-header">
    <div>
      <div class="section-title">Watchlist</div>
      <div class="section-subtitle">Live quotes — auto-refreshes every 2s</div>
    </div>
    <div style="display:flex;gap:8px;align-items:center">
      <span class="badge badge-green" style="font-size:10px">● LIVE</span>
      <button class="btn btn-ghost" onclick="Quotes.init()">↺ Refresh</button>
    </div>
  </div>

  <%-- Add symbol bar --%>
  <div id="watchlist-add" class="mb-4">
    <div class="card" style="padding:12px 16px">
      <div style="display:flex;gap:8px;align-items:center">
        <input  id="symbol-input"
                class="form-input"
                placeholder="e.g. NSE:NIFTY 50  or  NFO:NIFTY24JUL24000CE"
                style="flex:1;font-family:monospace;font-size:13px"
                onkeydown="if(event.key==='Enter') Quotes.addSymbol()">
        <select id="exchange-select" class="form-input" style="width:100px">
          <option value="NSE">NSE</option>
          <option value="NFO">NFO</option>
          <option value="BSE">BSE</option>
          <option value="BFO">BFO</option>
          <option value="MCX">MCX</option>
        </select>
        <button class="btn btn-primary" onclick="Quotes.addSymbol()">+ Add</button>
      </div>
    </div>
  </div>

  <div class="card">
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Exchange</th>
            <th>LTP</th>
            <th>Open</th>
            <th>High</th>
            <th>Low</th>
            <th>Change %</th>
            <th>OI</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody id="watchlist-tbody">
          <tr><td colspan="9" style="padding:32px;text-align:center;color:var(--text-muted)">
            Add symbols above to start watching
          </td></tr>
        </tbody>
      </table>
    </div>
  </div>
</section>
