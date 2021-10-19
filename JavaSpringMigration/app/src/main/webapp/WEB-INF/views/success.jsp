<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Registration Confirmation Page</title>
	<script src=https://code.jquery.com/jquery-3.6.0.min.js></script>
		<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css"
		    rel="stylesheet"
		    integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC"
		    crossorigin="anonymous">
</head>
<body>
	<div class="container">
		<br/>
		<br/>
		<div class="alert alert-success" role="alert">${success}</div>
		<br/>
		<br/>
		<c:choose>
			<c:when test = "${returnPage == 'product'}">
				<a class="btn btn-primary" href="<c:url value='/product/${farmId}' />">List Products</a>
			</c:when>
			<c:otherwise>
				<c:choose>
					<c:when test = "${returnPage == 'pricing'}">
						<a class="btn btn-success" href="<c:url value='/product/edit/${prodId}' />">List Purchase Options</a>
					</c:when>
					<c:otherwise>
						<a class="btn btn-primary" href="<c:url value='/' />">List Providers</a>
					</c:otherwise>
				</c:choose>
			</c:otherwise>
		</c:choose>
	</div>
</body>

</html>