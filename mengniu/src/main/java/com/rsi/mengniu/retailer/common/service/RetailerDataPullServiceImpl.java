package com.rsi.mengniu.retailer.common.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.http.impl.client.CloseableHttpClient;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.DataPullTaskPool;
import com.rsi.mengniu.retailer.module.AccountLogTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.AccountLogUtil;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.Utils;

public abstract class RetailerDataPullServiceImpl implements RetailerDataPullService {

	protected abstract Log getLog();

	protected abstract Log getSummaryLog();


	public void dataPull(User user) {
		CloseableHttpClient httpClient = Utils.createHttpClient(getRetailerID());
		StringBuffer summaryBuffer = new StringBuffer();
		summaryBuffer.append("运行时间: " + new Date() + "\r\n");
		summaryBuffer.append("零售商: " + user.getRetailer() + "\r\n");
		summaryBuffer.append("用户: " + user.getUserId() + "\r\n");

		// Login
		boolean loginInd = login(user, httpClient, summaryBuffer);
		if (!loginInd)
			return;

		String retailerID = getRetailerID();
		List<Date> dates = DateUtil.getDateArrayByRange(Utils.getStartDate(retailerID), Utils.getEndDate(retailerID));

		summaryBuffer.append(Constants.SUMMARY_TITLE_ORDER + "\r\n");
		// Get Order Data
		getOrderData(httpClient, user, dates, summaryBuffer);

		summaryBuffer.append(Constants.SUMMARY_TITLE_RECEIVING + "\r\n");
		// Get Receive Data
		getReceiveData(httpClient, user, dates, summaryBuffer);

		summaryBuffer.append(Constants.SUMMARY_TITLE_SALES + "\r\n");
		// Get Receive Data
		getSalesData(httpClient, user, dates, summaryBuffer);

		try {
			httpClient.close();
		} catch (IOException e) {
			errorLog.error(user, e);
		}

		summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
		getSummaryLog().info(summaryBuffer);

	}

	public boolean login(User user, CloseableHttpClient httpClient, StringBuffer summaryBuffer) {

		AccountLogTO accountLogLoginTO = new AccountLogTO(user.getRetailer(), user.getUserId(), user.getPassword(), "");

		String loginResult = null;
		int loginCount = 0; // 如果验证码出错重新login,最多15次
		try {
			do {
				loginResult = loginDetail(httpClient, user);
				loginCount++;
			} while ("InvalidCode".equals(loginResult) && loginCount < 15);
			// Invalid Password and others
			if (!"Success".equals(loginResult)) {
				summaryBuffer.append("登录失败!\r\n");
				summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
				getSummaryLog().info(summaryBuffer);
				
				AccountLogUtil.loginFailed(accountLogLoginTO);
				
				return false;
			}
			AccountLogUtil.loginSuccess(accountLogLoginTO);
		} catch (Exception e) {
			summaryBuffer.append("登录失败!\r\n");
			getLog().error(user + "网站登录出错,请检查!");
			errorLog.error(user, e);
			summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
			getSummaryLog().info(summaryBuffer);
			DataPullTaskPool.addFailedUser(user);

			return false;
		}
		return true;
	}

	/**
	 * Login
	 * 
	 * @param httpClient
	 * @param user
	 * @return
	 * @throws Exception
	 */
	protected abstract String loginDetail(CloseableHttpClient httpClient, User user) throws Exception;

	/**
	 * Get Order Data
	 * 
	 * @param httpClient
	 * @param user
	 * @param dates
	 * @param summaryBuffer
	 */
	public void getOrderData(CloseableHttpClient httpClient, User user, List<Date> dates, StringBuffer summaryBuffer) {
		for (Date processDate : dates) {
			int orderAmount = 0;
			AccountLogTO accountLogTO = new AccountLogTO(user.getRetailer(), user.getUserId(), user.getPassword(),
					DateUtil.toString(processDate));
			try {
				// order
				orderAmount = getOrder(httpClient, user, processDate, summaryBuffer);
				// 设置订单下载数量
				accountLogTO.setOrderDownloadAmount(orderAmount);
				AccountLogUtil.recordOrderDownloadAmount(accountLogTO);
			} catch (Exception e) {
				summaryBuffer.append("订单下载出错" + "\r\n");
				summaryBuffer.append("成功数量: " + orderAmount + "\r\n");
				getLog().error(user + "页面加载失败，请登录网站检查订单功能是否正常！");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
			}
		}
	}

	/**
	 * Get Order info from website
	 * 
	 * @param httpClient
	 * @param user
	 * @param dateStr
	 * @param summaryBuffer
	 * @return
	 * @throws Exception
	 */
	protected abstract int getOrder(CloseableHttpClient httpClient, User user, Date processDate,
			StringBuffer summaryBuffer) throws Exception;

	/**
	 * Get Receiving data
	 * 
	 * @param httpClient
	 * @param user
	 * @param dates
	 * @param summaryBuffer
	 */
	public void getReceiveData(CloseableHttpClient httpClient, User user, List<Date> dates, StringBuffer summaryBuffer) {

		for (Date processDate : dates) {
			int receivingAmount = 0;
			AccountLogTO accountLogTO = new AccountLogTO(user.getRetailer(), user.getUserId(), user.getPassword(),
					DateUtil.toString(processDate));
			try {
				summaryBuffer.append("收货单日期: " + DateUtil.toString(processDate, "yyyy-MM-dd") + "\r\n");
				receivingAmount = getReceive(httpClient, user, processDate, summaryBuffer);
				// 设置订单下载数量
				accountLogTO.setReceivingDownloadAmount(receivingAmount);
				AccountLogUtil.recordReceivingDownloadAmount(accountLogTO);
				summaryBuffer.append("收货单下载成功" + "\r\n");
			} catch (Exception e) {
				summaryBuffer.append("收货单下载失败" + "\r\n");
				getLog().error(user + "页面加载失败，请登录网站检查收货单查询功能是否正常!");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
			}
		}

	}

	/**
	 * Get Receiving info from website
	 * 
	 * @param httpClient
	 * @param user
	 * @param processDate
	 * @param summaryBuffer
	 * 
	 * @return TODO
	 * @throws Exception
	 */
	protected abstract int getReceive(CloseableHttpClient httpClient, User user, Date processDate,
			StringBuffer summaryBuffer) throws Exception;

	public void getSalesData(CloseableHttpClient httpClient, User user, List<Date> dates, StringBuffer summaryBuffer) {
		for (Date processDate : dates) {
			int salesAmount = 0;
			AccountLogTO accountLogTO = new AccountLogTO(user.getRetailer(), user.getUserId(), user.getPassword(),
					DateUtil.toString(processDate));
			try {
				// order
				salesAmount = getSales(httpClient, user, processDate, summaryBuffer);
				// 设置销售单下载数量
				accountLogTO.setSalesDownloadAmount(salesAmount);
				AccountLogUtil.recordSalesDownloadAmount(accountLogTO);
			} catch (Exception e) {
				summaryBuffer.append("销售单下载出错" + "\r\n");
				summaryBuffer.append("成功数量: " + salesAmount + "\r\n");
				getLog().error(user + "页面加载失败，请登录网站检查销售单功能是否正常！");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
			}
		}
	}

	protected abstract int getSales(CloseableHttpClient httpClient, User user, Date processDate,
			StringBuffer summaryBuffer) throws Exception;
}
