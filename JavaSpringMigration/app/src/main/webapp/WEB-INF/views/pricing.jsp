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
    <title>Purchase Option</title>
	<script src=https://code.jquery.com/jquery-3.6.0.min.js></script>
	<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css"
	    rel="stylesheet"
	    integrity="sha384-EVSTQN3/azprG1Anm3QDgpJLIm9Nao0Yz1ztcQTwFspd3yD65VohhpuuCOmLASjC"
	    crossorigin="anonymous">
    <style>
        tr:first-child{
            font-weight: bold;
            background-color: #C6C9C4;
        }
    </style>
</head>
<body>
	<div class="container">
		<h2>Edit Purchase Option</h2>
		<br/>
		<form:form method="POST" modelAttribute="pricing">
		    <form:input type="hidden" path="id" id="id"/>
		    <form:input type="hidden" path="product.id" value="${pricing.product.id}"/>
		    <form:input type="hidden" path="product.farmId" value="${pricing.product.farmId}"/>
		    
		    <div class="form-group">
			    <label for="productName">Product Name</label>
			    <input type="text" class="form-control" id="productName" value="${pricing.product.name}" readonly>
			    
			    <label for="productDesc">Product Description</label>
			    <input type="text" class="form-control" id="productDesc" value="${pricing.product.description}" readonly>
			    
				<label for="price">Price:</label>
			    <form:input type="text" path="price" id="price" class="form-control" value="${pricing.price}"/>
				<form:errors path="price" cssClass="error"/>
	
				<label for="weight">Weight: </label>
			    <form:input type="text" path="weight" id="weight" class="form-control" value="${pricing.weight}"/>
			    <form:errors path="weight" cssClass="error"/>
			    	
			    <br/>
	            <c:choose>
	                <c:when test = "${edit}">
	                	<button type="submit" class="btn btn-success">Update</button>
	                </c:when>
	                <c:otherwise>
	                	<button type="submit" class="btn btn-success">Add</button>
	                </c:otherwise>
	            </c:choose>
	        </div>
        </form:form>
    </div>
    
    <div class="container mt-2">
		<a class="btn btn-primary" role="button" href="<c:url value='/pricing/${product.id}' />">List Purchase Options</a>
	</div>
</body>
</html>
