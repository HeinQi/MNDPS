package com.rsi.mengniu.retailer.module;

public class User {
	String userId = "";
	String password = "";
	String retailer = "";
	String district = "";

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

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
		return "UserId[" + userId + "],Password[" + password + "],Retailer[" + retailer + "]";
	}

	public boolean equals(Object obj) {
		if (obj instanceof User) {
			User user = (User) obj;
			return this.userId.equals(((User) user).userId) && this.password.equals(((User) user).password)
					&& this.retailer.equals(((User) user).retailer);

		}
		return super.equals(obj);
	}

	public int hashCode() {
		return userId.hashCode() + password.hashCode() + retailer.hashCode();
	}
}
