package com.rsi.mengniu.retailer.rainbow.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.common.service.RetailerDataPullServiceImpl;
import com.rsi.mengniu.retailer.module.RainbowReceivingTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class RainbowDataPullService extends RetailerDataPullServiceImpl {

	private static Log log = LogFactory.getLog(RainbowDataPullService.class);

	private static Log summaryLog = LogFactory.getLog(Constants.SUMMARY_RAINBOW);

//	public void dataPull(User user) {
//		CloseableHttpClient httpClient = Utils.createHttpClient();
//		StringBuffer summaryBuffer = new StringBuffer();
//
//		summaryBuffer.append("\r\n");
//		summaryBuffer.append("运行时间：" + new Date() + "\r\n");
//		summaryBuffer.append("零售商：" + user.getRetailer() + "\r\n");
//		summaryBuffer.append("用户：" + user.getUserId() + " \r\n");
//		summaryBuffer.append("\r\n");
//
//		try {
//
//			String returnType = this.login(httpClient, user);
//
//			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
//			if (!"Success".equals(returnType)) {
//				return;
//			}
//
//		} catch (Exception e) {
//			log.error(user + Utils.getTrace(e));
//			DataPullTaskPool.addFailedUser(user);
//			return;
//		}
//		List<Date> dateList = null;
//		try {
//			dateList = DateUtil.getDateArrayByRange(Utils.getStartDate(user.getRetailer()),
//					Utils.getEndDate(user.getRetailer()));
//		} catch (Exception e1) {
//			log.error("日期转换出错");
//			errorLog.info("日期转换出错", e1);
//
//		}
//		summaryBuffer.append(Constants.SUMMARY_TITLE_RECEIVING + "\r\n");
//
//		for (Date receivingDate : dateList) {
//			CountTO countTO = new CountTO();
//
//			String receivingDateStr = DateUtil.toString(receivingDate);
//			summaryBuffer.append("收货单日期：" + receivingDateStr + " \r\n");
//
//			try {
//				this.getReceive(httpClient, user, receivingDate, summaryBuffer);
//				summaryBuffer.append("收货单下载成功" + "\r\n");
//				summaryBuffer.append("数量：" + countTO.getCounttotalNo() + " \r\n");
//			} catch (Exception e) {
//				summaryBuffer.append("收货单下载失败!" + "\r\n");
//				log.error(user + "页面加载失败，请登录网站检查收货单查询功能是否正常!");
//				errorLog.error(user, e);
//				DataPullTaskPool.addFailedUser(user);
//			}
//		}
//		summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
//
//		summaryBuffer.append(Constants.SUMMARY_TITLE_SALES + "\r\n");
//
//		for (Date salesDate : dateList) {
//
//			String salesDateStr = DateUtil.toString(salesDate);
//			summaryBuffer.append("销售日期：" + salesDateStr + " \r\n");
//			
//				try {
//
//					this.getSales(httpClient, user, salesDate, summaryBuffer);
//
//					summaryBuffer.append("销售单下载成功" + " \r\n");
//				} catch (Exception e) {
//					summaryBuffer.append("销售单下载失败!" + "\r\n");
//					log.error(user + "页面加载失败，请登录网站检查销售数据查询功能是否正常!");
//					errorLog.error(user, e);
//					DataPullTaskPool.addFailedUser(user);
//				}
//
//		}
//		summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE + "\r\n");
//
//		summaryLog.info(summaryBuffer);
//
//		try {
//			httpClient.close();
//		} catch (IOException e) {
//			errorLog.error(user, e);
//		}
//	}

	protected String loginDetail(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("loginfile", "/login/Login.jsp?logintype=1"));
		formParams.add(new BasicNameValuePair("logintype", "1"));
		formParams.add(new BasicNameValuePair("loginid", user.getUserId()));
		formParams.add(new BasicNameValuePair("userpassword", user.getPassword())); // 错误的密码
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");

		HttpPost httppost = new HttpPost("http://vd.rainbow.cn:8080/login/VerifyLogin.jsp");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);

		loginResponse.close();

		// forward
		log.info(loginResponse.getFirstHeader("location").getValue());
		HttpGet httpGet = new HttpGet(loginResponse.getFirstHeader("location").getValue());
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		String forwardStr = EntityUtils.toString(entity);

		if (forwardStr.contains("用户名或密码错误")) {
			log.info(user + "用户名或密码错误,退出!");
			Utils.recordIncorrectUser(user);
			return "Error";
		}

		if (!forwardStr.contains("欢迎您")) {
			log.info(user + "系统出错,退出!");
			return "Error";
		}
		response.close();
		log.info(user + "登录成功!");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
		return "Success";
	}


	@Override
	protected int getReceive(CloseableHttpClient httpClient, User user, Date receivingDate, StringBuffer summaryBuffer) throws Exception {

		log.info(user + "开始下载收货单...");
		if (Utils.isReceivingFileExist(user.getRetailer(), user.getUserId(), receivingDate)) {
			log.info(user + "收货单已存在,不再下载");
			return 0;
		}

		String receivingDateStr = DateUtil.toString(receivingDate);
		log.info(user + "收货单日期：" + receivingDateStr);
		List<ReceivingNoteTO> receivingList = new ArrayList<ReceivingNoteTO>();

		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
		HttpGet httpGet = new HttpGet("http://vd.rainbow.cn:8080/object/getAdvReportData.jsp" + "?start=0"
				+ "&limit=20" + "&rptId=3124" + "&showSelectionModel=false" + "&html=Y"
				+ "&sqlWhere=&where=%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3E=to_date('" + receivingDateStr
				+ "','yyyy-mm-dd')%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3C=to_date('" + receivingDateStr
				+ "','yyyy-mm-dd')");

		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		String responseStr = EntityUtils.toString(entity);
		response.close();

		// TODO Check if it's in correct page and if it has data

		Document doc = Jsoup.parse(responseStr);

		Element lastA = doc.select("a").last();
		// If there is only one page without pagination
		if (!lastA.hasAttr("onclick")) {
			receivingList.addAll(getReceivingFromPage(httpClient, user, doc));
		} else {

			// If there are multiple page without pagination
			String totalPageNumberStr = doc.select("a").last().attr("onclick");

			totalPageNumberStr = totalPageNumberStr.substring(totalPageNumberStr.indexOf("(") + 1,
					totalPageNumberStr.indexOf(")"));
			int totalPageNumber = Integer.valueOf(totalPageNumberStr);
			for (int i = 1; i <= totalPageNumber; i++) {
				Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));

				HttpGet summaryPageGet = new HttpGet("http://vd.rainbow.cn:8080/object/getAdvReportData.jsp"
						+ "?start=" + (i - 1) * 20 + "&limit=20" + "&rptId=3124" + "&showSelectionModel=false"
						+ "&html=Y" + "&sqlWhere=&where=%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3E=to_date('"
						+ receivingDateStr
						+ "','yyyy-mm-dd')%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3C=to_date('"
						+ receivingDateStr + "','yyyy-mm-dd')");

				CloseableHttpResponse summaryPageResponse = httpClient.execute(summaryPageGet);
				HttpEntity summaryPageEntity = summaryPageResponse.getEntity();
				String summaryPageResponseStr = EntityUtils.toString(summaryPageEntity);
				summaryPageResponse.close();
				Document summaryPageDoc = Jsoup.parse(summaryPageResponseStr);
				receivingList.addAll(getReceivingFromPage(httpClient, user, summaryPageDoc));

			}
		}

		log.info(user + "收货单下载结束.");
		log.info(user + "写入收货单文件.");
		Utils.exportReceivingInfoToTXTForRainbow(Constants.RETAILER_RAINBOW, user.getUserId(), receivingDate,
				receivingList);

		log.info(user + "写入收货单文件结束.");

		return Utils.getConlidatedOrderNoAmountByReceivingNoteTO(receivingList);

	}

	private List<ReceivingNoteTO> getReceivingFromPage(CloseableHttpClient httpClient, User user, Document pageDoc
			) throws InterruptedException, IOException, ClientProtocolException, BaseException {

		List<ReceivingNoteTO> receivingList = new ArrayList<ReceivingNoteTO>();
		// Receiving highlevel info
		Elements rowsElements = pageDoc.select("#dataTable").first().select("tr:gt(0)");
		rowsElements.remove(rowsElements.size() - 1);

		for (Element rowElement : rowsElements) {
			Elements tdElements = rowElement.select("td");
			String receivingNo = tdElements.get(0).text();
			log.info(user + "收货单编号：" + receivingNo);

			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
			HttpGet receivingDetailHTTPGet = new HttpGet("http://vd.rainbow.cn:8080//gysp/orderdetail.jsp?SHDH="
					+ receivingNo);
			CloseableHttpResponse receivingResponse = httpClient.execute(receivingDetailHTTPGet);
			HttpEntity receivingEntity = receivingResponse.getEntity();
			String receivingResponseStr = EntityUtils.toString(receivingEntity);
			// log.info(receivingResponseStr);
			receivingResponse.close();

			Document receivingDetailDoc = Jsoup.parse(receivingResponseStr);

			// Get receiving main info
			Elements receivingMainInfoElements = receivingDetailDoc.select("#table154").get(2).select("tr").get(1)
					.select("td");
			String storeName = receivingMainInfoElements.get(0).text();
			String receivingDate = receivingMainInfoElements.get(1).text();
			String orderNo = receivingMainInfoElements.get(2).text();

			storeName = storeName.substring(storeName.indexOf(":") + 1);
			receivingDate = receivingDate.substring(receivingDate.indexOf(":") + 1);
			orderNo = orderNo.substring(orderNo.indexOf(":") + 1);

			String storeID = Utils.getRainbowStoreIDByName(storeName);

			Elements receivingDetailTableElements = receivingDetailDoc.select("#table152").first().select("tr:gt(0)");
			receivingDetailTableElements.remove(receivingDetailTableElements.size() - 1);

			// Get receiving detail item
			for (Element receivingDetailRowElement : receivingDetailTableElements) {
				Elements receivingDetailTDElements = receivingDetailRowElement.select("td");
				String itemID = receivingDetailTDElements.get(0).text();
				String itemName = receivingDetailTDElements.get(2).text();
				String barcode = receivingDetailTDElements.get(1).text();
				String orderQuantity = receivingDetailTDElements.get(6).text();
				String orderTotalPrice = receivingDetailTDElements.get(8).text();
				String receivingQuantity = receivingDetailTDElements.get(9).text();
				String receivingTotalPrice = receivingDetailTDElements.get(12).text();
				String unitPrice = receivingDetailTDElements.get(7).text();
				RainbowReceivingTO rainbowReceivingTO = new RainbowReceivingTO(orderNo, storeID, storeName,
						receivingDate, itemID, barcode, itemName, orderQuantity, orderTotalPrice, receivingQuantity,
						receivingTotalPrice, unitPrice);
				receivingList.add(rainbowReceivingTO);
			}

//			countTO.increaseOne();

		}

		return receivingList;
	}

	protected int getSales(CloseableHttpClient httpClient, User user, Date salesDate, StringBuffer summaryBuffer)
			throws Exception {

		log.info(user + "开始下载销售单.");
		if (Utils.isSalesFileExist(user.getRetailer(), user.getUserId(), salesDate)) {
			log.info(user + "销售单已存在,不再下载");
			return 0;
		}
		String salesDateStr = DateUtil.toString(salesDate);
		log.info(user + "销售日期：" + salesDateStr);

		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));

		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("findModel", "0"));
		formParams.add(new BasicNameValuePair("command", "adv"));
		formParams.add(new BasicNameValuePair("rptId", "4762"));
		formParams.add(new BasicNameValuePair("optTJRQ", ">="));
		formParams.add(new BasicNameValuePair("TJRQ", salesDateStr));
		formParams.add(new BasicNameValuePair("optTJRQ_end", ">="));
		formParams.add(new BasicNameValuePair("TJRQ_end", salesDateStr));
		formParams.add(new BasicNameValuePair("sqlWhere", ""));
		formParams.add(new BasicNameValuePair("where", " and TJRQ>=to_date('" + salesDateStr
				+ "','yyyy-mm-dd') and TJRQ<=to_date('" + salesDateStr + "','yyyy-mm-dd')"));
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");

		HttpPost httppost = new HttpPost("http://vd.rainbow.cn:8080/object/ObjectReportExport.jsp");

		httppost.setEntity(loginEntity);

		CloseableHttpResponse response = httpClient.execute(httppost);
		HttpEntity entity = response.getEntity();
		// log.info( EntityUtils.toString(entity));

		// TODO Check if it's excel and if it has data in excel
		String salesFilePath = Utils.getProperty(user.getRetailer() + Constants.SALES_INBOUND_PATH);
		FileUtil.createFolder(salesFilePath);

		String salesFileNm = "Sales_" + user.getRetailer() + "_" + user.getUserId() + "_"
				+ DateUtil.toStringYYYYMMDD(salesDate) + ".xls";

		FileOutputStream receiveFos = new FileOutputStream(salesFilePath + salesFileNm);
		entity.writeTo(receiveFos);
		receiveFos.close();
		response.close();

		List<SalesTO> slaesList = Utils.getSalesTOListFromFileForRainbow(new File(salesFilePath + salesFileNm));
		
		
		log.info(user + "销售单文件名：" + salesFileNm);

		log.info(user + "下载销售单结束.");
		return slaesList.size();
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

		return Constants.RETAILER_RAINBOW;
	}

	@Override
	protected int getOrder(CloseableHttpClient httpClient, User user, Date processDate, StringBuffer summaryBuffer)
			throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}


}
