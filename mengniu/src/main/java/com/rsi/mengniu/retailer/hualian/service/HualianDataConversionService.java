package com.rsi.mengniu.retailer.hualian.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.carrefour.service.CarrefourDataConversionService;
import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class HualianDataConversionService extends RetailerDataConversionService {
	private Log log = LogFactory.getLog(HualianDataConversionService.class);

	@Override
	protected String getRetailerID() {

		return Constants.RETAILER_HUALIAN;
	}


	@Override
	protected Log getLog() {
		return log;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			String retailerID, Date startDate, Date endDate, File receivingFile)
			throws BaseException {
		return null;
	}
	
	public void convertSalesData(String retailerID, Date startDate,
			Date endDate) throws BaseException {
		
		
		String sourceFilePath = Utils.getProperty(retailerID
				+ Constants.SALES_INBOUND_PATH);
		getLog().info("复制销售单文件: 源文件目录"+sourceFilePath);
		String backupPath = Utils.getProperty(retailerID
				+ Constants.SALES_PROCESSED_PATH);
		getLog().info("复制销售单文件: 目标文件目录"+backupPath);
		
		FileUtil.copyFiles(FileUtil.getAllFile(sourceFilePath), sourceFilePath,
				backupPath);
		
		String destPath = Utils.getProperty(retailerID
				+ Constants.OUTPUT_SALES_PATH);
		getLog().info("复制销售单文件: 目标文件目录"+destPath);
		FileUtil.moveFiles(FileUtil.getAllFile(sourceFilePath), sourceFilePath,
				destPath);
	}
	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(
			String retailerID, Date startDate, Date endDate, File salesFile)
			throws BaseException {
		return null;
	}

	@Override
	protected Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}

}
