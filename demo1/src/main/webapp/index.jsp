<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>JSP - Hello World</title>
</head>
<body>
<h1><%= "Hello World!" %>
</h1>
<br/>
<form action="hello-servlet" method="GET" id="SearchEngineRequest">
    Search: <input type="text" name="Search"/>
    <br>
    <br>
    <input type="submit" name="Submit"/>
</form>
</body>
</html>