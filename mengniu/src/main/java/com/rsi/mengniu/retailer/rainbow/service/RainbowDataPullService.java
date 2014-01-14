package com.rsi.mengniu.retailer.rainbow.service;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.User;

public class RainbowDataPullService implements RetailerDataPullService {

	@Override
	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		this.login(httpClient, user);
		this.getReceiving();
		
	}

	private String login(CloseableHttpClient httpClient, User user){
		return null;
		
	}
	
	
	private void getReceiving(){
		
	}
}
