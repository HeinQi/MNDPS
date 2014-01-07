package com.rsi.mengniu.retailer.service;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.OCR;
import com.rsi.mengniu.util.Utils;



//http://vss.yonghui.cn:9999/vss/logon/logon.jsp
public class YonghuiDataPull implements RetailerDataPull {
	private static Log log = LogFactory.getLog(YonghuiDataPull.class);
	private OCR ocr;
	private String validateImgPath;
	private Properties configs; 

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
			getReceive(httpClient,user);
			 // order
			//getOrder(httpClient);
			httpClient.close();
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		HttpGet httpGet = new HttpGet("http://vss.yonghui.cn:9999/vss/logon/logon.jsp");
		CloseableHttpResponse loginPageResponse = httpClient.execute(httpGet);
		String loginPageStr = EntityUtils.toString(loginPageResponse.getEntity());
		Document loginPage = Jsoup.parse(loginPageStr);
		loginPageResponse.close();
		Element imageElement = loginPage.select("#img_checkcode").first();
		String checkcodeUrl = imageElement.attr("src");
		//checkcode1="+ 57646 
		String checkcode1 = loginPageStr.substring(loginPageStr.indexOf("checkcode1=\"+")+13);
		checkcode1 = checkcode1.substring(0,checkcode1.indexOf("+")).trim();
		
