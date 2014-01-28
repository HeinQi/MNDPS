package com.rsi.mengniu.retailer.tesco.service;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import net.lingala.zip4j.exception.ZipException;

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
import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.retailer.module.TescoOrderNotifyTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

//https://tesco.chinab2bi.com/security/login.hlt
public class TescoDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(TescoDataPullService.class);
	private static Log summaryLog = LogFactory.getLog(Constants.SUMMARY_TESCO);

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HashMap<String, Object> contextMap = new HashMap<String, Object>();
		StringBuffer summaryBuffer = new StringBuffer();
		summaryBuffer.append("运行时间: " + new Date() + "\r\n");
		summaryBuffer.append("零售商: " + user.getRetailer() + "\r\n");
		summaryBuffer.append("用户: " + user.getUserId() + "\r\n");
		try {
			String loginResult = login(httpClient, user);
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

		try {
			summaryBuffer.append("收货单日期: " + DateUtil.toString(Utils.getStartDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + " - "
					+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + "\r\n");
			getReceiveExcel(httpClient, user);
			summaryBuffer.append("收货单下载成功"+"\r\n");
		} catch (Exception e) {
			summaryBuffer.append("收货单下载失败"+"\r\n");
			log.error(user + "页面加载失败，请登录网站检查收货单查询功能是否正常!");
			errorLog.error(user, e);
		}

		try {
			summaryBuffer.append("订单日期: " + DateUtil.toString(Utils.getStartDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + " - "
					+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + "\r\n");
			getOrder(httpClient, user, contextMap);
			summaryBuffer.append("订单下载成功"+"\r\n");
		} catch (Exception e) {
			log.error(user + "页面加载失败，请登录网站检查订单查询功能是否正常!");
			errorLog.error(user, e);
			summaryBuffer.append("订单下载失败" + "\r\n");
			summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
			summaryLog.info(summaryBuffer);
			return;
		}
		List<Date> dates = null;
		try {
			dates = DateUtil.getDateArrayByRange(Utils.getStartDate(Constants.RETAILER_TESCO), Utils.getEndDate(Constants.RETAILER_TESCO));
		} catch (Exception e) {
			errorLog.error(user, e);
		}

		for (Date searchDate : dates) {
			try {
				summaryBuffer.append("销售日期: " +searchDate+"\r\n");
				getSales(httpClient, user, DateUtil.toString(searchDate, "yyyy-MM-dd"), contextMap);
				summaryBuffer.append("销售数据下载成功\r\n");
			} catch (Exception e) {
				summaryBuffer.append("销售数据下载失败"+"\r\n");
				log.error(user + "页面加载失败，请登录网站检查销售数据查询功能是否正常!");
				errorLog.error(user, e);
			}
		}

		try {
			httpClient.close();
		} catch (Exception e) {
			errorLog.error(user, e);
		}
		summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
		summaryLog.info(summaryBuffer);
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("j_username", user.getUserId()));
		formParams.add(new BasicNameValuePair("j_password", user.getPassword())); // 错误的密码
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("https://tesco.chinab2bi.com/j_spring_security_check");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		loginResponse.close();
		// forward
		HttpGet httpGet = new HttpGet(loginResponse.getFirstHeader("location").getValue());
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		String loginStr = EntityUtils.toString(entity);
		if (loginStr.contains("密码错误")) {
			log.info(user + "错误的密码,退出!");
			Utils.recordIncorrectUser(user);
			return "Error";
		} else if (loginStr.contains("用户不存在")) {
			log.info(user + "用户不存在,退出!");
			Utils.recordIncorrectUser(user);
			return "Error";
		}
		if (!loginStr.contains("mainMenu.hlt")) {
			log.info(user + "系统出错,退出!");
			return "Error";
		}
		response.close();
		log.info(user + "登录成功!");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_TESCO));
		return "Success";
	}

	public void getReceiveExcel(CloseableHttpClient httpClient, User user) throws Exception {
		// https://tesco.chinab2bi.com/tesco/sellGrnQry/init.hlt
		// get vendorTaxRegistration
		log.info(user + "开始下载收货单...");
		HttpGet httpGet = new HttpGet("https://tesco.chinab2bi.com/tesco/sellGrnQry/init.hlt");
		CloseableHttpResponse formResponse = httpClient.execute(httpGet);
		HttpEntity taxEntity = formResponse.getEntity();
		Document doc = Jsoup.parse(EntityUtils.toString(taxEntity));
		Element taxElement = doc.select("#vendorTaxRegistration").first();
		formResponse.close();
		String vendorTaxRegistration = taxElement.attr("value");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_TESCO));
		// query.hlt

		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("vendorTaxRegistration", vendorTaxRegistration));// 税号
		receiveformParams.add(new BasicNameValuePair("transactionType", "01"));// 进货
		receiveformParams.add(new BasicNameValuePair("grnModel.transactionDateStart", DateUtil.toString(Utils.getStartDate(Constants.RETAILER_TESCO),
				"yyyy-MM-dd")));// 交易日期
		receiveformParams.add(new BasicNameValuePair("grnModel.transactionDateEnd", DateUtil.toString(Utils.getEndDate(Constants.RETAILER_TESCO),
				"yyyy-MM-dd"))); // 交易日期
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		HttpPost receivePost = new HttpPost("https://tesco.chinab2bi.com/tesco/sellGrnQry/exportDetail.hlt");
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost); // filename=20140108220149.zip
		String fileNm = receiveRes.getFirstHeader("Content-Disposition").getValue();
		fileNm = fileNm.substring(fileNm.indexOf("filename=") + 9);
		String receiveFilePath = Utils.getProperty(user.getRetailer() + Constants.RECEIVING_INBOUND_PATH);
		FileUtil.createFolder(receiveFilePath);
		FileOutputStream receiveFos = new FileOutputStream(receiveFilePath + fileNm);
		receiveRes.getEntity().writeTo(receiveFos);
		receiveFos.close();
		receiveRes.close();
		try{
		FileUtil.unzip(receiveFilePath + fileNm, receiveFilePath, "");
		log.info(user + "Tesco收货单Excel下载成功!");
		} catch(ZipException e){

			String receiveFileExceptionPath = Utils.getProperty(user.getRetailer() + Constants.RECEIVING_EXCEPTION_PATH);
			FileUtil.moveFile(receiveFilePath, receiveFileExceptionPath, fileNm);
			
			log.info(user + "Tesco收货单Excel下载失败!");
		}
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_TESCO));
	}

	public void getOrder(CloseableHttpClient httpClient, User user, HashMap<String, Object> contextMap) throws Exception {
		// https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQryInit.hlt
		// load search from
		log.info(user + "跳转到订单查询页面...");
		HttpGet httpGet = new HttpGet("https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQryInit.hlt");
		CloseableHttpResponse formResponse = httpClient.execute(httpGet);
		HttpEntity formEntity = formResponse.getEntity();
		Document formDoc = Jsoup.parse(EntityUtils.toString(formEntity));
		formResponse.close();
		Element parentVendorElement = formDoc.select("#parentVendor").first();
		String parentVendor = parentVendorElement.attr("value");
		contextMap.put("parentVender", parentVendor);
		List<TescoOrderNotifyTO> notifyList = new ArrayList<TescoOrderNotifyTO>();

		getNotifyList(httpClient, parentVendor, notifyList, user);
		// get order detail
		int count = 0;
		for (TescoOrderNotifyTO notify : notifyList) {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_TESCO));
			String url = "https://tesco.chinab2bi.com/tesco/sp/purOrder/pdfView.hlt?seed&fileName=" + notify.getFileName() + "&createDate="
					+ notify.getCreateDate() + "&poId=" + notify.getPoId() + "&parentVendor=" + notify.getParentVendor();
			url = url.replaceAll(" ", "%20");
			HttpGet httpOrderGet = new HttpGet(url);
			CloseableHttpResponse orderDetailResponse = httpClient.execute(httpOrderGet);
			HttpEntity orderEntity = orderDetailResponse.getEntity();
			String orderStr = EntityUtils.toString(orderEntity);
			BufferedReader br = new BufferedReader(new StringReader(orderStr));
			orderDetailResponse.close();
			count++;
			log.info(user + "成功下载订单通知明细[" + count + "]");
			readOrder(br, user);
			log.info(user + "订单数据下载成功!");
		}

	}

	private void getNotifyList(CloseableHttpClient httpClient, String parentVendor, List<TescoOrderNotifyTO> notifyList, User user) throws Exception {
		// https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQry.hlt
		log.info(user + "查询订单通知...");
		int pageNo = 1;
		int totalPages = 1;
		do {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_TESCO));
			List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
			searchformParams.add(new BasicNameValuePair("orderDateStart", DateUtil.toString(Utils.getStartDate(Constants.RETAILER_TESCO),
					"yyyy-MM-dd")));// 通知日期
			searchformParams.add(new BasicNameValuePair("orderDateEnd", DateUtil.toString(Utils.getEndDate(Constants.RETAILER_TESCO), "yyyy-MM-dd"))); // 通知日期
			searchformParams.add(new BasicNameValuePair("parentVendor", parentVendor));// parentVendor
			searchformParams.add(new BasicNameValuePair("page.pageSize", "50"));// pageSize
			searchformParams.add(new BasicNameValuePair("page.pageNo", String.valueOf(pageNo))); // pageSize
			searchformParams.add(new BasicNameValuePair("page.totalPages", String.valueOf(totalPages))); // totalPages
			searchformParams.add(new BasicNameValuePair("status", "sell"));// pageSize

			HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
			HttpPost searchPost = new HttpPost("https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQry.hlt");
			searchPost.setEntity(searchFormEntity);
			CloseableHttpResponse searchRes = httpClient.execute(searchPost);
			String searchResStr = EntityUtils.toString(searchRes.getEntity());
			searchRes.close();
			Document doc = Jsoup.parse(searchResStr);
			Elements aElements = doc.select("a[onclick^=openPDF]");
			Element eTotalPages = doc.select("#totalPages").first();
			totalPages = Integer.parseInt(eTotalPages.attr("value")); // totalPages

			for (Element aElement : aElements) {
				TescoOrderNotifyTO notify = new TescoOrderNotifyTO();
				String openPdfStr = aElement.attr("onclick");
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf("'") + 1);
				notify.setPoId(openPdfStr.substring(0, openPdfStr.indexOf("'")));
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf(", '") + 3);
				notify.setParentVendor(openPdfStr.substring(0, openPdfStr.indexOf("'")));
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf(", '") + 3);
				notify.setFileName(openPdfStr.substring(0, openPdfStr.indexOf("'")));
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf(", '") + 3);
				notify.setCreateDate(openPdfStr.substring(0, 10).replaceAll("-", ""));
				notifyList.add(notify);
			}
			pageNo++;
		} while (pageNo <= totalPages);
		log.info(user + "查询到从" + DateUtil.toString(Utils.getStartDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + "到"
				+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + ",共有" + notifyList.size() + "条订单通知");

	}

	private void readOrder(BufferedReader br, User user) throws Exception {
		log.info(user + "读取订单通知明细");
		String line = null;
		int count = 0;
		while ((line = br.readLine()) != null) {
			if (line.contains("TESCO 乐  购  商  品  订  单")) {
				line = br.readLine(); // 店别: 大连友好店 页1 页
				line = line.substring(line.indexOf("店别:  ") + 5);
				String storeNm = line.substring(0, line.indexOf(" "));
				br.readLine();
				line = br.readLine();// 订单号码 17040620 促销期数 紧急订单
				String orderNo = line.substring(line.indexOf("订单号码") + 6, line.indexOf("促销期数")).trim();
				line = br.readLine(); // 订单日期 2014/01/05 交货日期 2014/01/06 订单类型 -DSD PO-
				if (!line.contains("DSD PO")) {
					continue;
				}

				String orderDate = line.substring(line.indexOf("订单日期") + 6, line.indexOf("交货日期")).trim();
				for (int i = 0; i < 10; i++) {
					br.readLine();
				}
				List<OrderTO> orderItems = new ArrayList<OrderTO>();
				readOrderItem(br, orderItems, storeNm, orderNo, orderDate);
				FileUtil.exportOrderInfoToTXT("tesco", orderNo, orderItems);
				log.info(user + "成功读取订单,订单号:" + orderNo);
				count++;
			}
		}
		log.info(user + "此订单通知明细共有" + count + "订单");
	}

	private void readOrderItem(BufferedReader br, List<OrderTO> orderItems, String storeNm, String orderNo, String orderDate) throws IOException {
		// 103933911/ SP_24_蒙牛冠益乳酸牛奶 瓶 箱 24 4.74 113.85 341.54 3 3 72 1 1 N
		String line = null;
		while ((line = br.readLine()).length() > 0) {
			OrderTO orderTo = new OrderTO();
			orderTo.setStoreName(storeNm);
			orderTo.setOrderNo(orderNo);
			orderTo.setOrderDate(orderDate);
			String itemId = line.substring(1, 10);
			line = line.substring(17);
			String itemName = line.substring(0, line.indexOf(" "));
			line = line.substring(line.indexOf(" ")).trim(); // 规格
			line = line.substring(line.indexOf(" ")).trim();// 订购单位
			line = line.substring(line.indexOf(" ")).trim();// 箱入数
			line = line.substring(line.indexOf(" ")).trim();// 单件成本
			line = line.substring(line.indexOf(" ")).trim();// 成本
			String unitPrice = line.substring(0, line.indexOf(" ")).trim();
			line = line.substring(line.indexOf(" ")).trim();// 总成本
			String totalPrice = line.substring(0, line.indexOf(" "));
			line = line.substring(line.indexOf(" ")).trim();// 订购数量
			String quantity = line.substring(0, line.indexOf(" "));
			line = br.readLine();
			line = line.substring(17);
			String itemNm2 = line.substring(0, line.indexOf(" "));
			line = line.substring(line.indexOf(" ")).trim(); // 国际条码
			String barcode = line.substring(0, line.indexOf(" "));
			orderTo.setItemID(itemId);
			orderTo.setItemName(itemName + itemNm2);
			orderTo.setUnitPrice(unitPrice);
			orderTo.setTotalPrice(totalPrice);
			orderTo.setQuantity(quantity);
			orderTo.setBarcode(barcode);
			orderItems.add(orderTo);
		}

	}

	public void getSales(CloseableHttpClient httpClient, User user, String searchDate, HashMap<String, Object> contextMap) throws Exception {
		log.info(user + "查询销售数据...");
		int pageNo = 1;
		int totalPages = 1;
		String parentVendor = (String) contextMap.get("parentVendor");
		List<SalesTO> salesList = new ArrayList<SalesTO>();
		do {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_TESCO));
			List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
			searchformParams.add(new BasicNameValuePair("tranDate", searchDate));// 查询日期
			searchformParams.add(new BasicNameValuePair("dateType", "1"));// 查询日期
			searchformParams.add(new BasicNameValuePair("parentVendor", parentVendor));// parentVendor
			searchformParams.add(new BasicNameValuePair("page.pageNo", String.valueOf(pageNo))); // pageSize
			searchformParams.add(new BasicNameValuePair("page.totalPages", String.valueOf(totalPages))); // totalPages
			searchformParams.add(new BasicNameValuePair("status", "sell"));// pageSize

			HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
			HttpPost searchPost = new HttpPost("https://tesco.chinab2bi.com/tesco/sp/saleStock/query.hlt");
			searchPost.setEntity(searchFormEntity);
			CloseableHttpResponse searchRes = httpClient.execute(searchPost);
			String searchResStr = EntityUtils.toString(searchRes.getEntity());
			searchRes.close();
			if (searchResStr.contains("查询到0条记录")) {
				log.info(user + "没有查到 " + searchDate + " 的销售数据");
				return;
			}
			String recordStr = searchResStr.substring(0, searchResStr.lastIndexOf("条记录"));
			recordStr = recordStr.substring(recordStr.lastIndexOf("共") + 1);
			Document doc = Jsoup.parse(searchResStr);
			Element eTotalPages = doc.select("#totalPages").first();
			totalPages = Integer.parseInt(eTotalPages.attr("value")); // totalPages
			log.info(user + "查到" + searchDate + "销售记录共" + recordStr + "条,当前第" + pageNo + "页,共" + totalPages + "页");

			Element tableElement = doc.select("table#row").first();
			Elements rowElements = tableElement.select("tr[class]");
			for (Element row : rowElements) {
				Elements tds = row.select("td");
				SalesTO salesTo = new SalesTO();
				salesTo.setSalesDate(searchDate);
				salesTo.setItemID(tds.get(2).text());
				salesTo.setItemName(tds.get(4).text());
				salesTo.setStoreID(tds.get(6).text());
				salesTo.setSalesQuantity(tds.get(8).text());
				salesTo.setSalesAmount(tds.get(9).text());
				salesList.add(salesTo);
				log.info(user + "成功读取销售数据第" + salesList.size() + "条");
			}
			pageNo++;
		} while (pageNo <= totalPages);

		Utils.exportSalesInfoToTXT(Constants.RETAILER_TESCO, user.getUserId(), DateUtil.toDate(searchDate, "yyyy-MM-dd"), salesList);
		// log.info(user + "查询到从" + DateUtil.toString(Utils.getStartDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + "到"
		// + DateUtil.toString(Utils.getEndDate(Constants.RETAILER_TESCO), "yyyy-MM-dd") + ",共有" + notifyList.size() + "条订单通知");
		log.info(user + "销售数据下载成功");
	}
}
