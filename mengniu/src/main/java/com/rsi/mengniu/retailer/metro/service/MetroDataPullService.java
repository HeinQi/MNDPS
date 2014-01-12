package com.rsi.mengniu.retailer.metro.service;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
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

import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.Utils;

//http://vss.yonghui.cn:9999/vss/logon/logon.jsp
public class MetroDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(MetroDataPullService.class);

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();

		try {
			login(httpClient, user);

			// receive
			getReceive(httpClient, user);
			// order
			// getOrder(httpClient);
			httpClient.close();
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		// https://portal.metro-link.com/cleartrust/ct_logon.asp
		log.info("开始登录..." + user);
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("user", user.getUserId()));
		formParams.add(new BasicNameValuePair("password", user.getPassword()));
		formParams.add(new BasicNameValuePair("auth_mode", "SECURID"));
		formParams.add(new BasicNameValuePair("language", "cn"));
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("https://portal.metro-link.com/cleartrust/ct_logon.asp");

		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		// forward
		HttpGet httpGet = new HttpGet("https://portal.metro-link.com" + loginResponse.getFirstHeader("location").getValue());
		httpGet.addHeader("Accept-Language", "zh-CN");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		String responseStr = EntityUtils.toString(response.getEntity());
		loginResponse.close();
		response.close();
		if (!responseStr.contains("Login Successful")) {
			log.info("登录失败,请检查登录名和密码!" + user);
			return "InvalidPassword";
		}
		log.info("登录成功!" + user);
		return "Success";
	}

	public void getReceive(CloseableHttpClient httpClient, User user) throws Exception {
		log.info("下载收货单");
		// https://portal.metro-link.com:443/irj/portal/mcc
		HttpGet httpGet = new HttpGet("http://portal.metro-link.com/irj/portal");
		httpGet.addHeader("Accept-Language", "zh-CN");
		httpGet.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		String responseStr = EntityUtils.toString(response.getEntity());
		response.close();
		
		String str = responseStr.substring(responseStr.indexOf("afpVerifierKey = '")+18);
		String afpVerifierKey = str.substring(0,str.indexOf("'"));
		str = str.substring(str.indexOf("sap-ep-inp\":\"")+13);
		String sap_ep_inp = str.substring(0,str.indexOf("\""));
		str = str.substring(str.indexOf("sap-ep-pp\":\"")+12);
		String sap_ep_pp = str.substring(0,str.indexOf("\""));
		str = str.substring(str.indexOf("sap-ep-nh\":\"")+12);
		String sap_ep_nh = str.substring(0,str.indexOf("\""));
		str = str.substring(str.indexOf("sap-ep-ur\":\"")+12);
		String sap_ep_ur = str.substring(0,str.indexOf("\""));;
		

		String url = "https://portal.metro-link.com/AFPServlet/NavigationServlet?action=getSelectedPathTree&mode=nogzip&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&targetNodeId=&pathname=%2Firj%2Fportal%2Fmcc&sap-ep-inp="
				+ sap_ep_inp
				+ "&sap-ep-nh="
				+ sap_ep_nh
				+ "&sap-ep-pp="
				+ sap_ep_pp
				+ "&sap-ep-ul=zh_CN&sap-ep-ur="
				+ sap_ep_ur
				+ "&afpVerifierKey="
				+ afpVerifierKey;
		System.out.println(url);
		HttpGet httpGet1 = new HttpGet(url);
		httpGet1.addHeader("Accept-Language", "zh-CN");
		//httpGet1.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		CloseableHttpResponse response1 = httpClient.execute(httpGet1);
		String responseStr1 = EntityUtils.toString(response1.getEntity());
		System.out.println(responseStr1);
		response1.close();
		/**
		 * 
		 * "action" := "getSelectedPathTree", "mode" := "nogzip", "supportInitialNavNodesFilter" := "true", "filterViewIdList" := ";mcc;common;",
		 * "targetNodeId" := "", "pathname" := "/irj/portal/mcc", "sap-ep-inp" := "", "sap-ep-nh" := "1386590678518", "sap-ep-pp" := "", "sap-ep-ul"
		 * := "zh_CN", "sap-ep-ur" := "69c3b503ebc19d4447da62ccf041c28b", "afpVerifierKey" := "d87d8a7ccce7e4f870f684cfe4575adf";
		 */

		/*
		 * sap-ext-sid:8bYti*zClHwZHwXfrCHo6A--qvel*Z7akaxIul*bBSNpTA-- sap-wd-cltwndid:WID1389278326411 sap-wd-norefresh:X
		 * sap-wd-secure-id:kWy9KBHx7eFt5TZYysnpjw==
		*/
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("NavigationTarget", "navurl://c81bab1e37e8bb37e6c6ba0a74c170ef"));
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		String url2 ="https://portal.metro-link.com/irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1389359093558&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1389359093558&NavMode=0&PrevNavTarget=navurl%3A%2F%2Ff0a962e1bd92af95fc8eba4691680ae4";
		HttpPost receivePost = new HttpPost(url2);
		receivePost.addHeader("Accept-Language", "zh-CN");
		receivePost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		Header[] heads = receiveRes.getAllHeaders();
		for (Header head : heads) {
			System.out.println(head.getName());
			System.out.println(head.getValue());

		}
		responseStr = EntityUtils.toString(receiveRes.getEntity());
		System.out.println(responseStr);
		receiveRes.close();
 
	}

	public void getOrder(CloseableHttpClient httpClient) throws Exception {
		// http://vss.yonghui.cn:9999/vss/DownloadSheet?orderdate_min=2014-01-01&orderdate_max=2014-01-05&operation=eptOrderSheet
		String startDate = Utils.getProperty("yonghui.startDate");
		String endDate = Utils.getProperty("yonghui.endDate");
		FileOutputStream orderFos = new FileOutputStream("/Users/haibin/Documents/temp/order.xls");
		HttpGet httpGet = new HttpGet("http://vss.yonghui.cn:9999/vss/DownloadSheet?orderdate_min=" + startDate + "&orderdate_max=" + endDate
				+ "&operation=eptOrderSheet");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		response.getEntity().writeTo(orderFos);
		response.close();
	}
}
