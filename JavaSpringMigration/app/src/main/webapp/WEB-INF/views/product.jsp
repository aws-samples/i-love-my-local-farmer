<%--
  Created by IntelliJ IDEA.
  User: dooreaga
  Date: 18/08/2021
  Time: 13:46
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<html>
<head>
    <title>Product</title>
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
		<h2>Farm ${product.farmId} - Add Product</h2><br/>
		
		<form:form method="POST" modelAttribute="product">
		    <form:input type="hidden" path="id" id="id"/>
		    
	        <div class="form-group">
	            <label for="name">Name: ${product.name}</label>
	            <form:input path="name" id="name"/>
	            <form:errors path="name" cssClass="error"/>
	            
	            <label for="description">Description: ${product.description}</label>
	            <form:input path="description" id="description"/>
	            <form:errors path="description" cssClass="error"/>

	        

	            <c:choose>
	                 <c:when test="${edit}">
	                     <button type="submit" class="btn btn-success">Update</button>
	                 </c:when>
	                 <c:otherwise>
	                     <button type="submit" class="btn btn-success">Add</button>
	                 </c:otherwise>
	            </c:choose>
           	</div>

		</form:form>
		<br/>
		<br/>
	</div>
	
	<div class="container mt-2">	
		<a class="btn btn-primary" role="button" href="<c:url value='/product/${product.farmId}' />">List Products</a>
		<a class="btn btn-primary" role="button" href="<c:url value='/' />">List Providers</a>
	</div>
</body>
</html>