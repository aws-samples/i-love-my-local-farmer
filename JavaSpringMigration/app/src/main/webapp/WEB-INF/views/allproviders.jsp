<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<script src=https://code.jquery.com/jquery-3.6.0.min.js></script>
	<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css"
	    rel="stylesheet"
	    integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC"
	    crossorigin="anonymous">
	<title>List of Providers</title>

	<style>
		tr:first-child{
			font-weight: bold;
			background-color: #C6C9C4;
		}
	</style>

</head>


<body>
	
	<div class="container">
		<h2>List of Providers</h2><br/>	
		<table class="table table-striped">
			<tr>
				<td>Name</td><td>Subscription Date</td><td>Country</td><td>CODE</td><td colspan="2"></td>
			</tr>
			<c:forEach items="${providers}" var="provider">
				<tr>
				<td><a class="btn btn-link" href="<c:url value='/edit-${provider.code}-provider' />">${provider.name}</a></td>
				<td>${provider.enteringDate}</td>
				<td>${provider.nationality}</td>
				<td>${provider.code}</td>
				<td><a class="btn btn-success btn-sm" href="<c:url value='/delete-${provider.code}-provider' />">Delete</a></td>
				</tr>
			</c:forEach>
		</table>
		
		<br/>
		
		<a class="btn btn-primary" role="button" href="<c:url value='/provider/new' />">Add Provider</a>
	</div>
	
</body>
</html>