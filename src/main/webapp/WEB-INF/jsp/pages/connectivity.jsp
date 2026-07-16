<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ CONNECTIVITY SECTION ═══════════════════════════════════════ --%>
<section class="page-section" id="page-connectivity">
  <div class="section-header">
    <div>
      <div class="section-title">Connectivity</div>
      <div class="section-subtitle">Test broker API reachability and latency</div>
    </div>
    <div style="display:flex;gap:8px;">
      <button class="btn btn-primary" onclick="Connectivity.testAll()">⚡ Test All</button>
      <button class="btn btn-ghost" onclick="Connectivity.login('MSTOCK')">🔑 Login mStock</button>
      <button class="btn btn-ghost" onclick="Connectivity.login('ANGELONE')">🔑 Login AngelOne</button>
    </div>
  </div>

  <%-- Overall status banner --%>
  <div id="connectivity-banner" class="card mb-4" style="padding:16px 20px;display:flex;align-items:center;gap:12px">
    <div style="font-size:28px" id="connectivity-icon">🔄</div>
    <div>
      <div style="font-weight:700;font-size:15px" id="connectivity-title">Not tested yet</div>
      <div style="font-size:12px;color:var(--text-muted)" id="connectivity-subtitle">Click "Test All" to check all configured brokers</div>
    </div>
  </div>

  <%-- Results grid --%>
  <div id="connectivity-content">
    <div class="card">
      <div class="card-header">
        <div class="card-title">Broker Endpoints</div>
        <span id="connectivity-last-run" style="font-size:11px;color:var(--text-muted)">Never tested</span>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Broker</th>
              <th>Endpoint</th>
              <th>Status</th>
              <th>Latency</th>
              <th>Last Tested</th>
            </tr>
          </thead>
          <tbody id="connectivity-tbody">
            <tr><td colspan="5" style="padding:32px;text-align:center;color:var(--text-muted)">
              No brokers configured — go to Settings to add one
            </td></tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</section>
