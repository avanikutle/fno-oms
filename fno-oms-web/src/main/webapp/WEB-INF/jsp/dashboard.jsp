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

  <!-- ══ TOP NAVIGATION ════════════════════════════════════════════════ -->
  <header class="top-nav">
    <div class="nav-logo">
      <h1>FnO OMS</h1>
      <span>Options Trading</span>
    </div>

    <div class="nav-links">
      <a class="nav-item" data-page="portfolio" id="nav-portfolio">
        <span class="nav-icon">📊</span> Portfolio
      </a>
      <a class="nav-item active" data-page="orders" id="nav-orders">
        <span class="nav-icon">📋</span> Orders
      </a>
      <a class="nav-item" data-page="connectivity" id="nav-connectivity">
        <span class="nav-icon">🔌</span> Connectivity
      </a>
      <a class="nav-item" data-page="settings" id="nav-settings">
        <span class="nav-icon">⚙️</span> Settings
      </a>
    </div>

    <div class="nav-right">
      <div class="broker-badge">
        <div class="broker-dot" id="broker-dot"></div>
        <div>
          <div style="font-weight:600;font-size:12px" id="broker-name">No Broker</div>
          <div style="font-size:10px;color:var(--text-muted)" id="broker-status">Not configured</div>
        </div>
      </div>
    </div>
  </header>

  <!-- ══ MAIN CONTENT ═══════════════════════════════════════════ -->
  <main class="main-content">

    <!-- Each page is a separate JSP fragment included here -->
    <jsp:include page="pages/portfolio.jsp"/>

    <jsp:include page="pages/orders.jsp"/>
    <jsp:include page="pages/connectivity.jsp"/>
    <jsp:include page="pages/settings.jsp" />
  </main>
</div>

<!-- Toast Container -->
<div class="toast-container" id="toast-container"></div>

<!-- Scripts -->
<script src="<%= ctx %>/static/js/app.js?v=2.3"></script>

<script src="<%= ctx %>/static/js/orders.js?v=2.4"></script>
<script src="<%= ctx %>/static/js/portfolio.js?v=2.1"></script>
<script src="<%= ctx %>/static/js/connectivity.js?v=2.1"></script>

<script>
  document.querySelectorAll('.nav-item[data-page]').forEach(el => {
    el.addEventListener('click', () => {
      const page = el.dataset.page;
      // Load page-specific data on nav
      if (page === 'orders')       Orders.loadOrderBook();
      if (page === 'portfolio')    Portfolio.load();
      if (page === 'connectivity') Connectivity.load();
    });
  });
</script>
</body>
</html>
