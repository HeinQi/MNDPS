package com.rsi.mengniu.retailer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.User;

public class CarrefourDataPull implements RetailerDataPull {
	private static Log log = LogFactory.getLog(CarrefourDataPull.class);
	public void dataPull(User user) {
		log.info("=======Carrefour======"+user);

	}

}
