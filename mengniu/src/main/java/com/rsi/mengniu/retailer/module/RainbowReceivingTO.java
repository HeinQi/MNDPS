package com.rsi.mengniu.retailer.module;

public class RainbowReceivingTO extends ReceivingNoteTO {
	
	private static final long serialVersionUID = 7620979609472511745L;
	
	private String orderQuantity = "";
	private String orderTotalPrice = "";
	private String receivingQuantity = "";
	private String receivingTotalPrice = "";

	
	
	public RainbowReceivingTO(String orderNo, String storeID, String storeName,
			String receivingDate, String itemID, String barcode,
			String itemName, String orderQuantity, String orderTotalPrice,
			String receivingQuantity, String receivingTotalPrice,
			String unitPrice) {
		this.orderNo = orderNo;
		this.storeID = storeID;
		this.storeName = storeName;
		this.receivingDate = receivingDate;
		this.itemID = itemID;
		this.barcode = barcode;
		this.itemName = itemName;
		this.orderQuantity = orderQuantity;
		this.orderTotalPrice = orderTotalPrice;
		this.receivingQuantity = receivingQuantity;
		this.receivingTotalPrice = receivingTotalPrice;
		this.unitPrice = unitPrice;
	}



	public RainbowReceivingTO(String consolidatedString) {
		String[] consolidatedFieldArray = consolidatedString.split("\t",-1);

		this.setOrderNo(consolidatedFieldArray[0]);
		this.setStoreID(consolidatedFieldArray[1]);
		this.setStoreName(consolidatedFieldArray[2]);
		this.setReceivingDate(consolidatedFieldArray[3]);
		this.setItemID(consolidatedFieldArray[4]);
		this.setBarcode(consolidatedFieldArray[5]);
		this.setItemName(consolidatedFieldArray[6]);
		this.setOrderQuantity(consolidatedFieldArray[7]);
		this.setOrderTotalPrice(consolidatedFieldArray[8]);
		this.setReceivingQuantity(consolidatedFieldArray[9]);
		this.setReceivingTotalPrice(consolidatedFieldArray[10]);
		this.setUnitPrice(consolidatedFieldArray[11]);
	}
	

	public String toString() {
		return this.orderNo
				+ "\t" + this.storeID
				+ "\t" + this.storeName
				+ "\t" + this.receivingDate 
				+ "\t" + this.itemID 
				+ "\t" + this.barcode 
				+ "\t" + this.itemName 
				+ "\t" + this.orderQuantity 
				+ "\t" + this.orderTotalPrice
				+ "\t" + this.receivingQuantity
				+ "\t" + this.receivingTotalPrice
				+ "\t" + this.unitPrice;
	}



	public String getOrderQuantity() {
		return orderQuantity;
	}



	public void setOrderQuantity(String orderQuantity) {
		this.orderQuantity = orderQuantity;
	}



	public String getOrderTotalPrice() {
		return orderTotalPrice;
	}



	public void setOrderTotalPrice(String orderTotalPrice) {
		this.orderTotalPrice = orderTotalPrice;
	}



	public String getReceivingQuantity() {
		return receivingQuantity;
	}



	public void setReceivingQuantity(String receivingQuantity) {
		this.receivingQuantity = receivingQuantity;
	}



	public String getReceivingTotalPrice() {
		return receivingTotalPrice;
	}



	public void setReceivingTotalPrice(String receivingTotalPrice) {
		this.receivingTotalPrice = receivingTotalPrice;
	}



}
