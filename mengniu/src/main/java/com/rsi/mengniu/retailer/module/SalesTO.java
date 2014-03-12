package com.rsi.mengniu.retailer.module;

public class SalesTO extends BaseTO {

	private static final long serialVersionUID = 4087166645842465278L;
	private String itemID = "";
	private String itemName = "";
	private String salesDate = "";
	private String storeID = "";
	private String salesQuantity = "";
	private String salesAmount = "";
	private String userID = "";

	public String toString() {
		return this.itemID + "\t" + this.itemName + "\t" + this.salesDate
				+ "\t" + this.storeID + "\t" + this.salesQuantity + "\t"
						+ this.salesAmount;
		//+ "\t"						+ this.userID;
	}

	public SalesTO() {

	}

	public SalesTO(String salesString) {
		String[] orderFieldArray = salesString.split("\t",-1);

		this.setItemID(orderFieldArray[0]);
		this.setItemName(orderFieldArray[1]);
		this.setSalesDate(orderFieldArray[2]);
		this.setStoreID(orderFieldArray[3]);
		this.setSalesQuantity(orderFieldArray[4]);
		this.setSalesAmount(orderFieldArray[5]);
		//this.setUserID(orderFieldArray[6]);
	}

	public String getItemID() {
		return itemID;
	}

	public void setItemID(String itemID) {
		this.itemID = itemID;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public String getSalesDate() {
		return salesDate;
	}

	public void setSalesDate(String salesDate) {
		this.salesDate = salesDate;
	}

	public String getStoreID() {
		return storeID;
	}

	public void setStoreID(String storeID) {
		this.storeID = storeID;
	}

	public String getSalesQuantity() {
		return salesQuantity;
	}

	public void setSalesQuantity(String salesQuantity) {
		this.salesQuantity = salesQuantity;
	}

	public String getSalesAmount() {
		return salesAmount;
	}

	public void setSalesAmount(String salesAmount) {
		this.salesAmount = salesAmount;
	}


	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

}
