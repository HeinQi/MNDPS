package com.rsi.mengniu;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MengniuPulling {

	public static void main(String[] args) {
		int threadNum = 1;
		ExecutorService exec = Executors.newFixedThreadPool(threadNum);
		for (int i=0; i<threadNum; i++) {
			
			
		}
		exec.shutdown();

	}

}
