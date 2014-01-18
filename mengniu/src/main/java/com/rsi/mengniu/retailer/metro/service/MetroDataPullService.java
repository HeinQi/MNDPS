package com.rsi.mengniu.retailer.metro.service;

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

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.retailer.common.service.RetailerDataPullService;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
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
			log.error(user + Utils.getTrace(e));
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		// https://portal.metro-link.com/cleartrust/ct_logon.asp
		log.info(user + "开始登录...");
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
			log.info(user + "登录失败,请检查登录名和密码!");
			return "InvalidPassword";
		}
		log.info(user + "登录成功!");
		return "Success";
	}

	public void getReceive(CloseableHttpClient httpClient, User user) throws Exception {
		log.info("下载收货单");
		/*
		 * // https://portal.metro-link.com:443/irj/portal/mcc HttpGet httpGet = new HttpGet("http://portal.metro-link.com/irj/portal");
		 * httpGet.addHeader("Accept-Language", "zh-CN"); httpGet.addHeader("User-Agent",
		 * "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)"); CloseableHttpResponse response =
		 * httpClient.execute(httpGet); String responseStr = EntityUtils.toString(response.getEntity()); response.close();
		 * 
		 * String str = responseStr.substring(responseStr.indexOf("afpVerifierKey = '")+18); String afpVerifierKey =
		 * str.substring(0,str.indexOf("'")); str = str.substring(str.indexOf("sap-ep-inp\":\"")+13); String sap_ep_inp =
		 * str.substring(0,str.indexOf("\"")); str = str.substring(str.indexOf("sap-ep-pp\":\"")+12); String sap_ep_pp =
		 * str.substring(0,str.indexOf("\"")); str = str.substring(str.indexOf("sap-ep-nh\":\"")+12); String sap_ep_nh =
		 * str.substring(0,str.indexOf("\"")); str = str.substring(str.indexOf("sap-ep-ur\":\"")+12); String sap_ep_ur =
		 * str.substring(0,str.indexOf("\""));;
		 * 
		 * 
		 * String url =
		 * "https://portal.metro-link.com/AFPServlet/NavigationServlet?action=getSelectedPathTree&mode=nogzip&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&targetNodeId=&pathname=%2Firj%2Fportal%2Fmcc&sap-ep-inp="
		 * + sap_ep_inp + "&sap-ep-nh=" + sap_ep_nh + "&sap-ep-pp=" + sap_ep_pp + "&sap-ep-ul=zh_CN&sap-ep-ur=" + sap_ep_ur + "&afpVerifierKey=" +
		 * afpVerifierKey; System.out.println(url); HttpGet httpGet1 = new HttpGet(url); httpGet1.addHeader("Accept-Language", "zh-CN");
		 * //httpGet1.addHeader("User-Agent",
		 * "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)"); CloseableHttpResponse response1
		 * = httpClient.execute(httpGet1); String responseStr1 = EntityUtils.toString(response1.getEntity()); //System.out.println(responseStr1);
		 * response1.close();
		 */
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
		String url2 = "https://portal.metro-link.com/irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1389359093558&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1389359093558&NavMode=0&PrevNavTarget=navurl%3A%2F%2Ff0a962e1bd92af95fc8eba4691680ae4";
		// /irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1389359093558&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1389359093558&NavMode=0&PrevNavTarget=navurl%3A%2F%2Ff0a962e1bd92af95fc8eba4691680ae4
		HttpPost receivePost = new HttpPost(url2);
		receivePost.addHeader("Accept-Language", "zh-CN");
		receivePost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr2 = EntityUtils.toString(receiveRes.getEntity());
		// System.out.println(responseStr2);
		receiveRes.close();
		Document doc1 = Jsoup.parse(responseStr2);
		String sap_ext_sid = doc1.select("input[name=sap-ext-sid]").first().attr("value");// sap-ext-sid
		String sap_wd_cltwndid = doc1.select("input[name=sap-wd-cltwndid]").first().attr("value");// sap-wd-cltwndid
		String sap_wd_tstamp = doc1.select("input[name=sap-wd-tstamp]").first().attr("value");// sap-wd-tstamp
		String PagePath = doc1.select("input[name=PagePath]").first().attr("value"); // PagePath"
		String sap_wd_app_namespace = doc1.select("input[name=sap-wd-app-namespace]").first().attr("value"); // sap-wd-app-namespace"
		String sap_ep_version = doc1.select("input[name=sap-ep-version]").first().attr("value"); // sap-ep-version"
		String sap_locale = doc1.select("input[name=sap-locale]").first().attr("value"); // sap-locale"
		String sap_accessibility = doc1.select("input[name=sap-accessibility]").first().attr("value"); // sap-accessibility"
		String sap_rtl = doc1.select("input[name=sap-rtl]").first().attr("value"); // sap-rtl"
		String sap_explanation = doc1.select("input[name=sap-explanation]").first().attr("value"); // sap-explanation"
		String sap_cssurl = doc1.select("input[name=sap-cssurl]").first().attr("value"); // sap-cssurl"
		String sap_cssversion = doc1.select("input[name=sap-cssversion]").first().attr("value"); // sap-cssversion"
		String sap_epcm_guid = doc1.select("input[name=sap-epcm-guid]").first().attr("value");// sap-epcm-guid"
		String restart = doc1.select("input[name=com.sap.portal.reserved.wd.pb.restart]").first().attr("value");// com.sap.portal.reserved.wd.pb.restart"
		String dynamicParameter = doc1.select("input[name=DynamicParameter]").first().attr("value"); // DynamicParameter"
		String supportInitialNavNodesFilter = doc1.select("input[name=supportInitialNavNodesFilter]").first().attr("value");// supportInitialNavNodesFilter"
		String navigationTarget = doc1.select("input[name=NavigationTarget]").first().attr("value"); // NavigationTarget"
		String navMode = doc1.select("input[name=NavMode]").first().attr("value");// NavMode"
		String executeLocally = doc1.select("input[name=ExecuteLocally]").first().attr("value"); // ExecuteLocally"
		String filterViewIdList = doc1.select("input[name=filterViewIdList]").first().attr("value");// filterViewIdList"
		String currentWindowId = doc1.select("input[name=CurrentWindowId]").first().attr("value"); // CurrentWindowId"
		String prevNavTarget = doc1.select("input[name=PrevNavTarget]").first().attr("value"); // PrevNavTarget"

		List<NameValuePair> receiveformParams3 = new ArrayList<NameValuePair>();
		receiveformParams3.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
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
		receiveformParams3.add(new BasicNameValuePair("sap-cssversion", sap_cssversion));
		receiveformParams3.add(new BasicNameValuePair("sap-epcm-guid", sap_epcm_guid));
		receiveformParams3.add(new BasicNameValuePair("com.sap.portal.reserved.wd.pb.restart", restart));
		receiveformParams3.add(new BasicNameValuePair("DynamicParameter", dynamicParameter));
		receiveformParams3.add(new BasicNameValuePair("supportInitialNavNodesFilter", supportInitialNavNodesFilter));
		receiveformParams3.add(new BasicNameValuePair("NavigationTarget", navigationTarget));
		receiveformParams3.add(new BasicNameValuePair("NavMode", navMode));
		receiveformParams3.add(new BasicNameValuePair("ExecuteLocally", executeLocally));
		receiveformParams3.add(new BasicNameValuePair("filterViewIdList", filterViewIdList));
		receiveformParams3.add(new BasicNameValuePair("CurrentWindowId", currentWindowId));
		receiveformParams3.add(new BasicNameValuePair("PrevNavTarget", prevNavTarget));

		HttpEntity receiveFormEntity3 = new UrlEncodedFormEntity(receiveformParams3, "UTF-8");
		String url3 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost3 = new HttpPost(url3);
		receivePost3.addHeader("Accept-Language", "zh-CN");
		receivePost3
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

		receivePost3.setEntity(receiveFormEntity3);
		CloseableHttpResponse receiveRes3 = httpClient.execute(receivePost3);
		String responseStr3 = EntityUtils.toString(receiveRes3.getEntity());
		receiveRes3.close();
		// System.out.println(responseStr3);
		Document doc3 = Jsoup.parse(responseStr3);
		String sap_wd_secure_id = doc3.select("input[name=sap-wd-secure-id]").first().attr("value");// sap-wd-secure-id
		String sap_wd_norefresh = doc3.select("input[name=sap-wd-norefresh]").first().attr("value");// sap-wd-norefresh

		List<NameValuePair> receiveformParams4 = new ArrayList<NameValuePair>();
		receiveformParams4.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueue = "InputField_ValidateIdaaaa.GoodsRecView.inputReceivingDateFromValue"
				+ DateUtil.toString(Utils.getStartDate(Constants.RETAILER_METRO),"yyyy-M-d")
				+ "ClientActionsubmitAsyncurEventNameValidateForm_RequestId...formAsynctrueFocusInfo@{\"sFocussedId\": \"ls-datepicker\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams4.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueue));
		System.out.println(sapEventQueue);
		HttpEntity receiveFormEntity4 = new UrlEncodedFormEntity(receiveformParams4, "UTF-8");
		String url4 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost4 = new HttpPost(url4);
		receivePost4.addHeader("Accept-Language", "zh-CN");
		receivePost4
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost4.setEntity(receiveFormEntity4);
		CloseableHttpResponse receiveRes4 = httpClient.execute(receivePost4);
		String responseStr4 = EntityUtils.toString(receiveRes4.getEntity());
		receiveRes4.close();

		List<NameValuePair> receiveformParams5 = new ArrayList<NameValuePair>();
		receiveformParams5.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams5.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams5.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams5.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueueTo = "InputField_ValidateIdaaaa.GoodsRecView.inputReceivingDateToValue"
				+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_METRO),"yyyy-M-d")
				+ "ClientActionsubmitAsyncurEventNameValidateForm_RequestId...formAsynctrueFocusInfo@{\"sFocussedId\": \"ls-datepicker\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams5.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueTo));
		HttpEntity receiveFormEntity5 = new UrlEncodedFormEntity(receiveformParams5, "UTF-8");
		String url5 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost5 = new HttpPost(url5);
		receivePost5.addHeader("Accept-Language", "zh-CN");
		receivePost5
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost5.setEntity(receiveFormEntity5);
		CloseableHttpResponse receiveRes5 = httpClient.execute(receivePost5);
		String responseStr5 = EntityUtils.toString(receiveRes5.getEntity());
		receiveRes5.close();

		List<NameValuePair> receiveformParams6 = new ArrayList<NameValuePair>();
		receiveformParams6.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueueSubmit = "Button_PressIdaaaa.GoodsRecView.ButtonSearchClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ButtonSearch\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams6.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueSubmit));
		System.out.println(sapEventQueueSubmit);
		HttpEntity receiveFormEntity6 = new UrlEncodedFormEntity(receiveformParams6, "UTF-8");
		String url6 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost6 = new HttpPost(url6);
		receivePost6.addHeader("Accept-Language", "zh-CN");
		receivePost6
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost6.setEntity(receiveFormEntity6);
		CloseableHttpResponse receiveRes6 = httpClient.execute(receivePost6);
		String responseStr6 = EntityUtils.toString(receiveRes6.getEntity());
		receiveRes6.close();
		responseStr6 = responseStr6.substring(
				responseStr6.indexOf("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"urST3Bd urFontStd\""),
				responseStr6.indexOf("<div id=\"aaaa.GoodsRecView.tableGoodsRec-hiddenData\""));
		Document receiveResult = Jsoup.parse(responseStr6);
		Elements dataElements = receiveResult.select("tr[uDat]");
		// CheckBox_ChangeIdaaaa.GoodsRecView.checked_editor.0CheckedtrueContextPathGoodsRec.0urEventNameCHECKBOXCLICKButton_PressIdaaaa.GoodsRecView.ButtonShowGRLinesClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo%40%7B"sFocussedId"%3A%20"aaaa.GoodsRecView.ButtonShowGRLines"%7DHashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle
		List<ReceivingNoteTO> receiveList = new ArrayList<ReceivingNoteTO>();
		//select all
		String sapEventQueueDetail = "Button_PressIdaaaa.GoodsRecView.ToolBarButtonChoiceClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ToolBarButtonChoice\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
