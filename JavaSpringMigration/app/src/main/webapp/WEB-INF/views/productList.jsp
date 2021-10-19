<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
         pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <title>Products</title>
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
		<h2>List of Products Sold - Farm #${farmId}</h2>
		<br/>
		<table class="table table-striped">
		    <tr>
		        <td>ID</td><td>Name</td><td>Description</td><td></td>
		    </tr>
		    <c:forEach items="${products}" var="product">
	            <tr>
	                <td>${product.id}</td>
	                <td><a class="btn btn-link" href="<c:url value='/product/edit/${product.id}' />">${product.name}</a></td>
	                <td>${product.description}</td>
	                <td><a class="btn btn-success btn-sm" href="<c:url value='/product/delete/${farmId}/${product.id}' />">Delete</a></td>
	            </tr>
		    </c:forEach>
		</table>
		<a class="btn btn-success" role="button" href="<c:url value='/product/new/${farmId}' />">Add Product</a>
	</div>
	
	<div class="container mt-3">
		<a class="btn btn-primary" role="button" href="<c:url value='/' />">List Providers</a>
	</div>
</body>
</html>