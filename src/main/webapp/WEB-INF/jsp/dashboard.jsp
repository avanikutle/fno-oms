<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="ctx-path" content="<%= ctx %>">
  <title>FnO OMS — Options Order Management System</title>
  <meta name="description" content="Professional options trading order management system with live mStock broker integration">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="<%= ctx %>/static/css/main.css">
</head>
<body>

<div class="app-shell">

  <!-- ══ TICKER BAR ══════════════════════════════════════════════ -->
  <header class="ticker-bar">
    <div class="ticker-track" id="ticker-track">
      <div class="ticker-item">
        <span class="ticker-symbol">NIFTY</span>
        <span class="ticker-price">—</span>
        <span class="ticker-change">Connecting…</span>
      </div>
    </div>
  </header>

  <!-- ══ SIDEBAR ════════════════════════════════════════════════ -->
  <aside class="sidebar">
    <div class="sidebar-logo">
      <h1>FnO OMS</h1>
      <span>Options Trading</span>
    </div>

    <span class="nav-section-label">Trading</span>
    <a class="nav-item active" data-page="dashboard" id="nav-dashboard">
      <span class="nav-icon">📈</span> Dashboard
    </a>
    <a class="nav-item" data-page="watchlist" id="nav-watchlist">
      <span class="nav-icon">👁</span> Watchlist
    </a>
    <a class="nav-item" data-page="orders" id="nav-orders">
      <span class="nav-icon">📋</span> Orders
    </a>
    <a class="nav-item" data-page="portfolio" id="nav-portfolio">
      <span class="nav-icon">📊</span> Portfolio
    </a>

    <span class="nav-section-label" style="margin-top:8px">System</span>
    <a class="nav-item" data-page="connectivity" id="nav-connectivity">
      <span class="nav-icon">🔌</span> Connectivity
    </a>
    <a class="nav-item" data-page="settings" id="nav-settings">
      <span class="nav-icon">⚙️</span> Settings
    </a>

    <div class="sidebar-footer">
      <div class="broker-badge">
        <div class="broker-dot" id="broker-dot"></div>
        <div>
          <div style="font-weight:600;font-size:12px" id="broker-name">No Broker</div>
          <div style="font-size:10px;color:var(--text-muted)" id="broker-status">Not configured</div>
        </div>
      </div>
    </div>
  </aside>

  <!-- ══ MAIN CONTENT ═══════════════════════════════════════════ -->
  <main class="main-content">

    <!-- Each page is a separate JSP fragment included here -->
    <jsp:include page="pages/dashboard.jsp"/>
    <jsp:include page="pages/watchlist.jsp"/>
    <jsp:include page="pages/orders.jsp"/>
    <jsp:include page="pages/portfolio.jsp"/>
    <jsp:include page="pages/connectivity.jsp"/>
    <jsp:include page="pages/settings.jsp"/>

  </main>
</div>

<!-- Toast Container -->
<div class="toast-container" id="toast-container"></div>

<!-- Scripts -->
<script src="<%= ctx %>/static/js/app.js?v=2.2"></script>
<script src="<%= ctx %>/static/js/quotes.js"></script>
<script src="<%= ctx %>/static/js/orders.js?v=2.2"></script>
<script src="<%= ctx %>/static/js/portfolio.js"></script>
<script src="<%= ctx %>/static/js/connectivity.js"></script>

<script>
  // Wire up watchlist polling — start when entering, stop when leaving
  document.querySelectorAll('.nav-item[data-page]').forEach(el => {
    el.addEventListener('click', () => {
      const page = el.dataset.page;
      if (page === 'watchlist') {
        Quotes.init();
      } else {
        Quotes.stop();
      }
      // Load page-specific data on nav
      if (page === 'orders')       Orders.loadOrderBook();
      if (page === 'portfolio')    Portfolio.load();
      if (page === 'dashboard')    Dashboard.load();
      if (page === 'connectivity') Connectivity.load();
      if (page === 'settings')     Settings.load();
    });
  });
</script>
</body>
</html>
