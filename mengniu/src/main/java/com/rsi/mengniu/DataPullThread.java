package com.rsi.mengniu;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.retailer.service.RetailerDataPull;
import com.rsi.mengniu.util.AppContextHelper;

public class DataPullThread implements Runnable {
	private static Log log = LogFactory.getLog(DataPullThread.class);
	
	public void run() {
		User user = DataPullTaskPool.getTask();
		while (user != null) {
			log.info(user);
			RetailerDataPull dataPull = (RetailerDataPull) AppContextHelper.getBean(user.getRetailer());
			dataPull.dataPull(user);
			user = DataPullTaskPool.getTask();
		}
		
	}

}
