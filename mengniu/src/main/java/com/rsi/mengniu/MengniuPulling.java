package com.rsi.mengniu;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.rsi.mengniu.util.Utils;

public class MengniuPulling {
	private static Log log = LogFactory.getLog(MengniuPulling.class);
	public static void main(String[] args) {
		try {
			new ClassPathXmlApplicationContext("applicationContext.xml");
			//DataPullTaskPool.initTaskPool("ALL");
			//DataPullTaskPool.initTaskPool("carrefour");
			//DataPullTaskPool.initTaskPool("tesco");
			//DataPullTaskPool.initTaskPool("yonghui");
			DataPullTaskPool.initTaskPool("metro");
			int threadNum = 2;
			final CountDownLatch mDoneSignal = new CountDownLatch(2); 

			ExecutorService exec = Executors.newFixedThreadPool(threadNum);
			for (int i=0; i<threadNum; i++) {
				exec.execute(new DataPullThread(mDoneSignal));
			}
			exec.shutdown();
			mDoneSignal.await(); // Wait all thread done
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
		log.info("All Data Pull Thread End!");
		

	}

}
