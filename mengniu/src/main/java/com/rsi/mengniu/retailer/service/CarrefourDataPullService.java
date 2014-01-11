package com.rsi.mengniu.retailer.service;

import java.io.FileOutputStream;
import java.util.ArrayList;
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

import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.OCR;
import com.rsi.mengniu.util.Utils;

//http://supplierweb.carrefour.com.cn/
public class CarrefourDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(CarrefourDataPullService.class);
	private OCR ocr;
	
	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String loginResult = null;
		int loginCount = 0; // 如果验证码出错重新login,最多15次
		try {
			do {
				loginResult = login(httpClient, user);
				loginCount++;
			} while ("InvalidCode".equals(loginResult) && loginCount < 15);
			// Invalid Password and others
			if (!"Success".equals(loginResult)) {
				return;
			}
			// receive
			 getReceiveExcel(httpClient,user);
			 // order
			getOrder(httpClient);
			httpClient.close();
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user+"开始登录...");
		HttpGet httpGet = new HttpGet("http://supplierweb.carrefour.com.cn/includes/image.jsp");
		String imgName = String.valueOf(java.lang.System.currentTimeMillis());

		FileOutputStream fos = new FileOutputStream(validateImgPath+"/" + imgName + ".jpg");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		entity.writeTo(fos);
		response.close();
		fos.close();
		String recognizeStr = ocr.recognizeText(validateImgPath+"/" + imgName + ".jpg",validateImgPath+"/"+ imgName,true);
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
		if (responseStr.contains("验证码失效")) {
			log.info("验证码失效,Relogin...");
			return "InvalidCode";
		} else if (responseStr.contains("错误的密码")) {
			log.info(user+"错误的密码,退出!");
			return "InvalidPassword";
		} else if (responseStr.contains("系统出错")) {
			log.info("系统出错,退出!");
			return "SystemError";
		}
		loginResponse.close();
		log.info(user+"登录成功!");
		return "Success";
	}

	public void getReceiveExcel(CloseableHttpClient httpClient,User user) throws Exception {
		// goMenu('inyr.do?action=query','14','预估进退查询')

		// $('inyrForm').action="inyr.do?action=export";
		// 供应商预估进退查询/Supplier Inyr Inquiry
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("unitid", "ALL"));
		receiveformParams.add(new BasicNameValuePair("butype", "byjv"));
		receiveformParams.add(new BasicNameValuePair("systemdate", "2013/12/22")); // yyyy/mm/dd
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		HttpPost receivePost = new HttpPost("http://supplierweb.carrefour.com.cn/inyr.do?action=export");
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr = EntityUtils.toString(receiveRes.getEntity());
		receiveRes.close();

		if (responseStr.contains("Excel文档生成成功")) {
			// Download Excel file
			responseStr = responseStr.substring(responseStr.indexOf("javascript:downloads('") + 22);
			String inyrFileName = responseStr.substring(0, responseStr.indexOf("'"));
			FileOutputStream receiveFos = new FileOutputStream("/Users/haibin/Documents/temp/" + inyrFileName);

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

		} else {
			log.info("Carrefour export Excel Error!");
		}

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

		// https://platform.powere2e.com/platform/mailbox/openInbox.htm?
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
	private String validateImgPath;
	
	public String getValidateImgPath() {
		return validateImgPath;
	}

	public void setValidateImgPath(String validateImgPath) {
		this.validateImgPath = validateImgPath;
	}
	public OCR getOcr() {
		return ocr;
	}

	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}
}