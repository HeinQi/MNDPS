package com.rsi.mengniu.retailer.module;

public class User {
	String userId;
	String password;
	String retailer;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRetailer() {
		return retailer;
	}
	public void setRetailer(String retailer) {
		this.retailer = retailer;
	}
	
	public String toString() {
		return "UserId["+userId+"],Password["+password+"],Retailer["+retailer+"]";
	}
}
