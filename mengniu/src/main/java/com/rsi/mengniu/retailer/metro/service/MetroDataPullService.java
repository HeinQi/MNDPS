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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.Utils;

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
			log.error(user+Utils.getTrace(e));
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
		//System.out.println(responseStr1);
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
		receiveformParams.add(new BasicNameValuePair("Command", "SUSPEND"));
		receiveformParams.add(new BasicNameValuePair("Embedded", "true"));
		receiveformParams.add(new BasicNameValuePair("SessionKeysAvailable", "true"));
		
		
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		String url2 ="https://portal.metro-link.com/irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1389359093558&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1389359093558&NavMode=0&PrevNavTarget=navurl%3A%2F%2Ff0a962e1bd92af95fc8eba4691680ae4";
		                                           // /irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1389359093558&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1389359093558&NavMode=0&PrevNavTarget=navurl%3A%2F%2Ff0a962e1bd92af95fc8eba4691680ae4
		HttpPost receivePost = new HttpPost(url2);
		receivePost.addHeader("Accept-Language", "zh-CN");
		receivePost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr2 = EntityUtils.toString(receiveRes.getEntity());
		//System.out.println(responseStr2);
		receiveRes.close();
		Document doc1 = Jsoup.parse(responseStr2);
		String sap_ext_sid = doc1.select("input[name=sap-ext-sid]").first().attr("value");//sap-ext-sid
		String sap_wd_cltwndid = doc1.select("input[name=sap-wd-cltwndid]").first().attr("value");//sap-wd-cltwndid
		String sap_wd_tstamp = doc1.select("input[name=sap-wd-tstamp]").first().attr("value");//sap-wd-tstamp
		String PagePath = doc1.select("input[name=PagePath]").first().attr("value");		//PagePath" 
		String sap_wd_app_namespace = doc1.select("input[name=sap-wd-app-namespace]").first().attr("value"); 	//sap-wd-app-namespace"  
		String sap_ep_version = doc1.select("input[name=sap-ep-version]").first().attr("value");		//sap-ep-version"  
		String sap_locale = doc1.select("input[name=sap-locale]").first().attr("value"); //sap-locale"  
		String sap_accessibility = doc1.select("input[name=sap-accessibility]").first().attr("value");		//sap-accessibility"  
		String sap_rtl = doc1.select("input[name=sap-rtl]").first().attr("value"); //sap-rtl"
		String sap_explanation = doc1.select("input[name=sap-explanation]").first().attr("value"); //sap-explanation"
		String sap_cssurl = doc1.select("input[name=sap-cssurl]").first().attr("value"); //sap-cssurl" 
		String sap_cssversion = doc1.select("input[name=sap-cssversion]").first().attr("value"); //sap-cssversion" 
		String sap_epcm_guid = doc1.select("input[name=sap-epcm-guid]").first().attr("value");//sap-epcm-guid" 
		String restart = doc1.select("input[name=com.sap.portal.reserved.wd.pb.restart]").first().attr("value");//com.sap.portal.reserved.wd.pb.restart" 
		String dynamicParameter = doc1.select("input[name=DynamicParameter]").first().attr("value");		//DynamicParameter" 
		String supportInitialNavNodesFilter = doc1.select("input[name=supportInitialNavNodesFilter]").first().attr("value");//supportInitialNavNodesFilter" 
		String navigationTarget = doc1.select("input[name=NavigationTarget]").first().attr("value");		//NavigationTarget" 
		String navMode = doc1.select("input[name=NavMode]").first().attr("value");//NavMode" 
		String executeLocally = doc1.select("input[name=ExecuteLocally]").first().attr("value");		//ExecuteLocally" 
		String filterViewIdList = doc1.select("input[name=filterViewIdList]").first().attr("value");//filterViewIdList"  
		String currentWindowId = doc1.select("input[name=CurrentWindowId]").first().attr("value");		//CurrentWindowId" 
		String prevNavTarget = doc1.select("input[name=PrevNavTarget]").first().attr("value");		//PrevNavTarget"  
		
		
		List<NameValuePair> receiveformParams3 = new ArrayList<NameValuePair>();
		receiveformParams3.add(new BasicNameValuePair("sap-ext-sid",sap_ext_sid));
		receiveformParams3.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams3.add(new BasicNameValuePair("sap-wd-tstamp", sap_wd_tstamp));
		receiveformParams3.add(new BasicNameValuePair("PagePath", PagePath));
		receiveformParams3.add(new BasicNameValuePair("sap-wd-app-namespace", sap_wd_app_namespace));
		receiveformParams3.add(new BasicNameValuePair("sap-ep-version", sap_ep_version));
		receiveformParams3.add(new BasicNameValuePair("sap-locale", sap_locale));
		receiveformParams3.add(new BasicNameValuePair("sap-accessibility", sap_accessibility));
		receiveformParams3.add(new BasicNameValuePair("sap-rtl", sap_rtl));
		receiveformParams3.add(new BasicNameValuePair("sap-explanation", sap_explanation));
		receiveformParams3.add(new BasicNameValuePair("sap-cssurl", sap_cssurl));
		receiveformParams3.add(new BasicNameValuePair("sap-cssversion",sap_cssversion ));
		receiveformParams3.add(new BasicNameValuePair("sap-epcm-guid", sap_epcm_guid));
		receiveformParams3.add(new BasicNameValuePair("com.sap.portal.reserved.wd.pb.restart",restart ));
		receiveformParams3.add(new BasicNameValuePair("DynamicParameter", dynamicParameter));
		receiveformParams3.add(new BasicNameValuePair("supportInitialNavNodesFilter", supportInitialNavNodesFilter));
		receiveformParams3.add(new BasicNameValuePair("NavigationTarget", navigationTarget));
		receiveformParams3.add(new BasicNameValuePair("NavMode", navMode));
		receiveformParams3.add(new BasicNameValuePair("ExecuteLocally",executeLocally ));
		receiveformParams3.add(new BasicNameValuePair("filterViewIdList", filterViewIdList));
		receiveformParams3.add(new BasicNameValuePair("CurrentWindowId", currentWindowId));
		receiveformParams3.add(new BasicNameValuePair("PrevNavTarget", prevNavTarget));
		
		
		HttpEntity receiveFormEntity3 = new UrlEncodedFormEntity(receiveformParams3, "UTF-8");
		String url3 ="https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost3 = new HttpPost(url3);
		receivePost3.addHeader("Accept-Language", "zh-CN");
		receivePost3.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		
		receivePost3.setEntity(receiveFormEntity3);
		CloseableHttpResponse receiveRes3 = httpClient.execute(receivePost3);
		String responseStr3 = EntityUtils.toString(receiveRes3.getEntity());
		receiveRes3.close();		
        System.out.println(responseStr3);        
        Document doc3 = Jsoup.parse(responseStr3);
		String sap_wd_secure_id = doc3.select("input[name=sap-wd-secure-id]").first().attr("value");//sap-wd-secure-id
		System.out.println(sap_wd_secure_id);
		
		
		
 
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
