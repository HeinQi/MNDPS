package com.rsi.mengniu.retailer.module;

public class SalesTO extends BaseTO{
	
	private static final long serialVersionUID = 4087166645842465278L;
	private String itemID = "";
	private String salesDate = "";
	private String salesQuantity = "";
	private String salesAmount = "";
	
	

	public String toString() {
		return this.itemID + "\t" + this.salesDate + "\t" + this.salesQuantity
				+ "\t" + this.salesAmount;
	}

	public SalesTO(String salesString) {
		String[] orderFieldArray = salesString.split("\t");

		this.setItemID(orderFieldArray[0]);
		this.setSalesDate(orderFieldArray[1]);
		this.setSalesQuantity(orderFieldArray[2]);
		this.setSalesAmount(orderFieldArray[3]);
	}
	
	
	public String getItemID() {
		return itemID;
	}
	public void setItemID(String itemID) {
		this.itemID = itemID;
	}
	public String getSalesDate() {
		return salesDate;
	}
	public void setSalesDate(String salesDate) {
		this.salesDate = salesDate;
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
	
	
	

}
