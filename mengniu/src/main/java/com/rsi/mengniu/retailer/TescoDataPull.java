package com.rsi.mengniu.retailer;

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

import com.rsi.mengniu.User;
import com.rsi.mengniu.util.OCR;
import com.rsi.mengniu.util.Utils;
//https://tesco.chinab2bi.com/security/login.hlt
public class TescoDataPull implements RetailerDataPull {
	private static Log log = LogFactory.getLog(TescoDataPull.class);
	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		login(httpClient,user);
	}
	public String login(CloseableHttpClient httpClient, User user) {
		try {
			List<NameValuePair> formParams = new ArrayList<NameValuePair>();
			formParams.add(new BasicNameValuePair("j_username", user.getUserId()));
			formParams.add(new BasicNameValuePair("j_password", user.getPassword())); // 错误的密码
			HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
			HttpPost httppost = new HttpPost("https://tesco.chinab2bi.com/j_spring_security_check");
			httppost.setEntity(loginEntity);
			CloseableHttpResponse loginResponse = httpClient.execute(httppost);
			String responseStr = EntityUtils.toString(loginResponse.getEntity());
			System.out.println(responseStr);
//			if (responseStr.contains("验证码失效")) {
//				log.error("验证码失效,Relogin...");
//				return "InvalidCode";
//			} else if (responseStr.contains("错误的密码")) {
//				log.error("错误的密码,退出!" + user);
//				return "InvalidPassword";
//			} else if (responseStr.contains("系统出错")) {
//				log.error("系统出错,退出!");
//				return "SystemError";
//			}
			loginResponse.close();
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
		return "Success";
	}
}
