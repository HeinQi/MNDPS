package com.rsi.mengniu.retailer.carrefour.service;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.DataPullTaskPool;
import com.rsi.mengniu.retailer.common.service.RetailerDataPullServiceImpl;
import com.rsi.mengniu.retailer.module.AccountLogTO;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.AccountLogUtil;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.OCR;
import com.rsi.mengniu.util.Utils;

//http://supplierweb.carrefour.com.cn/
public class CarrefourDataPullService extends RetailerDataPullServiceImpl {
	private static Log log = LogFactory.getLog(CarrefourDataPullService.class);
	private static Log summaryLog = LogFactory.getLog(Constants.SUMMARY_CARREFOUR);
	private OCR ocr;


	public String loginDetail(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		HttpGet httpGet = new HttpGet("http://supplierweb.carrefour.com.cn/includes/image.jsp");
		String imgName = String.valueOf(java.lang.System.currentTimeMillis());
		String validateImgPath = Utils.getProperty(Constants.TEMP_PATH);
		FileUtil.createFolder(validateImgPath);
		FileOutputStream fos = new FileOutputStream(validateImgPath + imgName + ".jpg");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		entity.writeTo(fos);
		response.close();
		fos.close();
		String recognizeStr = ocr.recognizeText(validateImgPath + imgName + ".jpg", validateImgPath + imgName, true);
		// login /login.do?action=doLogin
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("login", user.getUserId()));
		formParams.add(new BasicNameValuePair("password", user.getPassword())); // 错误的密码
		formParams.add(new BasicNameValuePair("validate", recognizeStr));
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("http://supplierweb.carrefour.com.cn/login.do?action=doLogin");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		String responseStr = EntityUtils.toString(loginResponse.getEntity());
		if (responseStr == null) {
			log.info(user + "网站登录出错,退出!");
			return "SystemError";
		}
		if (responseStr.contains("验证码失效")) {
			log.info(user + "验证码失效,Relogin...");
			return "InvalidCode";
		} else if (responseStr.contains("错误的密码")) {
			log.info(user + "错误的密码,退出!");
			Utils.recordIncorrectUser(user);
			return "InvalidPassword";
		} else if (responseStr.contains("错误的用户名")) {
			log.info(user + "错误的用户名,退出!");
			Utils.recordIncorrectUser(user);
			return "InvalidPassword";
		} else if (responseStr.contains("系统出错") || !responseStr.contains("Welcome")) {
			log.info(user + "网站登录出错,退出!");
			return "SystemError";
		}
		loginResponse.close();
		log.info(user + "登录成功!");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_CARREFOUR));
		return "Success";
	}
	
	public void getReceiveData(CloseableHttpClient httpClient, User user, List<Date> dates, StringBuffer summaryBuffer) {
		try {

			// receive
			String fileFullPath = getReceiveExcel(httpClient, user, summaryBuffer);

			// 计算Receiving下载成功的数量
			Map<String, Set<String>> receivingMapByDate = AccountLogUtil.getReceivingAmountFromFileForCarrefour(
					fileFullPath, Utils.getStartDate(Constants.RETAILER_CARREFOUR),
					Utils.getEndDate(Constants.RETAILER_CARREFOUR));

			for (String processDateStr : receivingMapByDate.keySet()) {
				AccountLogTO accountLogTO = new AccountLogTO(user.getRetailer(), user.getUserId(), user.getPassword(),
						processDateStr);
				accountLogTO.setReceivingDownloadAmount(receivingMapByDate.get(processDateStr).size());
				AccountLogUtil.recordReceivingDownloadAmount(accountLogTO);
			}
		} catch (Exception e) {
			summaryBuffer.append("收货单下载失败" + "\r\n");
			getLog().error(user + "页面加载失败，请登录网站检查收货单功能是否正常！");
			errorLog.error(user, e);
			DataPullTaskPool.addFailedUser(user);
			// getLog().error(user + Utils.getTrace(e));
		}

	}

	public String getReceiveExcel(CloseableHttpClient httpClient, User user, StringBuffer summaryBuffer)
			throws Exception {
		String receiveFileNm = "Receiving_" + user.getRetailer() + "_" + user.getUserId() + "_"
				+ DateUtil.toStringYYYYMMDD(new Date()) + ".xls";
		String receiveFilePath = Utils.getProperty(user.getRetailer() + Constants.RECEIVING_INBOUND_PATH);

		if (Utils.isReceivingFileExist(user.getRetailer(), receiveFileNm)) {
			log.info(user + "收货单已存在,不再下载");
			return null;
		}
		log.info(user + "开始下载收货单...");
		// goMenu('inyr.do?action=query','14','预估进退查询')

		// $('inyrForm').action="inyr.do?action=export";
		// 供应商预估进退查询/Supplier Inyr Inquiry
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("unitid", "ALL"));
		receiveformParams.add(new BasicNameValuePair("butype", "byjv"));
		receiveformParams.add(new BasicNameValuePair("systemdate", DateUtil.toString(
				Utils.getEndDate(Constants.RETAILER_CARREFOUR), "yyyy/MM/dd"))); // yyyy/mm/dd
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		HttpPost receivePost = new HttpPost("http://supplierweb.carrefour.com.cn/inyr.do?action=export");
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr = EntityUtils.toString(receiveRes.getEntity());
		receiveRes.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_CARREFOUR));
		if (responseStr.contains("Excel文档生成成功")) {
			// Download Excel file
			responseStr = responseStr.substring(responseStr.indexOf("javascript:downloads('") + 22);
			String inyrFileName = responseStr.substring(0, responseStr.indexOf("'"));
			FileUtil.createFolder(receiveFilePath);
			FileOutputStream receiveFos = new FileOutputStream(receiveFilePath + receiveFileNm);

			List<NameValuePair> downloadformParams = new ArrayList<NameValuePair>();
			downloadformParams.add(new BasicNameValuePair("filename", inyrFileName));
			downloadformParams.add(new BasicNameValuePair("filenamedownload", "excelpath"));
			HttpEntity downloadFormEntity = new UrlEncodedFormEntity(downloadformParams, "UTF-8");
			HttpPost downloadPost = new HttpPost("http://supplierweb.carrefour.com.cn/download.jsp");

			downloadPost.setEntity(downloadFormEntity);
			CloseableHttpResponse downloadRes = httpClient.execute(downloadPost);
			downloadRes.getEntity().writeTo(receiveFos);
			downloadRes.close();
			receiveFos.close();
			log.info(user + "家乐福收货单Excel下载成功!");
			summaryBuffer.append("收货单日期: "
					+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_CARREFOUR), "yyyy-MM-dd") + "\r\n");
			summaryBuffer.append("收货单下载成功" + "\r\n");
			summaryBuffer.append("文件: " + receiveFileNm + "\r\n");
		} else {
			log.info(user + "家乐福收货单Excel下载失败!");
			summaryBuffer.append("收货单下载失败" + "\r\n");
		}
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_CARREFOUR));

		return receiveFilePath + receiveFileNm;

	}

	@Override
	public int getOrder(CloseableHttpClient httpClient, User user, Date processDate,StringBuffer summaryBuffer
			) throws Exception {
		String processDateStr = DateUtil.toString(processDate, "dd-MM-yyyy");
		Set<String> orderNoSet = new HashSet<String>();
		if (Utils.isOrderFileExistForCarrefour(Constants.RETAILER_CARREFOUR, user.getUserId(),
				processDate)) {
			log.info(user + "订单日期: " + processDate + "的订单已存在,不再下载");
			return 0;
		}

		log.info(user + "跳转到订单查询页面...");
		summaryBuffer.append("订单日期: " + processDate + "\r\n");
		// forward to PowerE2E Platform
		HttpGet httpGet = new HttpGet("https://supplierweb.carrefour.com.cn/callSSO.jsp");

		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		if (!EntityUtils.toString(entity).contains("PowerE2E Platform")) {
			log.info(user + "订单查询页面加载出错,cannot forward to PowerE2E Platform");
			summaryBuffer.append("订单下载出错" + "\r\n");
			summaryBuffer.append("成功数量: 无\r\n");
			throw new Exception("订单查询页面加载出错");
			// return;
		}
		response.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_CARREFOUR));

		// https://platform.powere2e.com/platform/mailbox/openInbox.htm?
		List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
		searchformParams.add(new BasicNameValuePair("receivedDateFrom", processDateStr)); // "01-12-2013"
		searchformParams.add(new BasicNameValuePair("receivedDateTo", processDateStr));
		HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
		HttpPost searchPost = new HttpPost("https://platform.powere2e.com/platform/mailbox/openInbox.htm?");

		searchPost.setEntity(searchFormEntity);
		CloseableHttpResponse searchRes = httpClient.execute(searchPost);
		String responseStr = EntityUtils.toString(searchRes.getEntity());
		searchRes.close();
		Document doc = Jsoup.parse(responseStr);
		Element mailboxForm = doc.select("form[name=mailboxForm]").first();
		Element table = mailboxForm.select("table").first();

		String recordStr = table.select("tr[align=right]").select("td").get(0).text();
		if (recordStr == null || recordStr.equals("")) {
			log.info(user + "订单日期" + processDate + "记录为 0");
			return 0;
		}
		recordStr = recordStr.substring(recordStr.indexOf("共") + 1, recordStr.indexOf("记"));
		recordStr = recordStr.replaceAll(",", "");
		int record = Integer.parseInt(recordStr);
		int page = record % 10 > 0 ? record / 10 + 1 : record / 10;
		log.info(user + "查询到日期: " + processDate + ",共有" + record + "笔订单");
		Elements msgIds = doc.select("input[name=msgId]");
		List<String> msgIdList = new ArrayList<String>();
		for (Element msgId : msgIds) {
			msgIdList.add(msgId.attr("value"));
		}
		// 取得每页的MsgId
		while (page > 1) {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_CARREFOUR));
			getMsgIdByPage(page, msgIdList, httpClient, processDateStr);
			page--;
		}
		int count = 0;
		List<OrderTO> orderItems = new ArrayList<OrderTO>();
		for (String msgId : msgIdList) {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_CARREFOUR));
			HttpGet httpOrderGet = new HttpGet(
					"https://platform.powere2e.com/platform/mailbox/performDocAction.htm?actionId=1&guid=" + msgId);

			CloseableHttpResponse orderRes = httpClient.execute(httpOrderGet);
			String orderDetail = EntityUtils.toString(orderRes.getEntity());
			orderRes.close();
			if (!orderDetail.contains("Carrefour Purchase Order")) {
				continue;
			}

			Document orderDoc = Jsoup.parse(orderDetail);
			Element orderTable = orderDoc.select("table.tab2").first();
			String storeName = orderTable.select("tr:eq(1)").select("td").get(0).text();// store
			String orderNo = orderTable.select("tr:eq(2)").select("td").get(1).text();// 订单号码
			String orderDate = orderTable.select("tr:eq(3)").select("td").get(1).text();// 订单日期时间
			Element orderItemTable = orderDoc.select("table.tab2").last();

			for (Element row : orderItemTable.select("tr:gt(2)")) {
				Elements tds = row.select("td");
				OrderTO orderTo = new OrderTO();
				orderTo.setStoreName(storeName);
				orderTo.setOrderNo(orderNo);
				orderTo.setOrderDate(orderDate);
				orderTo.setItemID(tds.get(0).select("span").first().text());// 单品号
				orderTo.setBarcode(tds.get(1).select("span").first().text()); // 条形码
				orderTo.setItemName(tds.get(2).text());// 单品名称
				orderTo.setQuantity(tds.get(6).text());// 总计数量
				orderTo.setUnitPrice(tds.get(7).text()); // 单价
				orderTo.setTotalPrice(tds.get(8).text());// 总金额
				orderTo.setUserID(user.getUserId());
				orderItems.add(orderTo);
				// 计算成功的订单数量 以订单号为单位
				orderNoSet.add(orderNo);

			}
			count++;
