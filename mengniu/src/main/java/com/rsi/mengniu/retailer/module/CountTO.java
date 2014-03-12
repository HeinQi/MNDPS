package com.rsi.mengniu.retailer.module;

public class CountTO extends BaseTO{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6838164615125815346L;
	private int counttotalNo = 0;

	public int getCounttotalNo() {
		return counttotalNo;
	}

	public void setCounttotalNo(int counttotalNo) {
		this.counttotalNo = counttotalNo;
	}
	

	public void increaseOne() {
		 counttotalNo++;
	}
	

}
