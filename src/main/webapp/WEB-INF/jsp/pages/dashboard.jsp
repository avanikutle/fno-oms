<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%-- ══ DASHBOARD SECTION ══════════════════════════════════════════ --%>
<section class="page-section active" id="page-dashboard">
  <div class="section-header">
    <div>
      <div class="section-title">Dashboard</div>
      <div class="section-subtitle">Today's trading summary</div>
    </div>
    <button class="btn btn-ghost" onclick="Dashboard.load()">↺ Refresh</button>
  </div>
  <div id="dashboard-stats"></div>
</section>
