package com.rsi.mengniu.retailer.module;

import java.io.Serializable;
import java.util.Date;

public class BaseTO implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7475482599034761098L;
	
	private String createBy;
	private Date createDate;
	private String updateBy;
	private Date updateDate;
	
	
	
	public String getCreateBy() {
		return createBy;
	}
	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	public String getUpdateBy() {
		return updateBy;
	}
	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}
	public Date getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	
	
}
