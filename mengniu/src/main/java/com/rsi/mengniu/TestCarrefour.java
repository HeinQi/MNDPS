package com.rsi.mengniu;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.rsi.mengniu.util.OCR;

public class TestCarrefour {

	public static void main(String[] args) {
		String responseStr;
		// http://supplierweb.carrefour.com.cn/
		CloseableHttpClient httpClient = HttpClients.createDefault();
		// validateCode
		// http://supplierweb.carrefour.com.cn/includes/image.jsp
		HttpGet httpGet = new HttpGet("http://supplierweb.carrefour.com.cn/includes/image.jsp");
		try {
			String imgName = String.valueOf(java.lang.System.currentTimeMillis());
			FileOutputStream fos = new FileOutputStream("/Users/haibin/Documents/workspace/mengniu/temp/"+imgName+".jpg");
			CloseableHttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			entity.writeTo(fos);
			response.close();
			fos.close();

			String recognizeStr = OCR.getInstance().recognizeText("/Users/haibin/Documents/workspace/mengniu/temp/"+imgName+".jpg",
					"/Users/haibin/Documents/workspace/mengniu/temp/"+imgName);
			recognizeStr = recognizeStr.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
			//login /login.do?action=doLogin
			List<NameValuePair> formParams = new ArrayList<NameValuePair>();
			formParams.add(new BasicNameValuePair("login","ZFJ9"));
			formParams.add(new BasicNameValuePair("password","951763")); //错误的密码
			formParams.add(new BasicNameValuePair("validate",recognizeStr));
			HttpEntity loginEntity = new UrlEncodedFormEntity(formParams,"UTF-8");
			HttpPost httppost = new HttpPost("http://supplierweb.carrefour.com.cn/login.do?action=doLogin");
			httppost.setEntity(loginEntity);
			CloseableHttpResponse loginResponse = httpClient.execute(httppost);
			responseStr = EntityUtils.toString(loginResponse.getEntity());
			if (responseStr.contains("验证码失效")) {
				System.out.println("验证码失效");
				return;
			} else {
				//System.out.println(responseStr);
			}
			loginResponse.close();
			
			
			//goMenu('inyr.do?action=query','14','预估进退查询')
			
			//$('inyrForm').action="inyr.do?action=export";
			//供应商预估进退查询/Supplier Inyr Inquiry
			
			List<NameValuePair> receiveformParams = new ArrayList<NameValuePair>();
			receiveformParams.add(new BasicNameValuePair("unitid","ALL"));
			receiveformParams.add(new BasicNameValuePair("butype","byjv")); 
			receiveformParams.add(new BasicNameValuePair("systemdate","2013/12/22")); //yyyy/mm/dd
			HttpEntity receiveFormEntity = new UrlEncodedFormEntity(receiveformParams,"UTF-8");	
			HttpPost receivePost = new HttpPost("http://supplierweb.carrefour.com.cn/inyr.do?action=export");
			receivePost.setEntity(receiveFormEntity);
			CloseableHttpResponse receiveRes = httpClient.execute(receivePost);
			responseStr = EntityUtils.toString(receiveRes.getEntity());
			System.out.println(responseStr);
			receiveRes.close();
			if (responseStr.contains("Excel文档生成成功")) {	
				//Download Excel file
				responseStr = responseStr.substring(responseStr.indexOf("javascript:downloads('")+22);
				String inyrFileName = responseStr.substring(0,responseStr.indexOf("'"));
				FileOutputStream receiveFos = new FileOutputStream("/Users/haibin/Documents/workspace/mengniu/temp/"+inyrFileName);
				
				List<NameValuePair> downloadformParams = new ArrayList<NameValuePair>();
				downloadformParams.add(new BasicNameValuePair("filename",inyrFileName));
				downloadformParams.add(new BasicNameValuePair("filenamedownload","excelpath"));
				HttpEntity downloadFormEntity = new UrlEncodedFormEntity(downloadformParams,"UTF-8");	
				HttpPost downloadPost = new HttpPost("http://supplierweb.carrefour.com.cn/download.jsp");
				downloadPost.setEntity(downloadFormEntity);
				CloseableHttpResponse downloadRes = httpClient.execute(downloadPost);	
				downloadRes.getEntity().writeTo(receiveFos);
				downloadRes.close();
				receiveFos.close();
				
			} else {
				System.out.println("Export Error!");
			}
					
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
