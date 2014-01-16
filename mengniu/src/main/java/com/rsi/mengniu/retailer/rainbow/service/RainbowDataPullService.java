package com.rsi.mengniu.retailer.rainbow.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
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
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.Utils;

public class RainbowDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(RainbowDataPullService.class);

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			String returnType = this.login(httpClient, user);
			if (!"Success".equals(returnType)) {
				return;
			}


		} catch (Exception e) {
			log.error(user + Utils.getTrace(e));
		}
		try {
			this.getReceiving(httpClient, user);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private String login(CloseableHttpClient httpClient, User user)
			throws Exception {
		log.info(user + "开始登录...");
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("loginfile",
				"/login/Login.jsp?logintype=1"));
		formParams.add(new BasicNameValuePair("logintype", "1"));
		formParams.add(new BasicNameValuePair("loginid", user.getUserId()));
		formParams.add(new BasicNameValuePair("userpassword", user
				.getPassword())); // 错误的密码
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");

		HttpPost httppost = new HttpPost(
				"http://vd.rainbow.cn:8080/login/VerifyLogin.jsp");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);

		loginResponse.close();

		// forward
		log.info(loginResponse.getFirstHeader("location").getValue());
		HttpGet httpGet = new HttpGet(loginResponse.getFirstHeader("location")
				.getValue());
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		String forwardStr = EntityUtils.toString(entity);

		if (forwardStr.contains("用户名或密码错误")) {
			log.info(user + "用户名或密码错误,退出!");
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

	private void getReceiving(CloseableHttpClient httpClient, User user)
			throws Exception {

		HttpGet httpGet = new HttpGet(
				"http://vd.rainbow.cn:8080/object/getAdvReportData.jsp"
						+ "?rptId=3124"
						+ "&showSelectionModel=false"
						+ "&html=Y"
						+ "&sqlWhere=&where=%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3E=to_date('"
						+ DateUtil.toString(Utils.getStartDate(Constants.RETAILER_RAINBOW))
						+ "','yyyy-mm-dd')%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3C=to_date('"
						+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_RAINBOW))
						+ "','yyyy-mm-dd')");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		String responseStr = EntityUtils.toString(entity);
		log.info(responseStr);
		
		response.close();
		
		
		Document doc = Jsoup.parse(responseStr);
		Elements rowsElements = doc.select("#dataTable").first().select("tr:gt(0)");
		
		for(Element rowElement : rowsElements){
			Elements tdElements = rowElement.select("td");
			
		}
		

	}
}
