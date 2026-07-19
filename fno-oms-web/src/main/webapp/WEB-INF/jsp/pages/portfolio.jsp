<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ PORTFOLIO SECTION ══════════════════════════════════════════ --%>
<section class="page-section" id="page-portfolio">
  <div class="section-header">
    <div>
      <div class="section-title">Portfolio</div>
      <div class="section-subtitle">Positions and holdings</div>
    </div>
    <button class="btn btn-ghost" onclick="Portfolio.load()">↺ Refresh</button>
  </div>

  <%-- P&L Summary strip --%>
  <div id="portfolio-summary" class="mb-4"></div>

  <%-- Tabs --%>
  <div style="display:flex;gap:4px;margin-bottom:16px">
    <button class="btn btn-primary"  id="tab-positions" onclick="Portfolio.showTab('positions')">Net Positions</button>
    <button class="btn btn-ghost"    id="tab-holdings"  onclick="Portfolio.showTab('holdings')">Holdings</button>
  </div>

  <%-- Net Positions --%>
  <div id="tab-panel-positions" class="card mb-4">
    <div class="card-header">
      <div class="card-title">Net Positions</div>
      <span class="badge badge-amber" style="font-size:10px">Auto-refresh 5s</span>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Exchange</th>
            <th>Product</th>
            <th>Qty</th>
            <th>Avg Price</th>
            <th>LTP</th>
            <th>P&amp;L</th>
            <th>Realised</th>
            <th>Unrealised</th>
          </tr>
        </thead>
        <tbody id="positions-tbody">
          <tr><td colspan="9" style="padding:32px;text-align:center;color:var(--text-muted)">Loading…</td></tr>
        </tbody>
      </table>
    </div>
  </div>

  <%-- Holdings --%>
  <div id="tab-panel-holdings" class="card" style="display:none">
    <div class="card-header">
      <div class="card-title">Holdings</div>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Exchange</th>
            <th>Qty</th>
            <th>T1 Qty</th>
            <th>Avg Price</th>
            <th>LTP</th>
            <th>P&amp;L</th>
          </tr>
        </thead>
        <tbody id="holdings-tbody">
          <tr><td colspan="7" style="padding:32px;text-align:center;color:var(--text-muted)">Loading…</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</section>
