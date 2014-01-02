package com.rsi.mengniu;

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
			ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
			//DataPullTaskPool.initTaskPool("ALL");
			//DataPullTaskPool.initTaskPool("Carrefour");
			DataPullTaskPool.initTaskPool("Tesco");
			int threadNum = 2;
			ExecutorService exec = Executors.newFixedThreadPool(threadNum);
			for (int i=0; i<threadNum; i++) {
				exec.execute(new DataPullThread());
			}
			exec.shutdown();
		} catch (Exception e) {
			log.error(Utils.getTrace(e));
		}
		

	}

}
