package com.rsi.mengniu;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.AppContextHelper;

public class DataPullThread implements Runnable {
	private static Log log = LogFactory.getLog(DataPullThread.class);
	private final CountDownLatch mDoneSignal;  
	DataPullThread(final CountDownLatch doneSignal) {
		this.mDoneSignal= doneSignal;
	}
	public void run() {
		User user = DataPullTaskPool.getTask();
		while (user != null) {
			log.info(user);
			RetailerDataPullService dataPull = null;
			if (Constants.RETAILER_HUALIAN.equals(user.getRetailer())) {
				dataPull = (RetailerDataPullService) AppContextHelper.getBean(user.getRetailer()+"."+user.getDistrict());
			} else {
				dataPull = (RetailerDataPullService) AppContextHelper.getBean(user.getRetailer());
			}
			 
			dataPull.dataPull(user);
			user = DataPullTaskPool.getTask();
		}
		mDoneSignal.countDown();
		
	}

}
