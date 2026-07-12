<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ SETTINGS SECTION ═══════════════════════════════════════════ --%>
<section class="page-section" id="page-settings">
  <div class="section-header">
    <div>
      <div class="section-title">Settings</div>
      <div class="section-subtitle">Manage broker configurations and access tokens</div>
    </div>
  </div>

  <%-- Existing broker cards --%>
  <div id="settings-content" class="mb-4"></div>

  <%-- Add New Broker form --%>
  <div class="card">
    <div class="card-header">
      <div class="card-title">Add New Broker</div>
    </div>
    <div style="padding:16px;display:grid;grid-template-columns:1fr 1fr;gap:12px">

      <div>
        <label class="form-label">Broker Type</label>
        <select id="cfg-broker-type" class="form-input">
          <option value="MSTOCK">mStock (Mirae Asset)</option>
        </select>
      </div>

      <div>
        <label class="form-label">Display Name</label>
        <input id="cfg-display-name" class="form-input" placeholder="e.g. mStock Main Account">
      </div>

      <div>
        <label class="form-label">API Key</label>
        <input id="cfg-api-key" class="form-input" type="password" placeholder="Your mStock API key">
      </div>

      <div>
        <label class="form-label">Access Token (JWT)</label>
        <input id="cfg-access-token" class="form-input" type="password" placeholder="Today's JWT token">
      </div>

      <div>
        <label class="form-label">Token Expiry</label>
        <input id="cfg-token-expiry" class="form-input" type="datetime-local">
      </div>

      <div>
        <label class="form-label">Client ID <span style="color:var(--text-muted)">(optional)</span></label>
        <input id="cfg-client-id" class="form-input" placeholder="Your client/user ID">
      </div>

      <div style="grid-column:1/-1;display:flex;justify-content:flex-end;gap:8px;padding-top:4px">
        <button class="btn btn-ghost" onclick="Settings.clearForm()">Clear</button>
        <button class="btn btn-primary" onclick="Settings.addBroker()">Add Broker</button>
      </div>
    </div>
  </div>

  <%-- App info footer --%>
  <div style="margin-top:24px;padding:16px;background:var(--bg-card);border-radius:8px;font-size:12px;color:var(--text-muted)">
    <strong style="color:var(--text-secondary)">FnO OMS</strong> &nbsp;·&nbsp;
    Java 25 &nbsp;·&nbsp; Tomcat 11 &nbsp;·&nbsp; PostgreSQL + TimescaleDB &nbsp;·&nbsp; mStock Broker API
  </div>
</section>
