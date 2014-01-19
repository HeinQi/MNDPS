package com.rsi.mengniu.retailer.yonghui.service;

import java.io.FileOutputStream;
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
import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.OCR;
import com.rsi.mengniu.util.Utils;

//http://vss.yonghui.cn:9999/vss/logon/logon.jsp
public class YonghuiDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(YonghuiDataPullService.class);
	private OCR ocr;

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();

		String loginResult = null;
		int loginCount = 0; // 如果验证码出错重新login,最多20次
		try {
			do {
				loginResult = login(httpClient, user);
				loginCount++;
			} while ("InvalidCode".equals(loginResult) && loginCount < 20);
			// Invalid Password and others
			if (!"Success".equals(loginResult)) {
				return;
			}
			// receive
			getReceive(httpClient, user);
			// order
			getOrder(httpClient, user);

			httpClient.close();
		} catch (Exception e) {
			log.error(user+Utils.getTrace(e));
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
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
		String validateImgPath = Utils.getProperty("validate.image.path");
		FileUtil.createFolder(validateImgPath);
		HttpGet httpCheckcodeGet = new HttpGet("http://vss.yonghui.cn:9999/vss/" + checkcodeUrl.substring(checkcodeUrl.indexOf("DaemonLogonVender")));
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
			log.info(user+"校验码无效,Relogin...");
			return "InvalidCode";
		} else if (responseStr.contains("登录失败,请检查登录名和密码")) {
			log.info(user + "登录失败,请检查登录名和密码!");
			return "InvalidPassword";
		} else if (!responseStr.contains("订退货管理")) {
			log.info(user + "系统出错,退出!");
		}
		loginResponse.close();
		log.info(user + "登录成功!");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		return "Success";
	}

	public void getReceive(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始下载收货单...");
		// /vss/DaemonSearchSheet?docdate_min=2014-01-01&docdate_max=2014-10-05&sheetname=receipt
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("docdate_min", DateUtil.toString(Utils.getStartDate(Constants.RETAILER_YONGHUI), "yyyy-MM-dd")));
		receiveformParams.add(new BasicNameValuePair("docdate_max", DateUtil.toString(Utils.getEndDate(Constants.RETAILER_YONGHUI), "yyyy-MM-dd")));
		receiveformParams.add(new BasicNameValuePair("sheetname", "receipt"));
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		HttpPost receivePost = new HttpPost("http://vss.yonghui.cn:9999/vss/DaemonSearchSheet");
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr = EntityUtils.toString(receiveRes.getEntity());
		receiveRes.close();
		Document xmlDoc = Jsoup.parse(responseStr, "", Parser.xmlParser());
		Elements sheetIdElements = xmlDoc.select("sheetid");
		log.info(user + "查询到从" + DateUtil.toString(Utils.getStartDate(Constants.RETAILER_YONGHUI), "yyyy-MM-dd") + "到" + DateUtil.toString(Utils.getEndDate(Constants.RETAILER_YONGHUI), "yyyy-MM-dd")
				+ ",共有" + sheetIdElements.size() + "条收货单");
		int count = 0;
		List<ReceivingNoteTO> receiveList = new ArrayList<ReceivingNoteTO>();
		for (Element eSheetId : sheetIdElements) {
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
			String sheetId = eSheetId.text();
			HttpGet httpGet = new HttpGet("http://vss.yonghui.cn:9999/vss/DaemonViewSheet?sheet=receipt&sheetid=" + sheetId);
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
		FileUtil.exportReceivingInfoToTXT("yonghui", user.getUserId(), receiveList);
		log.info(user + "收货单数据下载成功!");
	}

	public void getOrder(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "订单数据下载...");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_YONGHUI));
		// http://vss.yonghui.cn:9999/vss/DownloadSheet?orderdate_min=2014-01-01&orderdate_max=2014-01-05&operation=eptOrderSheet
		String startDate = DateUtil.toString(Utils.getStartDate(Constants.RETAILER_YONGHUI), "yyyy-MM-dd");
		String endDate = DateUtil.toString(Utils.getEndDate(Constants.RETAILER_YONGHUI), "yyyy-MM-dd");
		String orderPath = Utils.getProperty(user.getRetailer()+Constants.ORDER_PATH);
		FileUtil.createFolder(orderPath);
		String fileNm = "Order_"+user.getRetailer()+"_"+user.getUserId()+DateUtil.toStringYYYYMMDD(new Date())+".xls";
		FileOutputStream orderFos = new FileOutputStream(orderPath+fileNm);
		String url = "http://vss.yonghui.cn:9999/vss/DownloadSheet?orderdate_min=" + startDate + "&orderdate_max=" + endDate
				+ "&operation=eptOrderSheet";
		HttpGet httpGet = new HttpGet(url);
		CloseableHttpResponse response = httpClient.execute(httpGet);
		response.getEntity().writeTo(orderFos);
		response.close();
		log.info(user + "订单数据下载成功");
	}

	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}

}