//			orderCount.increaseOne();
			log.info(user + "成功下载订单[" + count + "],订单号:" + orderNo);
		}
		// FileUtil.exportOrderInfoToTXT("carrefour", orderNo, orderItems);
		Utils.exportOrderInfoListToTXT(Constants.RETAILER_CARREFOUR, user.getUserId(),
				processDate, orderItems);
		log.info(user + "订单数据下载成功!");
		summaryBuffer.append("订单下载成功" + "\r\n");
		summaryBuffer.append("数量: " + orderNoSet.size() + "\r\n");

		return orderNoSet.size();
	}

	// function gotoPage(page) {
	// document.forms[0].action =
	// "/platform/mailbox/navigateInbox.htm?gotoPage="+ page;
	// document.forms[0].submit();
	// }
	private void getMsgIdByPage(int page, List<String> msgIdList, CloseableHttpClient httpClient, String searchDate)
			throws Exception {
		List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
		searchformParams.add(new BasicNameValuePair("receivedDateFrom", searchDate));
		searchformParams.add(new BasicNameValuePair("receivedDateTo", searchDate));
		HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
		HttpPost searchPost = new HttpPost("https://platform.powere2e.com/platform/mailbox/navigateInbox.htm?gotoPage="
				+ page);

		searchPost.setEntity(searchFormEntity);
		CloseableHttpResponse searchRes = httpClient.execute(searchPost);
		String responseStr = EntityUtils.toString(searchRes.getEntity());
		searchRes.close();
		Document doc = Jsoup.parse(responseStr);
		Elements msgIds = doc.select("input[name=msgId]");
		for (Element msgId : msgIds) {
			msgIdList.add(msgId.attr("value"));
		}
	}

	public OCR getOcr() {
		return ocr;
	}

	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}

	@Override
	protected Log getLog() {
	
		return log;
	}

	@Override
	protected Log getSummaryLog() {
		return summaryLog;
	}
	
	public String getRetailerID() {

		return Constants.RETAILER_CARREFOUR;
	}

	@Override
	protected int getReceive(CloseableHttpClient httpClient, User user, Date processDate, StringBuffer summaryBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getSales(CloseableHttpClient httpClient, User user, Date processDate, StringBuffer summaryBuffer) {
		// TODO Auto-generated method stub
		return 0;
	}
}
