package com.rsi.mengniu;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.util.AccountLogUtil;
import com.rsi.mengniu.util.Utils;

public class MengniuPulling {
	private static Log log = LogFactory.getLog(MengniuPulling.class);

	public static void main(String[] args) {
		String retailerId = args[0];
		try {
			ApplicationContext appContext = new ClassPathXmlApplicationContext("applicationContext.xml");
			DataPullTaskPool.initTaskPool(retailerId);
			int threadNum = Integer.parseInt(Utils.getProperty("datapull.thread.amount"));
			int retryNum = Integer.parseInt(Utils.getProperty("datapull.retry.amount"));
			for (int num = 0; num <= retryNum; num++) {
				final CountDownLatch mDoneSignal = new CountDownLatch(threadNum);

				ExecutorService exec = Executors.newFixedThreadPool(threadNum);
				for (int i = 0; i < threadNum; i++) {
					exec.execute(new DataPullThread(mDoneSignal));
				}
				exec.shutdown();
				mDoneSignal.await(); // Wait all thread done
				
				if (DataPullTaskPool.hasRetryTask() && (num+1)<=retryNum) {
					DataPullTaskPool.processFaileTask();
					log.info("有失败的用户,系统将对失败的用户进行第"+(num+1)+"次重试下载!");
				} else {
					break;
				}
			}
			if ("ALL".equalsIgnoreCase(retailerId)) {
				RetailerDataConversionService carrefourConversion = (RetailerDataConversionService) appContext.getBean("carrefour.data.conversion");
				carrefourConversion.retailerDataProcessing();

				RetailerDataConversionService tescoConversion = (RetailerDataConversionService) appContext.getBean("tesco.data.conversion");
				tescoConversion.retailerDataProcessing();

				RetailerDataConversionService yonhuiConversion = (RetailerDataConversionService) appContext.getBean("yonghui.data.conversion");
				yonhuiConversion.retailerDataProcessing();

				RetailerDataConversionService renrenleConversion = (RetailerDataConversionService) appContext.getBean("renrenle.data.conversion");
				renrenleConversion.retailerDataProcessing();

				RetailerDataConversionService rainbowConversion = (RetailerDataConversionService) appContext.getBean("rainbow.data.conversion");
				rainbowConversion.retailerDataProcessing();

				RetailerDataConversionService hualianConversion = (RetailerDataConversionService) appContext.getBean("hualian.data.conversion");
				hualianConversion.retailerDataProcessing();

				RetailerDataConversionService metroConversion = (RetailerDataConversionService) appContext.getBean("metro.data.conversion");
				metroConversion.retailerDataProcessing();

			} else if (appContext.containsBean(retailerId + ".data.conversion")) {
				RetailerDataConversionService dataConversion = (RetailerDataConversionService) appContext.getBean(retailerId + ".data.conversion");
				dataConversion.retailerDataProcessing();
			}
AccountLogUtil.writeAccountLogToFile();
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
		log.info("All Data Pull Thread End!");

	}

}
