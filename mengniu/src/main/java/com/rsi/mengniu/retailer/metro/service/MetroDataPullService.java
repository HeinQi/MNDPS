package com.rsi.mengniu.retailer.metro.service;

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
import com.rsi.mengniu.retailer.module.OrderTO;
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
			String loginResult = login(httpClient, user);
			// Invalid Password and others
			if (!"Success".equals(loginResult)) {
				return;
			}

			// receive
			getReceive(httpClient, user);
			// order
			getOrder(httpClient,user);
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
		if (responseStr.contains("Please check your User ID and/or your Passcode")) {
			log.info(user + "登录失败,请检查登录名和密码!");
			Utils.recordIncorrectUser(user);
			return "InvalidPassword";
		}		
		if (!responseStr.contains("Login Successful")) {
			log.info(user + "网站登录失败,请检查网站,退出");
			return "Error";
		}
		log.info(user + "登录成功!");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		return "Success";
	}

	public void getReceive(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user+"下载收货单...");

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

		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		
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
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		
		// From Date
		List<NameValuePair> receiveformParams4 = new ArrayList<NameValuePair>();
		receiveformParams4.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams4.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueue = "InputField_ValidateIdaaaa.GoodsRecView.inputReceivingDateFromValue"
				+ DateUtil.toString(Utils.getStartDate(Constants.RETAILER_METRO),"yyyy-M-d")
				+ "ClientActionsubmitAsyncurEventNameValidateForm_RequestId...formAsynctrueFocusInfo@{\"sFocussedId\": \"ls-datepicker\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams4.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueue));
		HttpEntity receiveFormEntity4 = new UrlEncodedFormEntity(receiveformParams4, "UTF-8");
		HttpPost receivePost4 = new HttpPost(url3);
		receivePost4.addHeader("Accept-Language", "zh-CN");
		receivePost4
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost4.setEntity(receiveFormEntity4);
		CloseableHttpResponse receiveRes4 = httpClient.execute(receivePost4);
		String responseStr4 = EntityUtils.toString(receiveRes4.getEntity());
		receiveRes4.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		// To Date
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
		HttpPost receivePost5 = new HttpPost(url3);
		receivePost5.addHeader("Accept-Language", "zh-CN");
		receivePost5
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost5.setEntity(receiveFormEntity5);
		CloseableHttpResponse receiveRes5 = httpClient.execute(receivePost5);
		String responseStr5 = EntityUtils.toString(receiveRes5.getEntity());
		receiveRes5.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		// Search Max500rows  查询结果包含多条记录 
		List<NameValuePair> receiveformParams6 = new ArrayList<NameValuePair>();
		receiveformParams6.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams6.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueueSubmit = "ComboBox_SelectIdaaaa.GoodsRecView.DropDownByKeyMaxResultKey500ByEnterfalseurEventNameCOMBOBOXSELECTIONCHANGEButton_PressIdaaaa.GoodsRecView.ButtonSearchClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ButtonSearch\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams6.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueSubmit));
		HttpEntity receiveFormEntity6 = new UrlEncodedFormEntity(receiveformParams6, "UTF-8");
		HttpPost receivePost6 = new HttpPost(url3);
		receivePost6.setEntity(receiveFormEntity6);
		CloseableHttpResponse receiveRes6 = httpClient.execute(receivePost6);
		String responseStr6 = EntityUtils.toString(receiveRes6.getEntity());
		receiveRes6.close();
		if (responseStr6.contains("查询结果包含多条记录")) {
			log.info(user+"查询结果超过500条，请减小日期区间!");
			
		} else if (responseStr6.contains("没有查询到符合条件的数据")) {
			log.info(user+"没有查询到符合条件的收货单数据!");
			return;
		}
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));		
		//Select ALL
		List<NameValuePair> receiveformParams7 = new ArrayList<NameValuePair>();
		receiveformParams7.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams7.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams7.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams7.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String selectAll = "Button_PressIdaaaa.GoodsRecView.ToolBarButtonChoiceClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ToolBarButtonChoice\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams7.add(new BasicNameValuePair("SAPEVENTQUEUE", selectAll));
		HttpEntity receiveFormEntity7 = new UrlEncodedFormEntity(receiveformParams7, "UTF-8");
		HttpPost receivePost7 = new HttpPost(url3);
		receivePost7.setEntity(receiveFormEntity7);
		CloseableHttpResponse receiveRes7 = httpClient.execute(receivePost7);
		receiveRes7.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));		
		
		//显示已交货订单明细
		List<NameValuePair> receiveformParams8 = new ArrayList<NameValuePair>();
		receiveformParams8.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams8.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams8.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams8.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String diplayAll = "Button_PressIdaaaa.GoodsRecView.ButtonShowGRLinesClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.GoodsRecView.ButtonShowGRLines\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams8.add(new BasicNameValuePair("SAPEVENTQUEUE", diplayAll));
		HttpEntity receiveFormEntity8 = new UrlEncodedFormEntity(receiveformParams8, "UTF-8");
		HttpPost receivePost8 = new HttpPost(url3);
		receivePost8.addHeader("Accept-Language", "zh-CN");
		receivePost8.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		receivePost8.setEntity(receiveFormEntity8);
		CloseableHttpResponse receiveRes8 = httpClient.execute(receivePost8);
		String receiveDetailRes = EntityUtils.toString(receiveRes8.getEntity()); 
		receiveRes8.close();
		if (receiveDetailRes.contains("数据下载成功")) {
			receiveDetailRes = receiveDetailRes.substring(receiveDetailRes.indexOf("<![CDATA[") + 9, receiveDetailRes.indexOf("]]></content-update>"));
			Document detailResult = Jsoup.parse(receiveDetailRes);
			Element pageElement = detailResult.select("div[ct=SCB]").first();
			String pageStr = pageElement.attr("lsdata");
			pageStr = pageStr.substring(pageStr.indexOf("ROW")+8,pageStr.lastIndexOf("}"));
			int record = Integer.valueOf(pageStr);
			log.info(user+"收货单明细共有 "+record+" 行");
			int page = record % 10 > 0 ? record / 10 + 1 : record / 10;
			List<ReceivingNoteTO> receiveList = new ArrayList<ReceivingNoteTO>();
			getReceiveDetailByPage(receiveDetailRes,1,record,receiveList,user);
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));	
			for (int i=2; i<=page; i++) {
				int startRow = i*10-9;
				String detailStr = getReceiveDetailByPage(httpClient,sap_ext_sid,sap_wd_cltwndid,sap_wd_norefresh,sap_wd_secure_id,startRow);
				detailStr = detailStr.substring(detailStr.indexOf("<![CDATA[") + 9, detailStr.indexOf("]]></content-update>"));
				getReceiveDetailByPage(detailStr,startRow,record,receiveList,user);
			}
			FileUtil.exportReceivingInfoToTXT(Constants.RETAILER_METRO, user.getUserId(), receiveList);
			log.info(user + "收货单下载成功!");
		} else {
			log.info(user+"下载收货单失败!");
		}
		
	}

	public String getReceiveDetailByPage(CloseableHttpClient httpClient, String sap_ext_sid, String sap_wd_cltwndid, String sap_wd_norefresh,
			String sap_wd_secure_id,int startRow) throws Exception {
		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		receiveformParams.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		receiveformParams.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		receiveformParams.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueuePage = "SapTable_VerticalScrollIdaaaa.GoodsRecLinesView.tableGRLineFirstVisibleItemIndex"+startRow+"ActionDIRECTCellIdAccessTypeSCROLLBARSelectionFollowFocusfalseShiftfalseCtrlfalseAltfalseClientActionsubmiturEventNameVerticalScrollForm_RequestId...formAsyncfalseFocusInfo@{}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		receiveformParams.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueuePage));
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		String url = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost receivePost = new HttpPost(url);
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
		String responseStr = EntityUtils.toString(receiveRes.getEntity()); // 数据下载成功
		receiveRes.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));	
		return responseStr;
	}	
	
	
	private void getReceiveDetailByPage(String responseStr,int startRow,int record,List<ReceivingNoteTO> receiveList,User user) {
		log.info(user+"读取收货单明细 "+startRow+"-"+((startRow+9)>record?record:(startRow+9))+" 行");
		Document detailResult = Jsoup.parse(responseStr);
		
		for (int i=startRow; i<startRow+10 && i<=record;i++) {
			Elements dataElements = detailResult.select("tr[rr="+i+"]");
			Elements tds = dataElements.first().select("td");
			ReceivingNoteTO receiveingTo = new ReceivingNoteTO();
			receiveingTo.setStoreID(tds.get(2).select("span").first().text()); // 门店编号
			receiveingTo.setOrderNo(tds.get(3).select("span").first().text()); // 订单编号
			receiveingTo.setReceivingDate(tds.get(6).select("span").first().text()); // 收货日期
			receiveingTo.setItemID(tds.get(8).select("span").first().text()); // 商品编号
			receiveingTo.setBarcode(tds.get(9).select("span").first().text()); // 条形码
			receiveingTo.setItemName(tds.get(10).select("span").first().text()); // 商品描述
			receiveingTo.setUnitPrice(tds.get(13).select("span").first().text()); // 进货价格
			receiveingTo.setQuantity(tds.get(15).select("span").first().text()); // 收货数量
			receiveingTo.setTotalPrice(tds.get(19).select("span").first().text()); // 净金额
			receiveList.add(receiveingTo);
		}

	}

	public void getOrder(CloseableHttpClient httpClient,User user) throws Exception {
		log.info(user+"订单数据下载...");
		List<NameValuePair> orderformParams = new ArrayList<NameValuePair>();
		orderformParams.add(new BasicNameValuePair("NavigationTarget", "navurl://2acb0a958f2485c518851f9e303d829f"));
		orderformParams.add(new BasicNameValuePair("Command", "SUSPEND"));
		orderformParams.add(new BasicNameValuePair("Embedded", "true"));
		orderformParams.add(new BasicNameValuePair("SessionKeysAvailable", "true"));

		HttpEntity orderFormEntity = new UrlEncodedFormEntity(orderformParams, "UTF-8");
		String url1 = "https://portal.metro-link.com/irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1390061377525&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1390061377525&NavMode=0&PrevNavTarget=navurl%3A%2F%2Fc81bab1e37e8bb37e6c6ba0a74c170ef";
		           //  https://portal.metro-link.com/irj/servlet/prt/portal/prteventname/Navigate/prtroot/pcd!3aportal_content!2fevery_user!2fgeneral!2fdefaultAjaxframeworkContent!2fcom.sap.portal.contentarea?ExecuteLocally=true&CurrentWindowId=WID1390061377525&supportInitialNavNodesFilter=true&filterViewIdList=%3Bmcc%3Bcommon%3B&windowId=WID1390061377525&NavMode=0&PrevNavTarget=navurl%3A%2F%2Fc81bab1e37e8bb37e6c6ba0a74c170ef
		HttpPost orderPost = new HttpPost(url1);
		orderPost.addHeader("Accept-Language", "zh-CN");
		orderPost.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

		orderPost.setEntity(orderFormEntity);
		CloseableHttpResponse orderRes = httpClient.execute(orderPost);
		String responseStr1 = EntityUtils.toString(orderRes.getEntity());
		orderRes.close();
		Document doc1 = Jsoup.parse(responseStr1);
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
		
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		
		List<NameValuePair> orderformParams2 = new ArrayList<NameValuePair>();
		orderformParams2.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams2.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams2.add(new BasicNameValuePair("sap-wd-tstamp", sap_wd_tstamp));
		orderformParams2.add(new BasicNameValuePair("PagePath", PagePath));
		orderformParams2.add(new BasicNameValuePair("sap-wd-app-namespace", sap_wd_app_namespace));
		orderformParams2.add(new BasicNameValuePair("sap-ep-version", sap_ep_version));
		orderformParams2.add(new BasicNameValuePair("sap-locale", sap_locale));
		orderformParams2.add(new BasicNameValuePair("sap-accessibility", sap_accessibility));
		orderformParams2.add(new BasicNameValuePair("sap-rtl", sap_rtl));
		orderformParams2.add(new BasicNameValuePair("sap-explanation", sap_explanation));
		orderformParams2.add(new BasicNameValuePair("sap-cssurl", sap_cssurl));
		orderformParams2.add(new BasicNameValuePair("sap-cssversion", sap_cssversion));
		orderformParams2.add(new BasicNameValuePair("sap-epcm-guid", sap_epcm_guid));
		orderformParams2.add(new BasicNameValuePair("com.sap.portal.reserved.wd.pb.restart", restart));
		orderformParams2.add(new BasicNameValuePair("DynamicParameter", dynamicParameter));
		orderformParams2.add(new BasicNameValuePair("supportInitialNavNodesFilter", supportInitialNavNodesFilter));
		orderformParams2.add(new BasicNameValuePair("NavigationTarget", navigationTarget));
		orderformParams2.add(new BasicNameValuePair("NavMode", navMode));
		orderformParams2.add(new BasicNameValuePair("ExecuteLocally", executeLocally));
		orderformParams2.add(new BasicNameValuePair("filterViewIdList", filterViewIdList));
		orderformParams2.add(new BasicNameValuePair("CurrentWindowId", currentWindowId));
		orderformParams2.add(new BasicNameValuePair("PrevNavTarget", prevNavTarget));

		HttpEntity orderFormEntity2 = new UrlEncodedFormEntity(orderformParams2, "UTF-8");
		String url2 = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost orderPost2 = new HttpPost(url2);
		orderPost2.addHeader("Accept-Language", "zh-CN");
		orderPost2.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

		orderPost2.setEntity(orderFormEntity2);
		CloseableHttpResponse orderRes2 = httpClient.execute(orderPost2);
		String responseStr2 = EntityUtils.toString(orderRes2.getEntity());
		orderRes2.close();
		Document doc2 = Jsoup.parse(responseStr2);
		String sap_wd_secure_id = doc2.select("input[name=sap-wd-secure-id]").first().attr("value");// sap-wd-secure-id
		String sap_wd_norefresh = doc2.select("input[name=sap-wd-norefresh]").first().attr("value");// sap-wd-norefresh

		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		
		//StartDate
		List<NameValuePair> orderformParams3 = new ArrayList<NameValuePair>();
		orderformParams3.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams3.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams3.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams3.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueueFrom = "InputField_ValidateIdaaaa.OrdersView.inputOrderDateFromValue"
				+ DateUtil.toString(Utils.getStartDate(Constants.RETAILER_METRO),"yyyy-M-d")
				+ "ClientActionsubmitAsyncurEventNameValidateForm_RequestId...formAsynctrueFocusInfo@{\"sFocussedId\": \"ls-datepicker\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams3.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueFrom));
		HttpEntity orderFormEntity3 = new UrlEncodedFormEntity(orderformParams3, "UTF-8");
		HttpPost orderPost3 = new HttpPost(url2);
		orderPost3.addHeader("Accept-Language", "zh-CN");
		orderPost3
				.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

		orderPost3.setEntity(orderFormEntity3);
		CloseableHttpResponse orderRes3 = httpClient.execute(orderPost3);
		String responseStr3 = EntityUtils.toString(orderRes3.getEntity());
		orderRes3.close();
		
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		
		//EndDate
		List<NameValuePair> orderformParams4 = new ArrayList<NameValuePair>();
		orderformParams4.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams4.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams4.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams4.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueueTo = "InputField_ValidateIdaaaa.OrdersView.inputOrderDateToValue"
				+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_METRO),"yyyy-M-d")
				+ "ClientActionsubmitAsyncurEventNameValidateForm_RequestId...formAsynctrueFocusInfo@{\"sFocussedId\": \"ls-datepicker\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams4.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueTo));
		HttpEntity orderFormEntity4 = new UrlEncodedFormEntity(orderformParams4, "UTF-8");
		HttpPost orderPost4 = new HttpPost(url2);
		orderPost4.addHeader("Accept-Language", "zh-CN");
		orderPost4.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		
		orderPost4.setEntity(orderFormEntity4);
		CloseableHttpResponse orderRes4 = httpClient.execute(orderPost4);
		String responseStr4 = EntityUtils.toString(orderRes4.getEntity());
		orderRes4.close();
		
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		
		// Search Max500rows  查询结果包含多条记录 
		List<NameValuePair> orderformParams5 = new ArrayList<NameValuePair>();
		orderformParams5.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams5.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams5.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams5.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		//String sapEventQueueSubmit = "ComboBox_SelectIdaaaa.OrdersView.DropDownByKeyMaxResultKey500ByEnterfalseurEventNameCOMBOBOXSELECTIONCHANGEButton_PressIdaaaa.OrdersView.ButtonSearchClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.OrdersView.ButtonSearch\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		String sapEventQueueSubmit = "ComboBox_SelectIdaaaa.OrdersView.DropDownByKeyStatusKeyRECEIVEDByEnterfalseurEventNameCOMBOBOXSELECTIONCHANGEComboBox_SelectIdaaaa.OrdersView.DropDownByKeyMaxResultKey500ByEnterfalseurEventNameCOMBOBOXSELECTIONCHANGEButton_PressIdaaaa.OrdersView.ButtonSearchClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.OrdersView.ButtonSearch\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams5.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueueSubmit));
		HttpEntity orderFormEntity5 = new UrlEncodedFormEntity(orderformParams5, "UTF-8");
		HttpPost orderPost5 = new HttpPost(url2);
		orderPost5.setEntity(orderFormEntity5);
		CloseableHttpResponse orderRes5 = httpClient.execute(orderPost5);
		String responseStr5 = EntityUtils.toString(orderRes5.getEntity());
		orderRes5.close();
		if (responseStr5.contains("查询结果包含多条记录")) {
			log.info(user+"查询结果超过500条，请减小日期区间!");
			
		} else if (responseStr5.contains("没有查询到符合条件的数据")) {
			log.info(user+"没有查询到符合条件的订单单数据!");
			return;
		}
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));
		//Select ALL
		List<NameValuePair> orderformParams6 = new ArrayList<NameValuePair>();
		orderformParams6.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams6.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams6.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams6.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String selectAll = "Button_PressIdaaaa.OrdersView.ToolBarButtonChoiceClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.OrdersView.ToolBarButtonChoice\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams6.add(new BasicNameValuePair("SAPEVENTQUEUE", selectAll));
		HttpEntity orderFormEntity6 = new UrlEncodedFormEntity(orderformParams6, "UTF-8");
		HttpPost orderPost6 = new HttpPost(url2);
		orderPost6.setEntity(orderFormEntity6);
		CloseableHttpResponse orderRes6 = httpClient.execute(orderPost6);
		orderRes6.close();
		/*
		//显示未交货订单明细
		getUnDispatchOrder(httpClient,user,sap_ext_sid,sap_wd_cltwndid,sap_wd_norefresh,sap_wd_secure_id);
		// Back
		List<NameValuePair> orderformParams8 = new ArrayList<NameValuePair>();
		orderformParams8.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams8.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams8.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams8.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String back = "Button_PressIdaaaa.OrderLinesView.ButtonBackClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.OrderLinesView.ButtonBack\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams8.add(new BasicNameValuePair("SAPEVENTQUEUE", back));
		HttpEntity orderFormEntity8 = new UrlEncodedFormEntity(orderformParams8, "UTF-8");
		HttpPost orderPost8 = new HttpPost(url2);
		orderPost8.addHeader("Accept-Language", "zh-CN");
		orderPost8.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		orderPost8.setEntity(orderFormEntity8);
		CloseableHttpResponse orderRes8 = httpClient.execute(orderPost8);
		String backRes = EntityUtils.toString(orderRes8.getEntity()); 
		orderRes8.close();		
		*/
		//显示已交货订单明细
		getDispatchOrder(httpClient,user,sap_ext_sid,sap_wd_cltwndid,sap_wd_norefresh,sap_wd_secure_id);
		
	}
	private void getUnDispatchOrder(CloseableHttpClient httpClient, User user,String sap_ext_sid, String sap_wd_cltwndid, String sap_wd_norefresh,
			String sap_wd_secure_id) throws Exception {
		//显示未交货订单明细
		log.info(user+"下载未交货订单明细...");
		List<NameValuePair> orderformParams7 = new ArrayList<NameValuePair>();
		orderformParams7.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams7.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams7.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams7.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String diplayAll = "Button_PressIdaaaa.OrdersView.ButtonShowOrderLineClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.OrdersView.ButtonShowOrderLine\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams7.add(new BasicNameValuePair("SAPEVENTQUEUE", diplayAll));
		HttpEntity orderFormEntity7 = new UrlEncodedFormEntity(orderformParams7, "UTF-8");
		String url = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost orderPost7 = new HttpPost(url);
		orderPost7.addHeader("Accept-Language", "zh-CN");
		orderPost7.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		orderPost7.setEntity(orderFormEntity7);
		CloseableHttpResponse orderRes7 = httpClient.execute(orderPost7);
		String orderDetailRes = EntityUtils.toString(orderRes7.getEntity()); 
		orderRes7.close();
		if (orderDetailRes.contains("数据下载成功")) {
			orderDetailRes = orderDetailRes.substring(orderDetailRes.indexOf("<![CDATA[") + 9, orderDetailRes.indexOf("]]></content-update>"));
			Document detailResult = Jsoup.parse(orderDetailRes);
			Element pageElement = detailResult.select("div[ct=SCB]").first();
			String pageStr = pageElement.attr("lsdata");
			pageStr = pageStr.substring(pageStr.indexOf("ROW")+8,pageStr.lastIndexOf("}"));
			int record = Integer.valueOf(pageStr);
			int page = record % 10 > 0 ? record / 10 + 1 : record / 10;
			List<OrderTO> orderList = new ArrayList<OrderTO>();
			getOrderDetail(orderDetailRes,1,record,orderList,user);
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));	
			for (int i=2; i<=page; i++) {
				int startRow = i*10-9;
				String detailStr = getOrderByPage(httpClient,sap_ext_sid,sap_wd_cltwndid,sap_wd_norefresh,sap_wd_secure_id,startRow);
				detailStr = detailStr.substring(detailStr.indexOf("<![CDATA[") + 9, detailStr.indexOf("]]></content-update>"));
				getOrderDetail(detailStr,startRow,record,orderList,user);
			}
			
			FileUtil.exportOrderInfoListToTXT(Constants.RETAILER_METRO, orderList);
			
			log.info(user + "下载未交货订单明细成功!");
		} else if ( orderDetailRes.contains("没有查询到符合条件的数据")) {
			log.info(user+"没有查询到符合条件的未交货订单明细!");
		} else {
			log.info(user+"下载未交货订单明细失败!");
		}
	}
	private void getDispatchOrder(CloseableHttpClient httpClient, User user,String sap_ext_sid, String sap_wd_cltwndid, String sap_wd_norefresh,
			String sap_wd_secure_id) throws Exception {
		//显示已交货订单明细
		log.info(user+"下载已交货订单明细...");
		List<NameValuePair> orderformParams7 = new ArrayList<NameValuePair>();
		orderformParams7.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams7.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams7.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams7.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String diplayAll = "Button_PressIdaaaa.OrdersView.ButtonShowGRLinesClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{\"sFocussedId\": \"aaaa.OrdersView.ButtonShowGRLines\"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
	                      //Button_PressIdaaaa.OrdersView.ButtonShowGRLinesClientActionsubmiturEventNameBUTTONCLICKForm_RequestId...formAsyncfalseFocusInfo@{"sFocussedId": "aaaa.OrdersView.ButtonShowGRLines"}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle
		orderformParams7.add(new BasicNameValuePair("SAPEVENTQUEUE", diplayAll));
		HttpEntity orderFormEntity7 = new UrlEncodedFormEntity(orderformParams7, "UTF-8");
		String url = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost orderPost7 = new HttpPost(url);
		orderPost7.addHeader("Accept-Language", "zh-CN");
		orderPost7.addHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
		orderPost7.setEntity(orderFormEntity7);
		CloseableHttpResponse orderRes7 = httpClient.execute(orderPost7);
		String orderDetailRes = EntityUtils.toString(orderRes7.getEntity()); 
		orderRes7.close();
		if (orderDetailRes.contains("数据下载成功")) {
			orderDetailRes = orderDetailRes.substring(orderDetailRes.indexOf("<![CDATA[") + 9, orderDetailRes.indexOf("]]></content-update>"));
			Document detailResult = Jsoup.parse(orderDetailRes);
			Element pageElement = detailResult.select("div[ct=SCB]").first();
			String pageStr = pageElement.attr("lsdata");
			pageStr = pageStr.substring(pageStr.indexOf("ROW")+8,pageStr.lastIndexOf("}"));
			int record = Integer.valueOf(pageStr);
			log.info(user+"已交货订单明细共有 "+record+" 行");
			int page = record % 10 > 0 ? record / 10 + 1 : record / 10;
			List<OrderTO> orderList = new ArrayList<OrderTO>();
			getOrderDetail(orderDetailRes,1,record,orderList,user);
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));	
			for (int i=2; i<=page; i++) {
				int startRow = i*10-9;
				String detailStr = getRecOrderByPage(httpClient,sap_ext_sid,sap_wd_cltwndid,sap_wd_norefresh,sap_wd_secure_id,startRow);
				detailStr = detailStr.substring(detailStr.indexOf("<![CDATA[") + 9, detailStr.indexOf("]]></content-update>"));
				getOrderDetail(detailStr,startRow,record,orderList,user);
			}
			
		    FileUtil.exportOrderInfoListToTXT(Constants.RETAILER_METRO, orderList);
			
			log.info(user + "下载已交货订单明细成功!");
		} else if ( orderDetailRes.contains("没有查询到符合条件的数据")) {
			log.info(user+"没有查询到符合条件的已交货订单明细!");
		} else {
			log.info(user+"下载已交货订单明细失败!");
		}

	}
	
	private String getRecOrderByPage(CloseableHttpClient httpClient, String sap_ext_sid, String sap_wd_cltwndid, String sap_wd_norefresh,
			String sap_wd_secure_id,int startRow) throws Exception {
		List<NameValuePair> orderformParams = new ArrayList<NameValuePair>();
		orderformParams.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		//String sapEventQueuePage = "SapTable_VerticalScrollIdaaaa.OrderLinesView.tableOrderLineFirstVisibleItemIndex"+startRow+"ActionDIRECTCellIdAccessTypeSCROLLBARSelectionFollowFocusfalseShiftfalseCtrlfalseAltfalseClientActionsubmiturEventNameVerticalScrollForm_RequestId...formAsyncfalseFocusInfo@{}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		String sapEventQueuePageRec = "SapTable_VerticalScrollIdaaaa.GoodsRecLinesView.tableGRLineFirstVisibleItemIndex"+startRow+"ActionDIRECTCellIdAccessTypeSCROLLBARSelectionFollowFocusfalseShiftfalseCtrlfalseAltfalseClientActionsubmiturEventNameVerticalScrollForm_RequestId...formAsyncfalseFocusInfo@{}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueuePageRec));
		HttpEntity orderFormEntity = new UrlEncodedFormEntity(orderformParams, "UTF-8");
		String url = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost orderPost = new HttpPost(url);
		orderPost.setEntity(orderFormEntity);
		CloseableHttpResponse orderRes = httpClient.execute(orderPost);
		String responseStr = EntityUtils.toString(orderRes.getEntity()); // 数据下载成功
		orderRes.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));	
		return responseStr;
	}	
	private String getOrderByPage(CloseableHttpClient httpClient, String sap_ext_sid, String sap_wd_cltwndid, String sap_wd_norefresh,
			String sap_wd_secure_id,int startRow) throws Exception {
		List<NameValuePair> orderformParams = new ArrayList<NameValuePair>();
		orderformParams.add(new BasicNameValuePair("sap-ext-sid", sap_ext_sid));
		orderformParams.add(new BasicNameValuePair("sap-wd-cltwndid", sap_wd_cltwndid));
		orderformParams.add(new BasicNameValuePair("sap-wd-norefresh", sap_wd_norefresh));
		orderformParams.add(new BasicNameValuePair("sap-wd-secure-id", sap_wd_secure_id));
		String sapEventQueuePage = "SapTable_VerticalScrollIdaaaa.OrderLinesView.tableOrderLineFirstVisibleItemIndex"+startRow+"ActionDIRECTCellIdAccessTypeSCROLLBARSelectionFollowFocusfalseShiftfalseCtrlfalseAltfalseClientActionsubmiturEventNameVerticalScrollForm_RequestId...formAsyncfalseFocusInfo@{}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		//String sapEventQueuePageRec = "SapTable_VerticalScrollIdaaaa.GoodsRecLinesView.tableGRLineFirstVisibleItemIndex"+startRow+"ActionDIRECTCellIdAccessTypeSCROLLBARSelectionFollowFocusfalseShiftfalseCtrlfalseAltfalseClientActionsubmiturEventNameVerticalScrollForm_RequestId...formAsyncfalseFocusInfo@{}HashDomChangedfalseIsDirtyfalseEnqueueCardinalitysingle";
		orderformParams.add(new BasicNameValuePair("SAPEVENTQUEUE", sapEventQueuePage));
		HttpEntity orderFormEntity = new UrlEncodedFormEntity(orderformParams, "UTF-8");
		String url = "https://portal.metro-link.com/webdynpro/resources/sap.com/pb/PageBuilder";
		HttpPost orderPost = new HttpPost(url);
		orderPost.setEntity(orderFormEntity);
		CloseableHttpResponse orderRes = httpClient.execute(orderPost);
		String responseStr = EntityUtils.toString(orderRes.getEntity()); // 数据下载成功
		orderRes.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_METRO));	
		return responseStr;
	}	
	
	private void getOrderDetail(String responseStr,int startRow,int record,List<OrderTO> orderList,User user) {
		log.info(user+"读取已交货订单明细 "+startRow+"-"+((startRow+9)>record?record:(startRow+9))+" 行");
		Document detailResult = Jsoup.parse(responseStr);
		for (int i=startRow; i<startRow+10 && i<=record;i++) {
			Elements dataElements = detailResult.select("tr[rr="+i+"]");
			Elements tds = dataElements.first().select("td");
			OrderTO orderTo = new OrderTO();
			orderTo.setStoreID(tds.get(2).select("span").first().text()); // 门店编号
			orderTo.setOrderNo(tds.get(3).select("span").first().text()); // 订单编号
			orderTo.setOrderDate(tds.get(4).select("span").first().text()); // 订货日期
			orderTo.setItemID(tds.get(8).select("span").first().text()); // 商品编号
			orderTo.setBarcode(tds.get(9).select("span").first().text()); // 条形码
			orderTo.setItemName(tds.get(10).select("span").first().text()); // 商品描述
			orderTo.setUnitPrice(tds.get(13).select("span").first().text());// 进货价格
			orderTo.setQuantity(tds.get(15).select("span").first().text()); // 订货数量
			orderTo.setTotalPrice(tds.get(19).select("span").first().text()); // 净金额
			orderList.add(orderTo);
		}

	}

}
