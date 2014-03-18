package com.rsi.mengniu.retailer.common.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.retailer.module.User;

public interface RetailerDataPullService {

	public static Log errorLog = LogFactory.getLog(Constants.SYS_ERROR);
	public void dataPull(User user);
	
	public String getRetailerID();

}
