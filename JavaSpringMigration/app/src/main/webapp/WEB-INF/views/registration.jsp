<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Provider Registration Form</title>
	
	<script src=https://code.jquery.com/jquery-3.6.0.min.js></script>
	<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css"
	    rel="stylesheet"
	    integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC"
	    crossorigin="anonymous">

<style>
	.error {
		color: #ff0000;
	}
</style>

</head>

<body>
	<div class="container">
		<c:choose>
			<c:when test="${edit}">
				<h2>Update Farm Details</h2>
		 	</c:when>
			<c:otherwise>
				<h2>Registration Form</h2>
			</c:otherwise>
		</c:choose>
		<br/>
		<form:form method="POST" modelAttribute="provider">
			<form:input type="hidden" path="id" id="id"/>
			<table  class="table">
				<tr>
					<td><label for="name">Name: </label> </td>
					<td><form:input path="name" id="name"/></td>
					<td><form:errors path="name" cssClass="error"/></td>
			    </tr>
		    
				<tr>
					<td><label for="enteringDate">Entering Date: </label> </td>
					<td><form:input path="enteringDate" id="enteringDate"/></td>
					<td><form:errors path="enteringDate" cssClass="error"/></td>
			    </tr>
		
				<tr>
					<td><label for="nationality">Country: </label> </td>
					<td><form:input path="nationality" id="nationality"/></td>
					<td><form:errors path="nationality" cssClass="error"/></td>
			    </tr>
		
				<tr>
					<td><label for="code">CODE: </label> </td>
					<td><form:input path="code" id="code"/></td>
					<td><form:errors path="code" cssClass="error"/></td>
			    </tr>
			</table>
				
			<c:choose>
				<c:when test="${edit}">
					<button type="submit" class="btn btn-success">Update</button>
				</c:when>
				<c:otherwise>
					<button type="submit" class="btn btn-success">Register</button>
				</c:otherwise>
			</c:choose>


		</form:form>
		<br/>
		<br/>
		
	</div>
	
	<div class="container mt-2">	
		<c:choose>
			<c:when test="${edit}">
				<a class="btn btn-primary" role="button" href="<c:url value='/product/${provider.id}' />">List Products</a>
		 	</c:when>
		</c:choose>
		<a class="btn btn-primary" role="button" href="<c:url value='/' />">List Providers</a>
	</div>
</body>
</html>