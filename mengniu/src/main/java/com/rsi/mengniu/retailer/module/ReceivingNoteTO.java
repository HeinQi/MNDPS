package com.rsi.mengniu.retailer.module;

public class ReceivingNoteTO extends BaseTO{

	private static final long serialVersionUID = 1665922323609964610L;
	private String orderNo;
	private String receivingDate;
	private String storeID;
	private String storeName;
	private String itemID;
	private String itemName;
	private String barcode;
	private String quantity;
	private String unitPrice;
	private String totalPrice;

	
	
	public String getOrderNo() {
		return orderNo;
	}



	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}



	public String getReceivingDate() {
		return receivingDate;
	}



	public void setReceivingDate(String receivingDate) {
		this.receivingDate = receivingDate;
	}



	public String getStoreNo() {
		return storeID;
	}



	public void setStoreNo(String storeNo) {
		this.storeID = storeNo;
	}



	public String getStoreName() {
		return storeName;
	}



	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}



	public String getItemCode() {
		return itemID;
	}



	public void setItemCode(String itemCode) {
		this.itemID = itemCode;
	}



	public String getItemName() {
		return itemName;
	}



	public void setItemName(String itemName) {
		this.itemName = itemName;
	}



	public String getBarCode() {
		return barcode;
	}



	public void setBarCode(String barCode) {
		this.barcode = barCode;
	}



	public String getQuantity() {
		return quantity;
	}



	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}



	public String getUnitPrice() {
		return unitPrice;
	}



	public void setUnitPrice(String unitPrice) {
		this.unitPrice = unitPrice;
	}



	public String getTotalPrice() {
		return totalPrice;
	}



	public void setTotalPrice(String totalPrice) {
		this.totalPrice = totalPrice;
	}




	public String toString() {
		return this.orderNo + "\t" + this.receivingDate
				+ "\t" + this.storeID + "\t" + this.storeName + "\t"
				+ this.itemID + "\t" + this.itemName + "\t" + this.quantity
				+ "\t" + this.unitPrice + "\t" + this.totalPrice;
	}

}
