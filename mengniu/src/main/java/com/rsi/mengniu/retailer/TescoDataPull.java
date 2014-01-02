package com.rsi.mengniu.retailer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.User;

public class TescoDataPull implements RetailerDataPull {
	private static Log log = LogFactory.getLog(TescoDataPull.class);
	public void dataPull(User user) {
		log.info("=======Tesco======"+user);

	}

}
