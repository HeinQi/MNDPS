package com.rsi.mengniu.retailer.renrenle.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.DataPullTaskPool;
import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.OCR;
import com.rsi.mengniu.util.Utils;

//http://www.renrenle.cn/scm/welcome.do
public class RenrenleDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(RenrenleDataPullService.class);
	private static Log summaryLog = LogFactory.getLog(Constants.SUMMARY_RENRENLE);
	private OCR ocr;

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		StringBuffer summaryBuffer = new StringBuffer();
		summaryBuffer.append("运行时间: " + new Date() + "\r\n");
		summaryBuffer.append("零售商: " + user.getRetailer() + "\r\n");
		summaryBuffer.append("用户: " + user.getUserId() + "\r\n");
		String loginResult = null;
		int loginCount = 0; // 如果验证码出错重新login,最多15次
		try {
			do {
				loginResult = login(httpClient, user);
				loginCount++;
			} while ("InvalidCode".equals(loginResult) && loginCount < 15);
			// Invalid Password and others
			if (!"Success".equals(loginResult)) {
				summaryBuffer.append("登录失败!\r\n");
				summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
				summaryLog.info(summaryBuffer);
				return;
			}
		} catch (Exception e) {
			log.error(user + "网站登录出错,请检查!");
			errorLog.error(user, e);
			summaryBuffer.append("登录失败!\r\n");
			summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
			summaryLog.info(summaryBuffer);
			return;
		}
		summaryBuffer.append(Constants.SUMMARY_TITLE_SALES + "\r\n");

		List<Date> dates = DateUtil.getDateArrayByRange(Utils.getStartDate(Constants.RETAILER_RENRENLE),
				Utils.getEndDate(Constants.RETAILER_RENRENLE));

		for (Date searchDate : dates) {
			try {
				summaryBuffer.append("销售日期: " + searchDate + "\r\n");
				getSalesExcel(httpClient, user, DateUtil.toString(searchDate, "yyyy-MM-dd"), summaryBuffer);
			} catch (Exception e) {
				summaryBuffer.append("销售数据下载失败" + "\r\n");
				log.error(user + "页面加载失败，请登录网站检查销售数据查询功能是否正常!");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
			}
		}

		summaryBuffer.append(Constants.SUMMARY_TITLE_ORDER + "\r\n");
		for (Date searchDate : dates) {
			try {
				summaryBuffer.append("订单日期: " + searchDate + "\r\n");
				getOrders(httpClient, user, DateUtil.toString(searchDate, "yyyy-MM-dd"));
				summaryBuffer.append("订单下载成功" + "\r\n");
			} catch (Exception e) {
				summaryBuffer.append("订单下载失败" + "\r\n");
				log.error(user + "页面加载失败，请登录网站检查订单查询功能是否正常!");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
			}
		}
		try {
			httpClient.close();
		} catch (IOException e) {
			errorLog.error(user, e);
		}

		summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
		summaryLog.info(summaryBuffer);

	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));
		HttpGet httpGet = new HttpGet("http://www.renrenle.cn/scm/verifyCode.jsp");
		String imgName = String.valueOf(java.lang.System.currentTimeMillis());
		String validateImgPath = Utils.getProperty(Constants.TEMP_PATH);
		FileUtil.createFolder(validateImgPath);
		FileOutputStream fos = new FileOutputStream(validateImgPath + imgName + ".jpg");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		entity.writeTo(fos);
		response.close();
		fos.close();
		String recognizeStr = ocr.recognizeText(validateImgPath + imgName + ".jpg", validateImgPath + imgName, false);
		// login /login.do?action=doLogin
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("name", user.getUserId()));
		formParams.add(new BasicNameValuePair("password", user.getPassword()));
		formParams.add(new BasicNameValuePair("shopID", "RRL001")); // 必须选人人乐集团
		formParams.add(new BasicNameValuePair("verifyCode", recognizeStr));
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("http://www.renrenle.cn/scm/login.do?method=login");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		String responseStr = new String(EntityUtils.toString(loginResponse.getEntity()).getBytes("ISO_8859_1"), "GBK");
		loginResponse.close();
		if (responseStr.contains("验证码输入不正确")) {
			log.info(user + "验证码输入不正确,Relogin...");
			return "InvalidCode";
		} else if (responseStr.contains("您的用户名或密码输入有误")) {
			log.info(user + "您的用户名或密码输入有误,退出!");
			Utils.recordIncorrectUser(user);
			return "InvalidPassword";
		} else if (!responseStr.contains("mainAction")) {
			log.info(user + "系统出错,退出!");
			return "SystemError";
		}
		log.info(user + "登录成功!");
		return "Success";
	}

	public void getSalesExcel(CloseableHttpClient httpClient, User user, String searchDate, StringBuffer summaryBuffer)
			throws Exception {
		if (Utils.isSalesFileExist(Constants.RETAILER_RENRENLE, user.getUserId(), DateUtil.toDate(searchDate,"yyyy-MM-dd"))) {
			log.info(user+"销售日期: "+searchDate+"的销售数据已存在,不再下载");
			return;
		}
		
		log.info(user + "开始下载" + searchDate + "的销售数据...");

		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));
		// /scm/jump.do?prefix=/sale&page=/saleAction.do?method=querySale&left=1&forward=byShopList&moduleID=401
		// 销售查询
		HttpGet httpGet = new HttpGet(
				"http://www.renrenle.cn/scm/jump.do?prefix=/sale&page=/saleAction.do?method=querySale&left=1&forward=byShopList&moduleID=401");
		CloseableHttpResponse searchRes = httpClient.execute(httpGet);
		HttpEntity searchEntity = searchRes.getEntity();
		String searchStr = new String(EntityUtils.toString(searchEntity).getBytes("ISO_8859_1"), "GBK");
		searchRes.close();
		Document searchDoc = Jsoup.parse(searchStr);
		Element token = searchDoc.select("input[name=org.apache.struts.taglib.html.TOKEN]").first();
		String tokenStr = token.attr("value");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));

		// http://www.renrenle.cn/scm/sale/saleAction.do?method=querySale&download=1
		String salesFilePath = Utils.getProperty(Constants.RETAILER_RENRENLE + Constants.SALES_INBOUND_PATH);
		FileUtil.createFolder(salesFilePath);
		String receiveFileNm = "Sales_" + Constants.RETAILER_RENRENLE + "_" + user.getUserId() + "_"
				+ searchDate.replaceAll("-", "") + ".xls";
		FileOutputStream salseFos = new FileOutputStream(salesFilePath + receiveFileNm);

		List<NameValuePair> salesformParams = new ArrayList<NameValuePair>();
		salesformParams.add(new BasicNameValuePair("searchDate", searchDate));
		salesformParams.add(new BasicNameValuePair("searchType", "0"));
		salesformParams.add(new BasicNameValuePair("saleType", "0"));
		salesformParams.add(new BasicNameValuePair("orderASC", "false"));
		salesformParams.add(new BasicNameValuePair("forward", "byShopList"));
		salesformParams.add(new BasicNameValuePair("org.apache.struts.taglib.html.TOKEN", tokenStr));
		HttpEntity salesFormEntity = new UrlEncodedFormEntity(salesformParams, "UTF-8");
		HttpPost salesPost = new HttpPost("http://www.renrenle.cn/scm/sale/saleAction.do?method=querySale&download=1");
		salesPost.setEntity(salesFormEntity);
		CloseableHttpResponse downloadRes = httpClient.execute(salesPost);
		downloadRes.getEntity().writeTo(salseFos);
		downloadRes.close();
		salseFos.close();

		summaryBuffer.append("销售单下载成功" + "\r\n");
		summaryBuffer.append("文件: " + receiveFileNm + "\r\n");
		log.info(user + searchDate + "的销售数据下载成功");
	}

	public void getOrders(CloseableHttpClient httpClient, User user, String searchDate) throws Exception {
		log.info(user + "开始下载" + searchDate + "的订单数据...");

		int pageNo = 1;
		int totalPages = 1;
		List<String> orderIdList = new ArrayList<String>();
		do {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));
			List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
			searchformParams.add(new BasicNameValuePair("startDate", searchDate));
			searchformParams.add(new BasicNameValuePair("endDate", searchDate));
			searchformParams.add(new BasicNameValuePair("activeFlag", "1"));
			searchformParams.add(new BasicNameValuePair("flag", "-1"));
			searchformParams.add(new BasicNameValuePair("purchaseFlag", "-1"));
			searchformParams.add(new BasicNameValuePair("orderASC", "false"));
			searchformParams.add(new BasicNameValuePair("thisPage", String.valueOf(pageNo)));

			HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
			HttpPost searchPost = new HttpPost("http://www.renrenle.cn/scm/order/orderAction.do?method=queryOrder");
			searchPost.setEntity(searchFormEntity);
			CloseableHttpResponse response = httpClient.execute(searchPost);
			String responseStr = new String(EntityUtils.toString(response.getEntity()).getBytes("ISO_8859_1"), "GBK");
			response.close();
			if (!responseStr.contains("泰斯玛供应链关系管理系统")) {
				log.error(user + "订单查询失败,请登录网站检查");
				DataPullTaskPool.addFailedUser(user);
				return;
			}
			String totalPageStr = responseStr.substring(responseStr.indexOf("</B>/<B>") + 8);
			totalPageStr = totalPageStr.substring(0, totalPageStr.indexOf("</B>"));
			totalPages = Integer.parseInt(totalPageStr);
			Document doc = Jsoup.parse(responseStr);
			Element table = doc.select("table.box_table1").first();
			Elements orderIds = table.select("a[href^=orderAction]");
			for (Element eOrderId : orderIds) {
				String orderId = eOrderId.text();
				orderIdList.add(orderId);
			}
			pageNo++;
		} while (pageNo <= totalPages);

		log.info(user + "查询到" + searchDate + "号订单共" + orderIdList.size() + "条");
		for (int i = 0; i < orderIdList.size(); i++) {
			String orderId = orderIdList.get(i);
			if (Utils.isOrderFileExist(Constants.RETAILER_RENRENLE, user.getUserId(),orderId, DateUtil.toDate(searchDate,"yyyy-MM-dd"))) {
				log.info(user+"订单日期: "+searchDate+" 订单号: "+orderId+"已存在,不再下载");
				continue;
			}
			
			List<OrderTO> orderList = new ArrayList<OrderTO>();
			getOrderDetail(httpClient, user, orderId, searchDate, orderList);
			Utils.exportOrderInfoToTXT(Constants.RETAILER_RENRENLE, user.getUserId(), orderId,
					DateUtil.toDate(searchDate, "yyyy-MM-dd"), orderList);
			log.info(user + "成功获取第" + (i + 1) + "条订单,订单号为" + orderId);
		}
		log.info(user + searchDate + "的订单数据下载成功");
	}

	private void getOrderDetail(CloseableHttpClient httpClient, User user, String orderId, String searchDate,
			List<OrderTO> orderList) throws Exception {
		// /scm/order/orderAction.do?method=printOrder&sheetID=A002201401024822
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));
		String url = "http://www.renrenle.cn/scm/order/orderAction.do?method=printOrder&sheetID=" + orderId;
		HttpGet httpOrderGet = new HttpGet(url);
		CloseableHttpResponse orderDetailResponse = httpClient.execute(httpOrderGet);
		HttpEntity orderEntity = orderDetailResponse.getEntity();
		String orderStr = new String(EntityUtils.toString(orderEntity).getBytes("ISO_8859_1"), "GBK");
		if (!orderStr.contains(orderId)) {
			log.error(user + "获取订单失败订单号为" + orderId + ",请登录网站检查");
			DataPullTaskPool.addFailedUser(user);
			return;
		}
		Document orderDoc = Jsoup.parse(orderStr);
		Element dataTable = orderDoc.select("table.print_box_table1").last();
		Element storeRow = dataTable.select("tr:eq(2)").first();
		String storeName = storeRow.select("td").get(3).text();
		Elements rows = dataTable.select("tr:gt(5)");
		for (int i = 0; i < rows.size() - 1; i++) {
			Elements tds = rows.get(i).select("td");
			OrderTO orderTo = new OrderTO();
			orderTo.setOrderNo(orderId);
			orderTo.setOrderDate(searchDate);
			orderTo.setStoreID(storeName.substring(0,4));
			orderTo.setStoreName(storeName);
			orderTo.setItemName(tds.get(0).text());
			orderTo.setItemID(tds.get(1).text());
			orderTo.setBarcode(tds.get(2).text());
			orderTo.setUnitPrice(tds.get(7).text());
			orderTo.setQuantity(tds.get(10).text());
			orderList.add(orderTo);
		}
	}

	public OCR getOcr() {
		return ocr;
	}

	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}
}
