package com.myfarmer.provman.controller;

/**
 * Enum for views to be rendered 
 *
 */
public enum Views {
	ALLPROVIDERS ("allproviders"),
	PRICING("pricing"),
	PRICINGLIST("pricingList"),
	PRODUCT("product"),
	PRODUCTLIST("productList"),
	REGISTRATION("registration"),
	SUCCESS("success");
	
	private String viewName;
	
	private Views(String viewName) {
		this.viewName = viewName;
	}
	
	public String getViewName() {
		return this.viewName;
	}
}
