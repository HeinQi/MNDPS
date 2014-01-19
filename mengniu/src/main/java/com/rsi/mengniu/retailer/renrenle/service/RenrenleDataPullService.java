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
			
			getSalesExcel(httpClient, user);
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

	public void getSalesExcel(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始下载销售数据...");
		//http://www.renrenle.cn/scm/sale/saleAction.do?method=querySale&download=1
		String salesFilePath = Utils.getProperty(Constants.RETAILER_RENRENLE + Constants.SALES_PATH);
		FileUtil.createFolder(salesFilePath);
		String receiveFileNm = "Sales_" + Constants.RETAILER_RENRENLE + "_" + user.getUserId() + "_" + DateUtil.toStringYYYYMMDD(Utils.getStartDate(Constants.RETAILER_RENRENLE)) + ".xls";
		FileOutputStream salseFos = new FileOutputStream(salesFilePath + receiveFileNm);

		String searchDate = DateUtil.toString(Utils.getStartDate(Constants.RETAILER_RENRENLE),"yyyy-MM-dd");
		List<NameValuePair> salesformParams = new ArrayList<NameValuePair>();
		salesformParams.add(new BasicNameValuePair("searchDate", searchDate));
		salesformParams.add(new BasicNameValuePair("searchType", "0"));
		salesformParams.add(new BasicNameValuePair("saleType", "0"));
		salesformParams.add(new BasicNameValuePair("orderASC", "false"));
		HttpEntity salesFormEntity = new UrlEncodedFormEntity(salesformParams, "UTF-8");
		HttpPost salesPost = new HttpPost("http://www.renrenle.cn/scm/sale/saleAction.do?method=querySale&download=1");
		salesPost.setEntity(salesFormEntity);
		CloseableHttpResponse downloadRes = httpClient.execute(salesPost);
		downloadRes.getEntity().writeTo(salseFos);
		downloadRes.close();
		salseFos.close();
		log.info(user + "销售数据下载成功");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RENRENLE));

	}



	public OCR getOcr() {
		return ocr;
	}

	public void setOcr(OCR ocr) {
		this.ocr = ocr;
	}
}
