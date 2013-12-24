package com.rsi.mengniu.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class AppContextHelper implements ApplicationContextAware {
	private static ApplicationContext appContext;
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		appContext = arg0;
		
	}
	public static Object getBean(String beanName) {
		return appContext.getBean(beanName);
	}

}
