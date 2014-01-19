package com.rsi.mengniu.retailer.module;

public class ReceivingNoteTO extends BaseTO {

	protected static final long serialVersionUID = 1665922323609964610L;


	protected String orderNo=""; // 1
	protected String receivingDate="";// 2
	protected String storeID=""; // 3
	protected String storeName="";// 4
	protected String itemID=""; // 5
	protected String itemName=""; // 6
	protected String barcode="";// 7
	protected String quantity="";// 8
	protected String unitPrice="";// 9
	protected String totalPrice="";// 10

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

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
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

	public ReceivingNoteTO(String receivingString) {
		String[] receivingFieldArray = receivingString.split("\t");

		this.setOrderNo(receivingFieldArray[0]);
		this.setReceivingDate(receivingFieldArray[1]);
		this.setStoreNo(receivingFieldArray[2]);
		this.setStoreName(receivingFieldArray[3]);
		this.setItemCode(receivingFieldArray[4]);
		this.setItemName(receivingFieldArray[5]);
		this.setBarcode(receivingFieldArray[6]);
		this.setQuantity(receivingFieldArray[7]);
		this.setUnitPrice(receivingFieldArray[8]);
		this.setTotalPrice(receivingFieldArray[9]);

	}

	public ReceivingNoteTO() {
		// TODO Auto-generated constructor stub
	}

	public String toString() {
		return this.orderNo + "\t" + this.receivingDate + "\t" + this.storeID
				+ "\t" + this.storeName + "\t" + this.itemID + "\t"
				+ this.itemName + "\t" + this.barcode + "\t" + this.quantity
				+ "\t" + this.unitPrice + "\t" + this.totalPrice;
	}

}
