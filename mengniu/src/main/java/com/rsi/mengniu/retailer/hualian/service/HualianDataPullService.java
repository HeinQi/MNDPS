package com.rsi.mengniu.retailer.hualian.service;

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
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

//https://tesco.chinab2bi.com/security/login.hlt
public class HualianDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(HualianDataPullService.class);

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			String loginResult = login(httpClient, user);
			// Invalid Password and others
			if (!"Success".equals(loginResult)) {
				return;
			}

			getSales(httpClient, user);

			//getOrder(httpClient, user);

			httpClient.close();
		} catch (Exception e) {
			log.error(user + Utils.getTrace(e));
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("UsernameGet", user.getUserId()));
		formParams.add(new BasicNameValuePair("PasswordGet", user.getPassword())); // 错误的密码
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("http://zunyi.beijing-hualian.com/checksuppllogin.asp");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);

		String reponseLogin = new String(EntityUtils.toString(loginResponse.getEntity()).getBytes("ISO_8859_1"),"GBK");
		loginResponse.close();
		if (reponseLogin.contains("您的供应商编号或密码有误")) {
			log.info(user + "错误的密码,退出!");
			return "Error";
		}
		// forward
		HttpGet httpGet = new HttpGet("http://zunyi.beijing-hualian.com/suppl_select.asp");
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		String loginStr = new String(EntityUtils.toString(entity).getBytes("ISO_8859_1"),"GBK");
		if (!loginStr.contains("查询系统")) {
			log.info(user + "系统出错,退出!");
			return "Error";
		}
		response.close();
		log.info(user + "登录成功!");
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_HUALIAN));
		return "Success";
	}

	public void getSales(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始下载销售数据...");
		HttpGet salesHttpGet = new HttpGet("http://zunyi.beijing-hualian.com/suppl_select.asp?action=sale");
		CloseableHttpResponse formResponse = httpClient.execute(salesHttpGet);
		HttpEntity formEntity = formResponse.getEntity();
		Document doc = Jsoup.parse(new String(EntityUtils.toString(formEntity).getBytes("ISO_8859_1"),"GBK"));
		Element storeElement = doc.select("#store").first();
		formResponse.close();
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_HUALIAN));
		List<SalesTO> salesList = new ArrayList<SalesTO>();
		Elements sElements = storeElement.select("option[value]");
		for (Element store:sElements) {
			String storeId = store.attr("value");
			getSalesByStore(httpClient,user,storeId,salesList);
		}
		FileUtil.exportSalesInfoToTXT(Constants.RETAILER_HUALIAN, user.getUserId(), salesList);
		log.info(user + "销售数据下载成功");
	}
	
	///suppl_select.asp?action=salesel
	private void getSalesByStore(CloseableHttpClient httpClient, User user,String storeId,List<SalesTO> salesList) throws Exception {
		String startDate = DateUtil.toString(Utils.getStartDate(Constants.RETAILER_HUALIAN),"yyyyMMdd");
		String endDate = DateUtil.toString(Utils.getEndDate(Constants.RETAILER_HUALIAN),"yyyyMMdd");
		
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("store", storeId));
		formParams.add(new BasicNameValuePair("begindate", startDate));
		formParams.add(new BasicNameValuePair("enddate", endDate));
		log.info(user + "下载店号为["+storeId+"],日期区间为"+startDate+" - "+endDate+"的销售数据");
		
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("http://zunyi.beijing-hualian.com/suppl_select.asp?action=salesel");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		Document doc = Jsoup.parse(new String(EntityUtils.toString(loginResponse.getEntity()).getBytes("ISO_8859_1"),"GBK"));
		Element dataTable = doc.select("table").first();
		Elements rows = dataTable.select("tr:gt(2)");
		for (int i=0; i<rows.size()-1; i++) {
			Elements tds = rows.select("td");
			SalesTO sales = new SalesTO();
			sales.setStoreID(tds.get(0).text());
			sales.setItemID(tds.get(1).text());
			sales.setItemName(tds.get(2).text());
			sales.setSalesQuantity(tds.get(3).text());
			sales.setSalesAmount(tds.get(4).text());
		}
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_HUALIAN));
	}

}
