package com.rsi.mengniu.retailer.module;

public class TescoOrderNotifyTO {
	private String poId;
	private String parentVendor;
	private String fileName;
	private String createDate;
	
	public String getPoId() {
		return poId;
	}
	public void setPoId(String poId) {
		this.poId = poId;
	}
	public String getParentVendor() {
		return parentVendor;
	}
	public void setParentVendor(String parentVendor) {
		this.parentVendor = parentVendor;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getCreateDate() {
		return createDate;
	}
	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}
	public String toString() {
		return poId+","+parentVendor+","+fileName+","+createDate;
	}
	

}
