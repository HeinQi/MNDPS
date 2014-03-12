package com.rsi.mengniu.retailer.module;

public class AccountLogTO extends BaseTO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2934294527253251233L;

	private String retailerID = "";
	private String userID = "";
	private String password = "";
	private String loginInd = "";
	private String processDateStr = "";
	private int orderDownloadAmount;
	private int receivingDownloadAmount;
	private int salesDownloadAmount;
	private int orderProcessedAmount;
	private int salesProcessedAmount;
	private String successInd = "";

	public String getRetailerID() {
		return retailerID;
	}

	public void setRetailerID(String retailerID) {
		this.retailerID = retailerID;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLoginInd() {
		return loginInd;
	}

	public void setLoginInd(String loginInd) {
		this.loginInd = loginInd;
	}

	public String getProcessDateStr() {
		return processDateStr;
	}

	public void setProcessDateStr(String processDateStr) {
		this.processDateStr = processDateStr;
	}

	public int getOrderDownloadAmount() {
		return orderDownloadAmount;
	}

	public void setOrderDownloadAmount(int orderDownloadAmount) {
		this.orderDownloadAmount = orderDownloadAmount;
	}

	public int getReceivingDownloadAmount() {
		return receivingDownloadAmount;
	}

	public void setReceivingDownloadAmount(int receivingDownloadAmount) {
		this.receivingDownloadAmount = receivingDownloadAmount;
	}

	public int getSalesDownloadAmount() {
		return salesDownloadAmount;
	}

	public void setSalesDownloadAmount(int salesDownloadAmount) {
		this.salesDownloadAmount = salesDownloadAmount;
	}

	public int getOrderProcessedAmount() {
		return orderProcessedAmount;
	}

	public void setOrderProcessedAmount(int orderProcessedAmount) {
		this.orderProcessedAmount = orderProcessedAmount;
	}

	public int getSalesProcessedAmount() {
		return salesProcessedAmount;
	}

	public void setSalesProcessedAmount(int salesProcessedAmount) {
		this.salesProcessedAmount = salesProcessedAmount;
	}

	public String getSuccessInd() {
		return successInd;
	}

	public void setSuccessInd(String successInd) {
		this.successInd = successInd;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	

	public AccountLogTO() {
		super();
	}

	public AccountLogTO(String retailerID, String userID, String password,String processDateStr) {
		super();
		this.retailerID = retailerID;
		this.userID = userID;
		this.password = password;
		this.processDateStr = processDateStr;
		this.successInd = "Y";
	}
	
	

}