		HttpGet httpCheckcodeGet = new HttpGet("http://vss.yonghui.cn:9999/vss/"+checkcodeUrl.substring(checkcodeUrl.indexOf("DaemonLogonVender")));
		String imgName = String.valueOf(java.lang.System.currentTimeMillis());
		FileOutputStream fos = new FileOutputStream(validateImgPath+"/" + imgName + ".jpg");
		CloseableHttpResponse checkcodeResponse = httpClient.execute(httpCheckcodeGet);
		HttpEntity entity = checkcodeResponse.getEntity();
		entity.writeTo(fos);
		checkcodeResponse.close();
		fos.close();
		Utils.binaryImage(validateImgPath+"/" + imgName );
		String recognizeStr = ocr.recognizeText(validateImgPath+"/" + imgName + ".png",
				"/Users/haibin/Documents/temp/" + imgName,false);
		recognizeStr.replaceAll("'","").replaceAll(",", "").replaceAll(".", "");
		//http://vss.yonghui.cn:9999/vss/DaemonLogonVender?action=logon&logonid=124746BJ&password=852963&checkcode1=57552&checkcode2=DEPM
		//action:logon
		//logonid:124746BJ
		//password:852963
		//checkcode1:57483
		//checkcode2:YLRD
		
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("action", "logon"));
		formParams.add(new BasicNameValuePair("logonid", user.getUserId()));
		formParams.add(new BasicNameValuePair("password", user.getPassword()));
		formParams.add(new BasicNameValuePair("checkcode1", checkcode1));
		formParams.add(new BasicNameValuePair("checkcode2", recognizeStr.toUpperCase())); //校验码无效
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("http://vss.yonghui.cn:9999/vss/DaemonLogonVender");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		String responseStr = EntityUtils.toString(loginResponse.getEntity());
		if (responseStr.contains("校验码无效") || responseStr.contains("checkcode2 not set")) {
			log.error("校验码无效,Relogin...");
			return "InvalidCode";
		} else if (responseStr.contains("登录失败,请检查登录名和密码")) {
			log.error("登录失败,请检查登录名和密码!" + user);
			return "InvalidPassword";
		}
		loginResponse.close();

		return "Success";
	}

	public void getReceive(CloseableHttpClient httpClient,User user) throws Exception {
		// /vss/DaemonSearchSheet?docdate_min=2014-01-01&docdate_max=2014-10-05&sheetname=receipt
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("docdate_min", "2014-01-01"));
		receiveformParams.add(new BasicNameValuePair("docdate_max", "2014-01-05"));
		receiveformParams.add(new BasicNameValuePair("sheetname", "receipt"));
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		HttpPost receivePost = new HttpPost("http://vss.yonghui.cn:9999/vss/DaemonSearchSheet");
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr = EntityUtils.toString(receiveRes.getEntity());
		receiveRes.close();
		Document xmlDoc = Jsoup.parse(responseStr, "", Parser.xmlParser());
		Elements sheetIdElements = xmlDoc.select("sheetid");
		List<ReceivingNoteTO> receiveList = new ArrayList<ReceivingNoteTO>();
		for (Element eSheetId: sheetIdElements) {
			String sheetId = eSheetId.text();
			HttpGet httpGet = new HttpGet("http://vss.yonghui.cn:9999/vss/DaemonViewSheet?sheet=receipt&sheetid="+sheetId);
			CloseableHttpResponse detailResponse = httpClient.execute(httpGet);
			String detailStr = EntityUtils.toString(detailResponse.getEntity());
			Document xmlDetailDoc = Jsoup.parse(detailStr, "", Parser.xmlParser());
			String storeId = xmlDetailDoc.select("shopid").first().text();
			String storeNm = xmlDetailDoc.select("shopname").first().text();
			String receiveDate = xmlDetailDoc.select("editdate").first().text();
			Element bodyElement = xmlDetailDoc.select("body").first();
			Elements rowElements= bodyElement.select("row");
			for (Element row:rowElements) {
				ReceivingNoteTO receiveTo = new ReceivingNoteTO();
				receiveTo.setStoreNo(storeId);
				receiveTo.setStoreName(storeNm);
				receiveTo.setReceivingDate(receiveDate);
				receiveTo.setItemCode(row.select("goodsid").text());
				receiveTo.setItemName(row.select("goodsname").text());
				receiveTo.setBarcode(row.select("barcode").text());
				receiveTo.setQuantity(row.select("rcvqty").text());
				receiveTo.setUnitPrice(row.select("cost").text());
				receiveTo.setTotalPrice(row.select("totalcost").text());
				receiveList.add(receiveTo);
			}
		}
		FileUtil.exportReceivingInfoToTXT("yonghui", userID, receiveList);
		
		//http://vss.yonghui.cn:9999/vss/DaemonViewSheet?sheet=receipt&sheetid=50000001092014MB
		
		
		
	}
		


	public void getOrder(CloseableHttpClient httpClient) throws Exception {
		// forward to PowerE2E Platform
		HttpGet httpGet = new HttpGet("http://supplierweb.carrefour.com.cn/callSSO.jsp");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		if (!EntityUtils.toString(entity).contains("PowerE2E Platform")) {
			log.error("Carrefour get order error,cannot forward to PowerE2E Platform");
			return;
		}
		response.close();

		// /vss/DaemonSearchSheet?docdate_min=2014-01-01&docdate_max=2014-10-05&sheetname=receipt
		List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
		searchformParams.add(new BasicNameValuePair("receivedDateFrom", "01-12-2013"));
		searchformParams.add(new BasicNameValuePair("receivedDateTo", "03-01-2014"));
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
		recordStr = recordStr.substring(recordStr.indexOf("共")+1,recordStr.indexOf("记"));
		recordStr = recordStr.replaceAll(",", "");
		int record = Integer.parseInt(recordStr);
		int page = record % 10 > 0 ? record/10+1 : record/10;
		System.out.println(page);
				
		Elements msgIds = doc.select("input[name=msgId]");
		List<String> msgIdList = new ArrayList<String>();
		for (Element msgId: msgIds) {
			msgIdList.add(msgId.attr("value"));
		}
		//取得每页的MsgId
		while (page>1) {
			getMsgIdByPage(page,msgIdList,httpClient);
			page--;
		}
		for (String msgId: msgIdList) {
			HttpGet httpOrderGet = new HttpGet("https://platform.powere2e.com/platform/mailbox/performDocAction.htm?actionId=1&guid="+msgId);
			CloseableHttpResponse orderRes = httpClient.execute(httpOrderGet);
			String orderDetail = EntityUtils.toString(orderRes.getEntity());
			orderRes.close();
			if (!orderDetail.contains("Carrefour Purchase Order")) {
				continue;
			}
			List<OrderTO> orderItems = new ArrayList<OrderTO>();
			Document orderDoc = Jsoup.parse(orderDetail);
			Element orderTable = orderDoc.select("table.tab2").first();
			String storeName = orderTable.select("tr:eq(1)").select("td").get(0).text();//store
			String orderNo = orderTable.select("tr:eq(2)").select("td").get(1).text();//订单号码
			String orderDate = orderTable.select("tr:eq(3)").select("td").get(1).text();//订单日期时间
			Element orderItemTable = orderDoc.select("table.tab2").last();
			
			for (Element row:orderItemTable.select("tr:gt(2)")) {
				Elements tds = row.select("td");
				OrderTO orderTo = new OrderTO();
				orderTo.setStoreName(storeName);
				orderTo.setOrderNo(orderNo);
				orderTo.setOrderDate(orderDate);
				orderTo.setItemCode(tds.get(0).select("span").first().text());//单品号
				orderTo.setBarcode(tds.get(1).select("span").first().text()); //条形码
				orderTo.setItemName(tds.get(2).text());//单品名称
				orderTo.setQuantity(tds.get(6).text());//总计数量
				orderTo.setUnitPrice(tds.get(7).text()); //单价
				orderTo.setTotalPrice(tds.get(8).text());//总金额
				orderItems.add(orderTo);
			}
			FileUtil.exportOrderInfoToTXT("carrefour", orderNo, orderItems);
		}

//		Element dataTable = doc.select("table.tbllist").first();
//		for (Element row : dataTable.select("tr:gt(0)")) {
//			Elements tds = row.select("td:not([rowspan])");
//			System.out.println(tds.get(0).text() + "->" + tds.get(1).text());
//		}
		// viewMessage('/platform', 'C4net2--56f631-1435654b8f5-f528764d624db129b32c21fbca0cb8d6');
		// location.href = (applicationContext + "/mailbox/performDocAction.htm?guid=" + guid + "&actionId=" + 1);

	}

//function gotoPage(page) {
//  document.forms[0].action = "/platform/mailbox/navigateInbox.htm?gotoPage="+ page;
//  document.forms[0].submit();
//}
	private void getMsgIdByPage(int page,List<String> msgIdList,CloseableHttpClient httpClient) throws Exception {
		List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
		searchformParams.add(new BasicNameValuePair("receivedDateFrom", "03-01-2014"));
		searchformParams.add(new BasicNameValuePair("receivedDateTo", "03-01-2014"));
		HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
		HttpPost searchPost = new HttpPost("https://platform.powere2e.com/platform/mailbox/navigateInbox.htm?gotoPage="+page);
		searchPost.setEntity(searchFormEntity);
		CloseableHttpResponse searchRes = httpClient.execute(searchPost);
		String responseStr = EntityUtils.toString(searchRes.getEntity());
		searchRes.close();	
		Document doc = Jsoup.parse(responseStr);
		Elements msgIds = doc.select("input[name=msgId]");
		for (Element msgId: msgIds) {
			msgIdList.add(msgId.attr("value"));
		}
	}


	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}

	public void setConfigs(Properties configs) {
		this.configs = configs;
	}
	
	public void setValidateImgPath(String validateImgPath) {
		this.validateImgPath = validateImgPath;
	}
}
