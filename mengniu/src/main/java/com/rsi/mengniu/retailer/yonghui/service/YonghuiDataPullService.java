package com.rsi.mengniu.retailer.yonghui.service;

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
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.DataPullTaskPool;
import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.OCR;
import com.rsi.mengniu.util.Utils;

//http://vss.yonghui.cn:9999/vss/logon/logon.jsp
public class YonghuiDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(YonghuiDataPullService.class);
	private static Log summaryLog = LogFactory.getLog(Constants.SUMMARY_YONGHUI);
	private OCR ocr;

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		List<Date> dates = DateUtil.getDateArrayByRange(Utils.getStartDate(Constants.RETAILER_YONGHUI),
				Utils.getEndDate(Constants.RETAILER_YONGHUI));
		StringBuffer summaryBuffer = new StringBuffer();
		summaryBuffer.append("运行时间: "+new Date()+"\r\n");
		summaryBuffer.append("零售商: "+user.getRetailer()+"\r\n");
		summaryBuffer.append("用户: "+user.getUserId()+"\r\n");
		String loginResult = null;
		int loginCount = 0; // 如果验证码出错重新login,最多20次
		try {
			do {
				loginResult = login(httpClient, user);
				loginCount++;
			} while ("InvalidCode".equals(loginResult) && loginCount < 20);
			// Invalid Password and others
			if (!"Success".equals(loginResult)) {
				summaryBuffer.append("登录失败!\r\n");
				summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE+"\r\n");
				summaryLog.info(summaryBuffer);
				return;
			}
		} catch (Exception e) {
			log.error(user + "网站登录出错,请检查!");
			errorLog.error(user, e);
			summaryBuffer.append("登录失败!\r\n");
			summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE+"\r\n");
			summaryLog.info(summaryBuffer);
			DataPullTaskPool.addFailedUser(user);
			return;
		}
		
		String venderIds ="";
		try {
			venderIds = getVenders(httpClient);
		} catch (Exception e) {
			errorLog.error(user, e);
		}
		//System.out.println(venderIds);
		summaryBuffer.append(Constants.SUMMARY_TITLE_RECEIVING+"\r\n");
		
		for (Date searchDate : dates) {
			try {
				// receive
				summaryBuffer.append("收货单日期: " + DateUtil.toString(searchDate, "yyyy-MM-dd") + "\r\n");
				getReceive(httpClient, user, searchDate);
			} catch (Exception e) {
				log.error(user + "页面加载失败，请登录网站检查收货单查询功能是否正常!");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
				summaryBuffer.append("收货单下载失败"+"\r\n");
			}
		}
		summaryBuffer.append(Constants.SUMMARY_TITLE_ORDER+"\r\n");
		
		for (Date searchDate : dates) {
			try {
				// order
				summaryBuffer.append("订单日期: " + DateUtil.toString(searchDate, "yyyy-MM-dd") + "\r\n");
				getOrder(httpClient, user, searchDate);
				summaryBuffer.append("订单下载成功" + "\r\n");
			} catch (Exception e) {
				summaryBuffer.append("订单下载失败" + "\r\n");
				log.error(user + "页面加载失败，请登录网站检查订单查询功能是否正常!");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
			}
		}
		summaryBuffer.append(Constants.SUMMARY_TITLE_SALES + "\r\n");
		for (Date searchDate : dates) {
			try {
				summaryBuffer.append("销售日期: " + DateUtil.toString(searchDate, "yyyy-MM-dd") + "\r\n");
				getSales(httpClient, user, DateUtil.toString(searchDate, "yyyy-MM-dd"),venderIds);
				summaryBuffer.append("销售数据下载成功\r\n");
			} catch (Exception e) {
				summaryBuffer.append("销售数据下载失败" + "\r\n");
				log.error(user + "页面加载失败，请登录网站检查销售数据查询功能是否正常!");
				errorLog.error(user, e);
				DataPullTaskPool.addFailedUser(user);
			}
		}

		try {
			httpClient.close();
		} catch (IOException e) {
			errorLog.error(user, e);
		}
		
		summaryBuffer.append(Constants.SUMMARY_SEPERATOR_LINE+"\r\n");
		summaryLog.info(summaryBuffer);
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		HttpGet httpGet = new HttpGet("http://vss.yonghui.cn:9999/vss/logon/logon.jsp");
		CloseableHttpResponse loginPageResponse = httpClient.execute(httpGet);
		String loginPageStr = EntityUtils.toString(loginPageResponse.getEntity());
		Document loginPage = Jsoup.parse(loginPageStr);
		loginPageResponse.close();
		Element imageElement = loginPage.select("#img_checkcode").first();
		String checkcodeUrl = imageElement.attr("src");
		// checkcode1="+ 57646
		String checkcode1 = loginPageStr.substring(loginPageStr.indexOf("checkcode1=\"+") + 13);
		checkcode1 = checkcode1.substring(0, checkcode1.indexOf("+")).trim();
		String validateImgPath = Utils.getProperty(Constants.TEMP_PATH);
		FileUtil.createFolder(validateImgPath);
		HttpGet httpCheckcodeGet = new HttpGet("http://vss.yonghui.cn:9999/vss/"
				+ checkcodeUrl.substring(checkcodeUrl.indexOf("DaemonLogonVender")));
		String imgName = String.valueOf(java.lang.System.currentTimeMillis());
		FileOutputStream fos = new FileOutputStream(validateImgPath + imgName + ".jpg");
		CloseableHttpResponse checkcodeResponse = httpClient.execute(httpCheckcodeGet);
		HttpEntity entity = checkcodeResponse.getEntity();
		entity.writeTo(fos);
		checkcodeResponse.close();
		fos.close();
		Utils.binaryImage(validateImgPath + imgName);
		String recognizeStr = ocr.recognizeText(validateImgPath + imgName + ".png", validateImgPath + imgName, false);
		recognizeStr.replaceAll("'", "").replaceAll(",", "").replaceAll(".", "");
		// http://vss.yonghui.cn:9999/vss/DaemonLogonVender?action=logon&logonid=124746BJ&password=852963&checkcode1=57552&checkcode2=DEPM
		// action:logon
		// logonid:124746BJ
		// password:852963
		// checkcode1:57483
		// checkcode2:YLRD
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("action", "logon"));
		formParams.add(new BasicNameValuePair("logonid", user.getUserId()));
		formParams.add(new BasicNameValuePair("password", user.getPassword()));
		formParams.add(new BasicNameValuePair("checkcode1", checkcode1));
		formParams.add(new BasicNameValuePair("checkcode2", recognizeStr.toUpperCase())); // 校验码无效
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("http://vss.yonghui.cn:9999/vss/DaemonLogonVender");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		String responseStr = EntityUtils.toString(loginResponse.getEntity());
		if (responseStr.contains("校验码无效") || responseStr.contains("checkcode2 not set")) {
			log.info(user + "校验码无效,Relogin...");
			return "InvalidCode";
		} else if (responseStr.contains("登录失败,请检查登录名和密码")) {
			log.info(user + "登录失败,请检查登录名和密码!");
			Utils.recordIncorrectUser(user);
			return "InvalidPassword";
		} else if (!responseStr.contains("OK")) {
			log.info(user + "系统出错,退出!");
			return "Error";
		}
		loginResponse.close();
		log.info(user + "登录成功!");
		return "Success";
	}

	public void getReceive(CloseableHttpClient httpClient, User user, Date searchDate) throws Exception {
		if (Utils.isReceivingFileExist(user.getRetailer(),user.getUserId(),searchDate)) {
			log.info(user+"收货单已存在,不再下载");
			return;
		}
		log.info(user + "开始下载收货单...");
		String searchDateStr = DateUtil.toString(searchDate, "yyyy-MM-dd");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		// /vss/DaemonSearchSheet?docdate_min=2014-01-01&docdate_max=2014-10-05&sheetname=receipt
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("docdate_min", searchDateStr));
		receiveformParams.add(new BasicNameValuePair("docdate_max", searchDateStr));
		receiveformParams.add(new BasicNameValuePair("sheetname", "receipt"));
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		HttpPost receivePost = new HttpPost("http://vss.yonghui.cn:9999/vss/DaemonSearchSheet");
		receivePost.setConfig(Utils.getTimeoutConfig());
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr = EntityUtils.toString(receiveRes.getEntity());
		receiveRes.close();
		Document xmlDoc = Jsoup.parse(responseStr, "", Parser.xmlParser());
		Elements sheetIdElements = xmlDoc.select("sheetid");
		log.info(user + "查询到从" + searchDateStr + "到"
				+ searchDateStr + ",共有"
				+ sheetIdElements.size() + "条收货单");
		int count = 0;
		List<ReceivingNoteTO> receiveList = new ArrayList<ReceivingNoteTO>();
		for (Element eSheetId : sheetIdElements) {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
			String sheetId = eSheetId.text();
			HttpGet httpGet = new HttpGet("http://vss.yonghui.cn:9999/vss/DaemonViewSheet?sheet=receipt&sheetid="
					+ sheetId);
			httpGet.setConfig(Utils.getTimeoutConfig());
			CloseableHttpResponse detailResponse = httpClient.execute(httpGet);
			String detailStr = EntityUtils.toString(detailResponse.getEntity());
			Document xmlDetailDoc = Jsoup.parse(detailStr, "", Parser.xmlParser());
			String storeId = xmlDetailDoc.select("shopid").first().text();
			String storeNm = xmlDetailDoc.select("shopname").first().text();
			String orderNo = xmlDetailDoc.select("refsheetid").first().text();
			String receiveDate = xmlDetailDoc.select("editdate").first().text();
			Element bodyElement = xmlDetailDoc.select("body").first();
			Elements rowElements = bodyElement.select("row");
			for (Element row : rowElements) {
				ReceivingNoteTO receiveTo = new ReceivingNoteTO();
				receiveTo.setStoreID(storeId);
				receiveTo.setStoreName(storeNm);
				receiveTo.setOrderNo(orderNo);
				receiveTo.setReceivingDate(receiveDate);
				receiveTo.setItemID(row.select("goodsid").text());
				receiveTo.setItemName(row.select("goodsname").text());
				receiveTo.setBarcode(row.select("barcode").text());
				receiveTo.setQuantity(row.select("rcvqty").text());
				receiveTo.setUnitPrice(row.select("cost").text());
				receiveTo.setTotalPrice(row.select("totalcost").text());
				receiveList.add(receiveTo);
			}
			count++;
			log.info(user + "成功下载收货单[" + count + "],验收单号:" + sheetId);
		}
		Utils.exportReceivingInfoToTXT(Constants.RETAILER_YONGHUI,user.getUserId(),searchDate,receiveList);
		log.info(user + "收货单数据下载成功!");
	}

	public void getOrder(CloseableHttpClient httpClient, User user, Date searchDate) throws Exception {
		String fileNm = "Order_" + user.getRetailer() + "_" + user.getUserId() + "_"
				+ DateUtil.toStringYYYYMMDD(searchDate) + ".xls";
		String searchDateStr = DateUtil.toString(searchDate, "yyyy-MM-dd");
		if (Utils.isOrderFileExist(user.getRetailer(),fileNm)) {
			log.info(user+"订单日期: "+searchDateStr+"的订单已存在,不再下载");
			return;
		}
		log.info(user + "订单数据下载...");
		
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		// http://vss.yonghui.cn:9999/vss/DownloadSheet?orderdate_min=2014-01-01&orderdate_max=2014-01-05&operation=eptOrderSheet
		String orderPath = Utils.getProperty(user.getRetailer() + Constants.ORDER_INBOUND_PATH);
		FileUtil.createFolder(orderPath);

		FileOutputStream orderFos = new FileOutputStream(orderPath + fileNm);
		String url = "http://vss.yonghui.cn:9999/vss/DownloadSheet?orderdate_min=" + searchDateStr + "&orderdate_max="
				+ searchDateStr + "&operation=eptOrderSheet";
		HttpGet httpGet = new HttpGet(url);
		httpGet.setConfig(Utils.getTimeoutConfig());
		CloseableHttpResponse response = httpClient.execute(httpGet);
		response.getEntity().writeTo(orderFos);
		response.close();
		log.info(user + "订单数据下载成功");
	}

	public void getSales(CloseableHttpClient httpClient, User user, String searchDate,String venderIds) throws Exception {
		if (Utils.isSalesFileExist(user.getRetailer(), user.getUserId(), DateUtil.toDate(searchDate,"yyyy-MM-dd"))) {
			log.info(user+"销售日期: "+searchDate+"的销售数据已存在,不再下载");
			return;			
		}		
		log.info(user + "下载日期为" + searchDate + "的销售数据...");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
		searchformParams.add(new BasicNameValuePair("reportname", "salecost"));
		searchformParams.add(new BasicNameValuePair("sdate_min", searchDate));
		searchformParams.add(new BasicNameValuePair("sdate_max", searchDate));
		searchformParams.add(new BasicNameValuePair("nosale", "false"));
		searchformParams.add(new BasicNameValuePair("action", "getwithoutshop"));
		searchformParams.add(new BasicNameValuePair("venderidarray", venderIds));
		HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
		HttpPost searchPost = new HttpPost("http://vss.yonghui.cn:9999/vss/DaemonReport");
		searchPost.setConfig(Utils.getTimeoutConfig());
		searchPost.setEntity(searchFormEntity);
		String responseStr = Utils.HttpExecute(httpClient, searchPost, "sale_cost");
		Document xmlDoc = Jsoup.parse(responseStr, "", Parser.xmlParser());
		Elements goodsIds = xmlDoc.select("goodsid");

		if (goodsIds != null) {
			log.info(user + "查询到日期为" + searchDate + "销售的货物" + goodsIds.size() + "条");
			List<SalesTO> salesList = new ArrayList<SalesTO>();
			for (Element eGoodsId : goodsIds) {
				Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
				String goodsId = eGoodsId.text();
				List<NameValuePair> withShopformParams = new ArrayList<NameValuePair>();
				withShopformParams.add(new BasicNameValuePair("reportname", "salecost"));
				withShopformParams.add(new BasicNameValuePair("sdate_min", searchDate));
				withShopformParams.add(new BasicNameValuePair("sdate_max", searchDate));
				withShopformParams.add(new BasicNameValuePair("goodsid", goodsId));
				withShopformParams.add(new BasicNameValuePair("action", "getwithshop"));
				withShopformParams.add(new BasicNameValuePair("venderidarray", venderIds));
				HttpEntity withShopFormEntity = new UrlEncodedFormEntity(withShopformParams, "UTF-8");
				HttpPost withShopPost = new HttpPost("http://vss.yonghui.cn:9999/vss/DaemonReport");
				withShopPost.setConfig(Utils.getTimeoutConfig());
				withShopPost.setEntity(withShopFormEntity);
				String withShopStr = Utils.HttpExecute(httpClient, withShopPost, "sale_cost");
				Document xmlDetailDoc = Jsoup.parse(withShopStr, "", Parser.xmlParser());
				Elements rows = xmlDetailDoc.select("row");
				if (rows != null) {
					log.info(user + "下载货物ID为" + goodsId + "的门店销售数据");
					for (Element row : rows) {
						SalesTO salesTo = new SalesTO();
						salesTo.setStoreID(row.select("shopid").text());
						salesTo.setItemID(goodsId);
						salesTo.setSalesDate(searchDate);
						salesTo.setItemName(row.select("goodsname").text());
						salesTo.setSalesQuantity(row.select("qty").text());
						salesTo.setSalesAmount(row.select("truevalue").text());
						salesList.add(salesTo);
					}
				}
			}
			Utils.exportSalesInfoToTXT(Constants.RETAILER_YONGHUI, user.getUserId(),
					DateUtil.toDate(searchDate, "yyyy-MM-dd"), salesList);
			log.info(user + "成功下载日期为" + searchDate + "的销售数据");
		} else {
			log.info(user + "没有查询到销售数据或网站出错");
		}
	}

	
	private String getVenders(CloseableHttpClient httpClient) throws Exception {
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		HttpGet httpGet = new HttpGet("http://vss.yonghui.cn:9999/vss/DaemonShopList?sheetname=vender");
		CloseableHttpResponse venderPageResponse = httpClient.execute(httpGet);
		String venderPageStr = EntityUtils.toString(venderPageResponse.getEntity());
		Document venderPage = Jsoup.parse(venderPageStr);
		venderPageResponse.close();
		String venderIds = "";
		if (venderPageStr.contains("ddlSubVender") && venderPageStr != null) {
			Elements venders = venderPage.select("option");
			for (Element vender: venders) {
				venderIds += vender.attr("value")+",";
			}
			venderIds = venderIds.substring(0, venderIds.length()-1);
		}
		return venderIds;
		
	}
	
	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}

}
