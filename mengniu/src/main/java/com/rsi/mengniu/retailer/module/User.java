package com.rsi.mengniu.retailer.module;

public class User extends BaseTO{
	/**
	 * 
	 */
	private static final long serialVersionUID = -765618785271367907L;
	String userId = "";
	String password = "";
	String retailer = "";
	String url = "";
	String district = ""; 
	String agency = "";
	String loginNm = "";
	String storeNo = "";

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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getAgency() {
		return agency;
	}

	public void setAgency(String agency) {
		this.agency = agency;
	}

	public String getLoginNm() {
		return loginNm;
	}

	public void setLoginNm(String loginNm) {
		this.loginNm = loginNm;
	}

	public String toString() {
		return "UserId[" + userId + "],Password[" + password + "],Retailer[" + retailer + "]";
	}

	
	public String getStoreNo() {
		return storeNo;
	}

	public void setStoreNo(String storeNo) {
		this.storeNo = storeNo;
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