//		for (int i = 0; i < 2; i++) {
//			sapEventQueueDetail += "CheckBox_ChangeIdaaaa.GoodsRecView.checked_editor."+i+"CheckedtrueContextPathGoodsRec."+i+"urEventNameCHECKBOXCLICK";
//		}
		if (sapEventQueueDetail != "") {
		getReceiveDetail(httpClient, sap_ext_sid, sap_wd_cltwndid, sap_wd_norefresh, sap_wd_secure_id,sapEventQueueDetail, receiveList);
		FileUtil.exportReceivingInfoToTXT(Constants.RETAILER_METRO, user.getUserId(), receiveList);
		}
	}

	public void getReceiveDetail(CloseableHttpClient httpClient, String sap_ext_sid, String sap_wd_cltwndid, String sap_wd_norefresh,
			String sap_wd_secure_id, String sapEventQueueDetail, List<ReceivingNoteTO> receiveList) throws Exception {
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		/*String sapEventQueueDetail = "CheckBox_ChangeIdaaaa.GoodsRecView.checked_editor."
				+ itemId
				+ "CheckedtrueContextPathGoodsRec."
				+ itemId
				+ "urEventNameCHECKBOXCLICKButton_PressIdaaaa.GoodsRecView.ButtonShowGRLinesClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ButtonShowGRLines\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		*/
		//sapEventQueueDetail += "Button_PressIdaaaa.GoodsRecView.ButtonShowGRLinesClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ButtonShowGRLines\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		System.out.println(sapEventQueueDetail);
		receiveformParams.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueDetail));
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		String url = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost = new HttpPost(url);
		receivePost.addHeader("Accept-Language", "zh-CN");
		receivePost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr = EntityUtils.toString(receiveRes.getEntity()); // 数据下载成功
		receiveRes.close();
		System.out.println(responseStr);
		responseStr = responseStr.substring(responseStr.indexOf("<![CDATA[") + 9, responseStr.indexOf("]]></content-update>"));
		Document detailResult = Jsoup.parse(responseStr);
		Elements dataElements = detailResult.select("tr[uDat]");
		for (Element row : dataElements) {
			Elements tds = row.select("td");
			ReceivingNoteTO receiveingTo = new ReceivingNoteTO();
			receiveingTo.setStoreNo(tds.get(2).select("span").first().text()); // 门店编号
			receiveingTo.setOrderNo(tds.get(3).select("span").first().text()); // 订单编号
			receiveingTo.setReceivingDate(tds.get(6).select("span").first().text()); // 收货日期
			receiveingTo.setItemCode(tds.get(8).select("span").first().text()); // 商品编号
			receiveingTo.setBarcode(tds.get(9).select("span").first().text()); // 条形码
			receiveingTo.setItemName(tds.get(10).select("span").first().text()); // 商品描述
			receiveingTo.setUnitPrice(tds.get(13).select("span").first().text()); // 进货价格
			receiveingTo.setQuantity(tds.get(15).select("span").first().text()); // 收货数量
			receiveingTo.setTotalPrice(tds.get(19).select("span").first().text()); // 净金额
			receiveList.add(receiveingTo);
		}

	}

	public void getOrder(CloseableHttpClient httpClient) throws Exception {
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("NavigationTarget", "navurl://c81bab1e37e8bb37e6c6ba0a74c170ef"));
		receiveformParams.add(new BasicNameValuePair("Command", "SUSPEND"));
		receiveformParams.add(new BasicNameValuePair("Embedded", "true"));
		receiveformParams.add(new BasicNameValuePair("SessionKeysAvailable", "true"));

		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		String url2 = "https://portal.metro-link.com/irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1389359093558&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1389359093558&NavMode=0&PrevNavTarget=navurl%3A%2F%2Ff0a962e1bd92af95fc8eba4691680ae4";
		// /irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1389359093558&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1389359093558&NavMode=0&PrevNavTarget=navurl%3A%2F%2Ff0a962e1bd92af95fc8eba4691680ae4
		HttpPost receivePost = new HttpPost(url2);
		receivePost.addHeader("Accept-Language", "zh-CN");
		receivePost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr2 = EntityUtils.toString(receiveRes.getEntity());
		// System.out.println(responseStr2);
		receiveRes.close();
		Document doc1 = Jsoup.parse(responseStr2);
		String sap_ext_sid = doc1.select("input[name=sap-ext-sid]").first().attr("value");// sap-ext-sid
		String sap_wd_cltwndid = doc1.select("input[name=sap-wd-cltwndid]").first().attr("value");// sap-wd-cltwndid
		String sap_wd_tstamp = doc1.select("input[name=sap-wd-tstamp]").first().attr("value");// sap-wd-tstamp
		String PagePath = doc1.select("input[name=PagePath]").first().attr("value"); // PagePath"
		String sap_wd_app_namespace = doc1.select("input[name=sap-wd-app-namespace]").first().attr("value"); // sap-wd-app-namespace"
		String sap_ep_version = doc1.select("input[name=sap-ep-version]").first().attr("value"); // sap-ep-version"
		String sap_locale = doc1.select("input[name=sap-locale]").first().attr("value"); // sap-locale"
		String sap_accessibility = doc1.select("input[name=sap-accessibility]").first().attr("value"); // sap-accessibility"
		String sap_rtl = doc1.select("input[name=sap-rtl]").first().attr("value"); // sap-rtl"
		String sap_explanation = doc1.select("input[name=sap-explanation]").first().attr("value"); // sap-explanation"
		String sap_cssurl = doc1.select("input[name=sap-cssurl]").first().attr("value"); // sap-cssurl"
		String sap_cssversion = doc1.select("input[name=sap-cssversion]").first().attr("value"); // sap-cssversion"
		String sap_epcm_guid = doc1.select("input[name=sap-epcm-guid]").first().attr("value");// sap-epcm-guid"
		String restart = doc1.select("input[name=com.sap.portal.reserved.wd.pb.restart]").first().attr("value");// com.sap.portal.reserved.wd.pb.restart"
		String dynamicParameter = doc1.select("input[name=DynamicParameter]").first().attr("value"); // DynamicParameter"
		String supportInitialNavNodesFilter = doc1.select("input[name=supportInitialNavNodesFilter]").first().attr("value");// supportInitialNavNodesFilter"
		String navigationTarget = doc1.select("input[name=NavigationTarget]").first().attr("value"); // NavigationTarget"
		String navMode = doc1.select("input[name=NavMode]").first().attr("value");// NavMode"
		String executeLocally = doc1.select("input[name=ExecuteLocally]").first().attr("value"); // ExecuteLocally"
		String filterViewIdList = doc1.select("input[name=filterViewIdList]").first().attr("value");// filterViewIdList"
		String currentWindowId = doc1.select("input[name=CurrentWindowId]").first().attr("value"); // CurrentWindowId"
		String prevNavTarget = doc1.select("input[name=PrevNavTarget]").first().attr("value"); // PrevNavTarget"

		List<NameValuePair> receiveformParams3 = new ArrayList<NameValuePair>();
		receiveformParams3.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
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
		receiveformParams3.add(new BasicNameValuePair("sap-cssversion", sap_cssversion));
		receiveformParams3.add(new BasicNameValuePair("sap-epcm-guid", sap_epcm_guid));
		receiveformParams3.add(new BasicNameValuePair("com.sap.portal.reserved.wd.pb.restart", restart));
		receiveformParams3.add(new BasicNameValuePair("DynamicParameter", dynamicParameter));
		receiveformParams3.add(new BasicNameValuePair("supportInitialNavNodesFilter", supportInitialNavNodesFilter));
		receiveformParams3.add(new BasicNameValuePair("NavigationTarget", navigationTarget));
		receiveformParams3.add(new BasicNameValuePair("NavMode", navMode));
		receiveformParams3.add(new BasicNameValuePair("ExecuteLocally", executeLocally));
		receiveformParams3.add(new BasicNameValuePair("filterViewIdList", filterViewIdList));
		receiveformParams3.add(new BasicNameValuePair("CurrentWindowId", currentWindowId));
		receiveformParams3.add(new BasicNameValuePair("PrevNavTarget", prevNavTarget));

		HttpEntity receiveFormEntity3 = new UrlEncodedFormEntity(receiveformParams3, "UTF-8");
		String url3 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost3 = new HttpPost(url3);
		receivePost3.addHeader("Accept-Language", "zh-CN");
		receivePost3
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

		receivePost3.setEntity(receiveFormEntity3);
		CloseableHttpResponse receiveRes3 = httpClient.execute(receivePost3);
		String responseStr3 = EntityUtils.toString(receiveRes3.getEntity());
		receiveRes3.close();
		// System.out.println(responseStr3);
		Document doc3 = Jsoup.parse(responseStr3);
		String sap_wd_secure_id = doc3.select("input[name=sap-wd-secure-id]").first().attr("value");// sap-wd-secure-id
		String sap_wd_norefresh = doc3.select("input[name=sap-wd-norefresh]").first().attr("value");// sap-wd-norefresh

		List<NameValuePair> receiveformParams4 = new ArrayList<NameValuePair>();
		receiveformParams4.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueue = "InputField_ValidateIdaaaa.GoodsRecView.inputReceivingDateFromValue"
				+ DateUtil.toString(Utils.getStartDate(Constants.RETAILER_METRO),"yyyy-M-d")
				+ "ClientActionsubmitAsyncurEventNameValidateForm_RequestId...formAsynctrueFocusInfo@{\"sFocussedId\": \"ls-datepicker\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams4.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueue));
		System.out.println(sapEventQueue);
		HttpEntity receiveFormEntity4 = new UrlEncodedFormEntity(receiveformParams4, "UTF-8");
		String url4 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost4 = new HttpPost(url4);
		receivePost4.addHeader("Accept-Language", "zh-CN");
		receivePost4
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost4.setEntity(receiveFormEntity4);
		CloseableHttpResponse receiveRes4 = httpClient.execute(receivePost4);
		String responseStr4 = EntityUtils.toString(receiveRes4.getEntity());
		receiveRes4.close();

		List<NameValuePair> receiveformParams5 = new ArrayList<NameValuePair>();
		receiveformParams5.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams5.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams5.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams5.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueueTo = "InputField_ValidateIdaaaa.GoodsRecView.inputReceivingDateToValue"
				+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_METRO),"yyyy-M-d")
				+ "ClientActionsubmitAsyncurEventNameValidateForm_RequestId...formAsynctrueFocusInfo@{\"sFocussedId\": \"ls-datepicker\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams5.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueTo));
		HttpEntity receiveFormEntity5 = new UrlEncodedFormEntity(receiveformParams5, "UTF-8");
		String url5 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost5 = new HttpPost(url5);
		receivePost5.addHeader("Accept-Language", "zh-CN");
		receivePost5
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost5.setEntity(receiveFormEntity5);
		CloseableHttpResponse receiveRes5 = httpClient.execute(receivePost5);
		String responseStr5 = EntityUtils.toString(receiveRes5.getEntity());
		receiveRes5.close();

		List<NameValuePair> receiveformParams6 = new ArrayList<NameValuePair>();
		receiveformParams6.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueueSubmit = "Button_PressIdaaaa.GoodsRecView.ButtonSearchClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ButtonSearch\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams6.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueSubmit));
		System.out.println(sapEventQueueSubmit);
		HttpEntity receiveFormEntity6 = new UrlEncodedFormEntity(receiveformParams6, "UTF-8");
		String url6 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost6 = new HttpPost(url6);
		receivePost6.addHeader("Accept-Language", "zh-CN");
		receivePost6
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost6.setEntity(receiveFormEntity6);
		CloseableHttpResponse receiveRes6 = httpClient.execute(receivePost6);
		String responseStr6 = EntityUtils.toString(receiveRes6.getEntity());
		receiveRes6.close();

		
	}
}
