package com.rsi.mengniu.retailer.tesco.service;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
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
import com.rsi.mengniu.retailer.module.TescoOrderNotifyTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

//https://tesco.chinab2bi.com/security/login.hlt
public class TescoDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(TescoDataPullService.class);

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			login(httpClient, user);
			getReceiveExcel(httpClient, user);

			getOrder(httpClient, user);

			httpClient.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String login(CloseableHttpClient httpClient, User user) throws Exception {
		log.info(user + "开始登录...");
		List<NameValuePair> formParams = new ArrayList<NameValuePair>();
		formParams.add(new BasicNameValuePair("j_username", user.getUserId()));
		formParams.add(new BasicNameValuePair("j_password", user.getPassword())); // 错误的密码
		HttpEntity loginEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
		HttpPost httppost = new HttpPost("https://tesco.chinab2bi.com/j_spring_security_check");
		httppost.setEntity(loginEntity);
		CloseableHttpResponse loginResponse = httpClient.execute(httppost);
		String loginStr = EntityUtils.toString(loginResponse.getEntity());
		if (loginStr.contains("密码错误")) {
			log.info(user + "错误的密码,退出!");
		}
		loginResponse.close();
		// forward
		HttpGet httpGet = new HttpGet(loginResponse.getFirstHeader("location").getValue());
		CloseableHttpResponse response = httpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		if (!EntityUtils.toString(entity).contains("mainMenu.hlt")) {
			log.info(user + "系统出错,退出!");
			return "Error";
		}
		response.close();
		log.info(user + "登录成功!");
		return "Success";
	}

	public void getReceiveExcel(CloseableHttpClient httpClient, User user) throws Exception {
		// https://tesco.chinab2bi.com/tesco/sellGrnQry/init.hlt
		// get vendorTaxRegistration
		log.info(user + "开始下载收货单...");
		HttpGet httpGet = new HttpGet("https://tesco.chinab2bi.com/tesco/sellGrnQry/init.hlt");
		CloseableHttpResponse formResponse = httpClient.execute(httpGet);
		HttpEntity taxEntity = formResponse.getEntity();
		Document doc = Jsoup.parse(EntityUtils.toString(taxEntity));
		Element taxElement = doc.select("#vendorTaxRegistration").first();
		formResponse.close();
		String vendorTaxRegistration = taxElement.attr("value");

		// query.hlt

		List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
		receiveformParams.add(new BasicNameValuePair("vendorTaxRegistration", vendorTaxRegistration));// 税号
		receiveformParams.add(new BasicNameValuePair("transactionType", "01"));// 进货
		receiveformParams.add(new BasicNameValuePair("grnModel.transactionDateStart", DateUtil.toString(Utils.getStartDate(), "yyyy-MM-dd")));// 交易日期
		receiveformParams.add(new BasicNameValuePair("grnModel.transactionDateEnd", DateUtil.toString(Utils.getEndDate(), "yyyy-MM-dd"))); // 交易日期
		HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams, "UTF-8");
		HttpPost receivePost = new HttpPost("https://tesco.chinab2bi.com/tesco/sellGrnQry/exportDetail.hlt");
		receivePost.setEntity(receiveFormEntity);
		CloseableHttpResponse receiveRes = httpClient.execute(receivePost); // filename=20140108220149.zip
		String fileNm = receiveRes.getFirstHeader("Content-Disposition").getValue();
		fileNm = fileNm.substring(fileNm.indexOf("filename=") + 9);
		String receiveFilePath = Utils.getProperty(user.getRetailer()+Constants.RECEIVING_INBOUND_PATH);
		FileUtil.createFolder(receiveFilePath);
		FileOutputStream receiveFos = new FileOutputStream(receiveFilePath + fileNm);
		receiveRes.getEntity().writeTo(receiveFos);
		receiveFos.close();
		receiveRes.close();
		FileUtil.unzip(receiveFilePath + fileNm, receiveFilePath, "");
		log.info(user + "Tesco收货单Excel下载成功!");
	}

	public void getOrder(CloseableHttpClient httpClient, User user) throws Exception {
		// https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQryInit.hlt
		// load search from
		log.info(user + "跳转到订单查询页面...");
		HttpGet httpGet = new HttpGet("https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQryInit.hlt");
		CloseableHttpResponse formResponse = httpClient.execute(httpGet);
		HttpEntity formEntity = formResponse.getEntity();
		Document formDoc = Jsoup.parse(EntityUtils.toString(formEntity));
		formResponse.close();
		Element parentVendorElement = formDoc.select("#parentVendor").first();
		String parentVendor = parentVendorElement.attr("value");
		List<TescoOrderNotifyTO> notifyList = new ArrayList<TescoOrderNotifyTO>();

		getNotifyList(httpClient, parentVendor, notifyList,user);
		// get order detail
		int count =0;
		for (TescoOrderNotifyTO notify : notifyList) {
			String url = "https://tesco.chinab2bi.com/tesco/sp/purOrder/pdfView.hlt?seed&fileName=" + notify.getFileName() + "&createDate="
					+ notify.getCreateDate() + "&poId=" + notify.getPoId() + "&parentVendor=" + notify.getParentVendor();
			url = url.replaceAll(" ", "%20");
			HttpGet httpOrderGet = new HttpGet(url);
			CloseableHttpResponse orderDetailResponse = httpClient.execute(httpOrderGet);
			HttpEntity orderEntity = orderDetailResponse.getEntity();
			String orderStr = EntityUtils.toString(orderEntity);
			BufferedReader br = new BufferedReader(new StringReader(orderStr));
			orderDetailResponse.close();
			count++;
			log.info(user+"成功下载订单通知明细["+count+"]");
			readOrder(br,user);
			log.info(user+"订单数据下载成功!");
		}

		/*
		 * 
		 * page.togglestatus:nullform status:sell parentVendor:303688 vendor:11152615 orderDateStart:2014-01-01 orderDateEnd:2014-01-05 readFlag:
		 * downFlag: page.pageSize:10 page.pageNo:1 page.totalPages:1 page.jumpNumber:1
		 */

		/*
		 * openPDF('4195444', '303688', 'MERGE_11152615@supplier.cn.tesco.com_20140105105533.txt', '20140105') function openPDF(poId, parentVendor,
		 * fileName, createDate){ var url = "/tesco/sp/purOrder/pdfView.hlt?seed"; var params = "&fileName="+fileName; params +=
		 * "&createDate="+createDate; params += "&poId=" + poId; params += "&parentVendor=" + parentVendor;
		 * 
		 * url += params; openEasyWin("winId","订单明细信息",url,"1000","600",true); }
		 */

	}

	private void getNotifyList(CloseableHttpClient httpClient, String parentVendor, List<TescoOrderNotifyTO> notifyList,User user) throws Exception {
		// https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQry.hlt
		log.info(user + "查询订单通知...");
		int pageNo = 1;
		int totalPages = 1;
		do {
			List<NameValuePair> searchformParams = new ArrayList<NameValuePair>();
			searchformParams.add(new BasicNameValuePair("orderDateStart", DateUtil.toString(Utils.getStartDate(), "yyyy-MM-dd")));// 通知日期
			searchformParams.add(new BasicNameValuePair("orderDateEnd", DateUtil.toString(Utils.getEndDate(), "yyyy-MM-dd"))); // 通知日期
			searchformParams.add(new BasicNameValuePair("parentVendor", parentVendor));// parentVendor
			searchformParams.add(new BasicNameValuePair("page.pageSize", "50"));// pageSize
			searchformParams.add(new BasicNameValuePair("page.pageNo", String.valueOf(pageNo))); // pageSize
			searchformParams.add(new BasicNameValuePair("page.totalPages", String.valueOf(totalPages))); // totalPages
			searchformParams.add(new BasicNameValuePair("status", "sell"));// pageSize

			HttpEntity searchFormEntity = new UrlEncodedFormEntity(searchformParams, "UTF-8");
			HttpPost searchPost = new HttpPost("https://tesco.chinab2bi.com/tesco/sp/purOrder/sellPubOrderQry.hlt");
			searchPost.setEntity(searchFormEntity);
			CloseableHttpResponse searchRes = httpClient.execute(searchPost);
			String searchResStr = EntityUtils.toString(searchRes.getEntity());
			searchRes.close();
			Document doc = Jsoup.parse(searchResStr);
			Elements aElements = doc.select("a[onclick^=openPDF]");
			Element eTotalPages = doc.select("#totalPages").first();
			totalPages = Integer.parseInt(eTotalPages.attr("value")); // totalPages

			for (Element aElement : aElements) {
				TescoOrderNotifyTO notify = new TescoOrderNotifyTO();
				String openPdfStr = aElement.attr("onclick");
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf("'") + 1);
				notify.setPoId(openPdfStr.substring(0, openPdfStr.indexOf("'")));
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf(", '") + 3);
				notify.setParentVendor(openPdfStr.substring(0, openPdfStr.indexOf("'")));
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf(", '") + 3);
				notify.setFileName(openPdfStr.substring(0, openPdfStr.indexOf("'")));
				openPdfStr = openPdfStr.substring(openPdfStr.indexOf(", '") + 3);
				notify.setCreateDate(openPdfStr.substring(0, 10).replaceAll("-", ""));
				notifyList.add(notify);
			}
			pageNo++;
		} while (pageNo <= totalPages);
		log.info(user+"查询到从"+DateUtil.toString(Utils.getStartDate(), "yyyy-MM-dd")+"到"+DateUtil.toString(Utils.getEndDate(), "yyyy-MM-dd")+",共有"+notifyList.size()+"条订单通知");				
		
		
	}

	private void readOrder(BufferedReader br,User user) throws Exception {
		log.info(user+"读取订单通知明细");
		String line = null;
		int count =0;
		while ((line = br.readLine()) != null) {
			if (line.contains("TESCO 乐  购  商  品  订  单")) {
				line = br.readLine(); // 店别: 大连友好店 页1 页
				line = line.substring(line.indexOf("店别:  ") + 5);
				String storeNm = line.substring(0, line.indexOf(" "));
				br.readLine();
				line = br.readLine();// 订单号码 17040620 促销期数 紧急订单
				String orderNo = line.substring(line.indexOf("订单号码") + 6, line.indexOf("促销期数")).trim();
				line = br.readLine(); // 订单日期 2014/01/05 交货日期 2014/01/06 订单类型 -DSD PO-
				if (!line.contains("DSD PO")) {
					continue;
				}

				String orderDate = line.substring(line.indexOf("订单日期") + 6, line.indexOf("交货日期")).trim();
				for (int i = 0; i < 10; i++) {
					br.readLine();
				}
				List<OrderTO> orderItems = new ArrayList<OrderTO>();
				readOrderItem(br, orderItems, storeNm, orderNo, orderDate);
				FileUtil.exportOrderInfoToTXT("tesco", orderNo, orderItems);
				log.info(user+"成功读取订单,订单号:"+orderNo);
				count++;
			}
		}
		log.info(user+"此订单通知明细共有"+count+"订单");
	}

	private void readOrderItem(BufferedReader br, List<OrderTO> orderItems, String storeNm, String orderNo, String orderDate) throws IOException {
		// 103933911/ SP_24_蒙牛冠益乳酸牛奶 瓶 箱 24 4.74 113.85 341.54 3 3 72 1 1 N
		String line = null;
		while ((line = br.readLine()).length() > 0) {
			OrderTO orderTo = new OrderTO();
			orderTo.setStoreName(storeNm);
			orderTo.setOrderNo(orderNo);
			orderTo.setOrderDate(orderDate);
			String itemId = line.substring(1, 10);
			line = line.substring(17);
			String itemName = line.substring(0, line.indexOf(" "));
			line = line.substring(line.indexOf(" ")).trim(); // 规格
			line = line.substring(line.indexOf(" ")).trim();// 订购单位
			line = line.substring(line.indexOf(" ")).trim();// 箱入数
			line = line.substring(line.indexOf(" ")).trim();// 单件成本
			line = line.substring(line.indexOf(" ")).trim();// 成本
			String unitPrice = line.substring(0, line.indexOf(" ")).trim();
			line = line.substring(line.indexOf(" ")).trim();// 总成本
			String totalPrice = line.substring(0, line.indexOf(" "));
			line = line.substring(line.indexOf(" ")).trim();// 订购数量
			String quantity = line.substring(0, line.indexOf(" "));
			line = br.readLine();
			line = line.substring(17);
			String itemNm2 = line.substring(0, line.indexOf(" "));
			line = line.substring(line.indexOf(" ")).trim(); // 国际条码
			String barcode = line.substring(0, line.indexOf(" "));
			orderTo.setItemCode(itemId);
			orderTo.setItemName(itemName + itemNm2);
			orderTo.setUnitPrice(unitPrice);
			orderTo.setTotalPrice(totalPrice);
			orderTo.setQuantity(quantity);
			orderTo.setBarcode(barcode);
			orderItems.add(orderTo);
		}

	}
}
