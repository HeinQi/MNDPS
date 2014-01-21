package com.rsi.mengniu.retailer.rainbow.service;

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
import com.rsi.mengniu.retailer.module.RainbowReceivingTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.User;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class RainbowDataPullService implements RetailerDataPullService {
	private static Log log = LogFactory.getLog(RainbowDataPullService.class);

	public void dataPull(User user) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			String returnType = this.login(httpClient, user);
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
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
		List <ReceivingNoteTO> receivingList = new ArrayList<ReceivingNoteTO>();
		
		Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
		HttpGet httpGet = new HttpGet(
				"http://vd.rainbow.cn:8080/object/getAdvReportData.jsp"
						+ "?start=0"
						+ "&limit=20"
						+ "&rptId=3124"
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
		response.close();
		
		
		Document doc = Jsoup.parse(responseStr);
		
		String totalPageNumberStr = doc.select("a").last().attr("onclick");
		
		totalPageNumberStr = totalPageNumberStr.substring(totalPageNumberStr.indexOf("(")+1, totalPageNumberStr.indexOf(")"));
		int totalPageNumber = Integer.valueOf(totalPageNumberStr);
		for(int i = 1; i<= totalPageNumber;i++){
			Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
			
			HttpGet summaryPageGet = new HttpGet(
					"http://vd.rainbow.cn:8080/object/getAdvReportData.jsp"
							+ "?start="+(i-1)*20
							+ "&limit=20"
							+ "&rptId=3124"
							+ "&showSelectionModel=false"
							+ "&html=Y"
							+ "&sqlWhere=&where=%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3E=to_date('"
							+ DateUtil.toString(Utils.getStartDate(Constants.RETAILER_RAINBOW))
							+ "','yyyy-mm-dd')%20and%20%E6%94%B6%E8%B4%A7%E6%97%A5%E6%9C%9F%3C=to_date('"
							+ DateUtil.toString(Utils.getEndDate(Constants.RETAILER_RAINBOW))
							+ "','yyyy-mm-dd')");
			

			
			CloseableHttpResponse summaryPageResponse = httpClient.execute(summaryPageGet);
			HttpEntity summaryPageEntity = summaryPageResponse.getEntity();
			String summaryPageResponseStr = EntityUtils.toString(summaryPageEntity);
			summaryPageResponse.close();
			
			Document summaryPageDoc = Jsoup.parse(summaryPageResponseStr);
			
			//Receiving highlevel info
			Elements rowsElements = summaryPageDoc.select("#dataTable").first().select("tr:gt(0)");
			rowsElements.remove(rowsElements.size()-1);
			
			
//			<tr>
//			0	<td style="display: ">
//					<a href='javascript:void(0);'
//						onclick="doJump('/gysp/orderdetail.jsp?SHDH=REC-050140104000060','')">REC-050140104000060</a>
//				</td>
//			1	<td style="display: ">观澜天虹</td>
//			2	<td style="display: ">天虹商场股份有限公司</td>
//			3	<td style="display: ">天虹商场股份有限公司</td>
//			4	<td style="display: ">2014-01-04</td>
//			5	<td style="display: ">ORD-050140102000052</td>
//			6	<td style="display: ">2014-01-04</td>
//			7	<td style="display: ">4103.03</td>
//			8	<td style="display: ">N</td>
//			9	<td style="display: ">正常</td>
//			10	<td style="display: ">未读</td>
//			</tr>
			for(Element rowElement : rowsElements){
				Elements tdElements = rowElement.select("td");
				String receivingNo = tdElements.get(0).text();
				log.info(user + "收货单编号：" + receivingNo);

				Thread.sleep(Utils.getSleepTime(Constants.RETAILER_RAINBOW));
				HttpGet receivingDetailHTTPGet = new HttpGet(
						"http://vd.rainbow.cn:8080//gysp/orderdetail.jsp?SHDH="
								+ receivingNo);
				CloseableHttpResponse receivingResponse =  httpClient.execute(receivingDetailHTTPGet);
				HttpEntity receivingEntity = receivingResponse.getEntity();
				String receivingResponseStr = EntityUtils.toString(receivingEntity);
				//log.info(receivingResponseStr);
				receivingResponse.close();
				
				Document receivingDetailDoc = Jsoup.parse(receivingResponseStr);
				
				//Get receiving main info
				Elements receivingMainInfoElements = receivingDetailDoc.select("#table154").get(2).select("tr").get(1).select("td");
				String storeName = receivingMainInfoElements.get(0).text();
				String receivingDate = receivingMainInfoElements.get(1).text();
				String orderNo = receivingMainInfoElements.get(2).text();
				

				storeName = storeName.substring(storeName.indexOf(":")+1);
				receivingDate = receivingDate.substring(receivingDate.indexOf(":")+1);
				orderNo = orderNo.substring(orderNo.indexOf(":")+1);
				
				String storeID = Utils.getRaimbowStoreIDByName(storeName);
				
				Elements receivingDetailTableElements = receivingDetailDoc.select("#table152").first().select("tr:gt(0)");
				receivingDetailTableElements.remove(receivingDetailTableElements.size()-1);
				
				//Get receiving detail item
				for(Element receivingDetailRowElement : receivingDetailTableElements){
					Elements receivingDetailTDElements = receivingDetailRowElement.select("td");
					 String itemID=receivingDetailTDElements.get(0).text();
					 String itemName=receivingDetailTDElements.get(2).text();
					 String barcode=receivingDetailTDElements.get(1).text();
					 String orderQuantity=receivingDetailTDElements.get(6).text();
					 String orderTotalPrice=receivingDetailTDElements.get(8).text();
					 String receivingQuantity=receivingDetailTDElements.get(9).text();
					 String receivingTotalPrice=receivingDetailTDElements.get(12).text();
					 String unitPrice=receivingDetailTDElements.get(7).text();
					 RainbowReceivingTO rainbowReceivingTO = new RainbowReceivingTO(orderNo, storeID, storeName, receivingDate, itemID, barcode, itemName, orderQuantity, orderTotalPrice, receivingQuantity, receivingTotalPrice, unitPrice);
					 receivingList.add(rainbowReceivingTO);
				}
				
			}
			
		}
		FileUtil.exportReceivingInfoToTXTForRainbow(Constants.RETAILER_RAINBOW, user.getUserId(), receivingList);
		

	}}
