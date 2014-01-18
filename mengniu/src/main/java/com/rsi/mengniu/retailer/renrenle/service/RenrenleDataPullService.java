package com.rsi.mengniu.retailer.renrenle.service;

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
import org.jsoup.select.Elements;

import com.rsi.mengniu.Constants;
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
			//getReceiveExcel(httpClient, user);
			// order
			//getOrder(httpClient, user);
			httpClient.close();
		} catch (Exception e) {
			log.error(user + Utils.getTrace(e));
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));
		HttpGet httpGet = new HttpGet("http://www.renrenle.cn/scm/verifyCode.jsp");
		String imgName = String.valueOf(java.lang.System.currentTimeMillis());
		String validateImgPath = Utils.getProperty("validate.image.path");
		FileUtil.createFolder(validateImgPath);
		FileOutputStream fos = new FileOutputStream(validateImgPath + imgName + ".jpg");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		entity.writeTo(fos);
		response.close();
		fos.close();
		String recognizeStr = ocr.recognizeText(validateImgPath + imgName + ".jpg", validateImgPath + imgName, false);
		// login /login.do?action=doLogin
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("name", user.getUserId()));
		formParams.add(new BasicNameValuePair("password",user.getPassword()));
		formParams.add(new BasicNameValuePair("shopID", "RRL001")); //必须选人人乐集团
		formParams.add(new BasicNameValuePair("verifyCode", recognizeStr)); 
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("http://www.renrenle.cn/scm/login.do?method=login");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		String responseStr = new String (EntityUtils.toString(loginResponse.getEntity()).getBytes("ISO_8859_1"),"GBK");
		
		if (responseStr.contains("验证码输入不正确")) {
			log.info(user + "验证码输入不正确,Relogin...");
			return "InvalidCode";
		} else if (responseStr.contains("您的用户名或密码输入有误")) {
			log.info(user + "您的用户名或密码输入有误,退出!");
			return "InvalidPassword"; 
		} else if (!responseStr.contains("mainAction")) {
			log.info(user + "系统出错,退出!");
			return "SystemError";
		}
		loginResponse.close();
		log.info(user + "登录成功!");
		
		return "Success";
	}

	public void getReceiveExcel(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始下载收货单...");
		// goMenu('inyr.do?action=query','14','预估进退查询')

		// $('inyrForm').action="inyr.do?action=export";
		// 供应商预估进退查询/Supplier Inyr Inquiry
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("unitid", "ALL"));
		receiveformParams.add(new BasicNameValuePair("butype", "byjv"));
		receiveformParams.add(new BasicNameValuePair("systemdate", DateUtil.toString(Utils.getEndDate(Constants.RETAILER_CARREFOUR), "yyyy/MM/dd"))); // yyyy/mm/dd
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
			String receiveFilePath = Utils.getProperty(user.getRetailer() + Constants.RECEIVING_INBOUND_PATH);
			FileUtil.createFolder(receiveFilePath);
			String receiveFileNm = "Receiving_" + user.getRetailer() + "_" + user.getUserId() + "_" + DateUtil.toStringYYYYMMDD(new Date()) + ".xls";
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
		} else {
			log.info(user + "家乐福收货单Excel下载失败!");
		}
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_CARREFOUR));

	}



	public OCR getOcr() {
		return ocr;
	}

	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}
}
