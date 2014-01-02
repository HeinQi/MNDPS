package com.rsi.mengniu.retailer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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

//http://supplierweb.carrefour.com.cn/
public class CarrefourDataPull implements RetailerDataPull {
	private static Log log = LogFactory.getLog(CarrefourDataPull.class);

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String loginResult = null;
		int loginCount = 0; // 如果验证码出错重新login,最多20次
		do {
			loginResult = login(httpClient, user);
			loginCount++;
		} while ("InvalidCode".equals(loginResult) && loginCount < 20);
		// Invalid Password and others
		if (!"Success".equals(loginResult)) {
			return;
		}

	}

	public String login(CloseableHttpClient httpClient, User user) {
		HttpGet httpGet = new HttpGet("http://supplierweb.carrefour.com.cn/includes/image.jsp");
		String imgName = String.valueOf(java.lang.System.currentTimeMillis());
		try {
			FileOutputStream fos = new FileOutputStream("/Users/haibin/Documents/workspace/mengniu/temp/" + imgName + ".jpg");
			CloseableHttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			entity.writeTo(fos);
			response.close();
			fos.close();
			String recognizeStr = OCR.getInstance().recognizeText("/Users/haibin/Documents/workspace/mengniu/temp/" + imgName + ".jpg",
					"/Users/haibin/Documents/workspace/mengniu/temp/" + imgName);
			recognizeStr = recognizeStr.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
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
				log.error("验证码失效,Relogin...");
				return "InvalidCode";
			} else if (responseStr.contains("错误的密码")) {
				log.error("错误的密码,退出!" + user);
				return "InvalidPassword";
			} else if (responseStr.contains("系统出错")) {
				log.error("系统出错,退出!");
				return "SystemError";
			}
			loginResponse.close();
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
		return "Success";
	}

	public void getReceiveExcel(CloseableHttpClient httpClient) {
		// goMenu('inyr.do?action=query','14','预估进退查询')

		// $('inyrForm').action="inyr.do?action=export";
		// 供应商预估进退查询/Supplier Inyr Inquiry
		try {
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
				FileOutputStream receiveFos = new FileOutputStream("/Users/haibin/Documents/workspace/mengniu/temp/" + inyrFileName);

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
				System.out.println("Export Error!");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
