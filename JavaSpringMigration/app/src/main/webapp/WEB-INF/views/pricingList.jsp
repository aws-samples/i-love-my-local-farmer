<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%--
  Created by IntelliJ IDEA.
  User: dooreaga
  Date: 18/08/2021
  Time: 13:46
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Pricings</title>
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
		<h2>Edit Product</h2><br/>
		<div class="form-group">
			<p class="text-left"><b>Name:</b> ${product.name}</p>
			<p class="text-left"><b>Description:</b> ${product.description}</p>
		</div>
		
		<h4>Purchase Options</h4>
		<table class="table table-striped">
		    <tr>
		        <td>Weight</td>
		        <td>Price</td>
		        <td colspan="2"/>
		    </tr>
		
		    <c:forEach items="${product.productPricings}" var="pricing">
		        <tr>
		            <td>${pricing.weight} kg.</td>
		            <td>€${pricing.price}</td>
		            <td>
		            	<a class="btn btn-success btn-sm" href="<c:url value='/pricing/edit/${pricing.id}' />">Edit</a>
		            	<a class="btn btn-success btn-sm" href="<c:url value='/pricing/delete/${product.id}/${pricing.id}' />">Delete</a>
		           	</td>
		        </tr>
		    </c:forEach>
		</table>
		<br/>
		<a class="btn btn-success" href="<c:url value='/pricing/new/${product.id}' />">Add Purchase Option</a>
	</div>
	
	<div class="container mt-2">
		<a class="btn btn-primary" role="button" href="<c:url value='/product/${product.farmId}' />">List Products</a>
		<a class="btn btn-primary" role="button" href="<c:url value='/' />">List Providers</a>
		
	</div>
</body>
</html>
