<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<%
  int code = response.getStatus();
  String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Error <%= code %> — FnO OMS</title>
  <link rel="stylesheet" href="<%= ctx %>/static/css/main.css">
</head>
<body style="display:flex;align-items:center;justify-content:center;height:100vh;">
  <div style="text-align:center;max-width:400px">
    <div style="font-size:72px;margin-bottom:16px"><%= code == 404 ? "🔍" : "⚠️" %></div>
    <div style="font-size:32px;font-weight:700;margin-bottom:8px;color:var(--text-primary)"><%= code %></div>
    <div style="font-size:14px;color:var(--text-secondary);margin-bottom:24px">
      <%= code == 404 ? "Page not found" : "Something went wrong on the server" %>
    </div>
    <a href="<%= ctx %>/" class="btn btn-primary">← Back to Dashboard</a>
  </div>
</body>
</html>
