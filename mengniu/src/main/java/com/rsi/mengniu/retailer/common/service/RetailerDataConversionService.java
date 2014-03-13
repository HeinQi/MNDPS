package com.rsi.mengniu.retailer.common.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.AccountLogTO;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.AccountLogUtil;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public abstract class RetailerDataConversionService {

	protected abstract String getRetailerID();

	protected abstract Log getLog();

	protected abstract Log getSummaryLog();

	protected static Log errorLog = LogFactory.getLog(Constants.SYS_ERROR);

	protected abstract Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(String retailerID, Date startDate,
			Date endDate, File receivingFile) throws BaseException;

	protected abstract Map<String, OrderTO> getOrderInfoFromFile(String retailerID, File orderFile)
			throws BaseException;

	/**
	 * Get Sales Info
	 * 
	 * @param retailerID
	 * @param startDate
	 * @param endDate
	 * @param salesFile
	 * @return
	 * @throws BaseException
	 */
	protected abstract Map<String, List<SalesTO>> getSalesInfoFromFile(String retailerID, Date startDate, Date endDate,
			File salesFile) throws BaseException;

	/**
	 * Process Data by time range
	 * 
	 * Root method of data conversion
	 * 
	 * @param retailerID
	 * @param startDate
	 * @param endDate
	 * @throws BaseException
	 */
	public void retailerDataProcessing() {
		String retailerID = this.getRetailerID();
		Date startDate = Utils.getStartDate(retailerID);
		Date endDate = Utils.getEndDate(retailerID);

		// Delete Exception File
		deleteExceptionFile(retailerID, startDate, endDate);

		try {

			getLog().info("开始处理数据");

			getLog().info("开始处理收货单和订单数据");

			getSummaryLog().info("订单合并情况：");

			processOrderData(retailerID, startDate, endDate);

			getLog().info("收货单和订单数据处理结束");
		} catch (Exception e) {
			errorLog.error("零售商：" + retailerID + "订单处理失败。");
			errorLog.error(e);

		}

		try {
			getLog().info("零售商：" + retailerID + " 开始处理销售单");

			getSummaryLog().info("销售单合并情况：");
			processSalesData(retailerID, startDate, endDate);

			getLog().info("零售商：" + retailerID + " 销售单处理结束");
		} catch (Exception e) {
			errorLog.error("零售商：" + retailerID + "销售单处理失败。");
			errorLog.error(e);

		}
		getLog().info("数据处理结束");

		getSummaryLog().info("零售商数据处理结束");
		getSummaryLog().info(Constants.SUMMARY_SEPERATOR_LINE + "\n");

	}

	private void deleteExceptionFile(String retailerID, Date startDate, Date endDate) {
		getLog().info("开始清理Exception文件");
		List<Date> dates = DateUtil.getDateArrayByRange(startDate, endDate);
		for (Date processDate : dates) {
			String receivingExceptionFolderPath = Utils.getProperty(retailerID + Constants.RECEIVING_EXCEPTION_PATH);
			FileUtil.createFolder(receivingExceptionFolderPath);
			String fileName = "Receiving_" + retailerID + "_"
					+ DateUtil.toStringYYYYMMDD(processDate) + ".txt";
			String receivingExceptionFilePath = receivingExceptionFolderPath + fileName;

			File receivingFile = new File(receivingExceptionFilePath);

			if(receivingFile.exists()){
				receivingFile.delete();
			}
			
			getLog().info("收货单异常文件： " + fileName + "清理完毕");
			
			
			String outputOrderExceptionFolderPath = Utils.getProperty(retailerID + Constants.OUTPUT_ORDER_EXCEPTION_PATH);

			String outputOrderExceptionFileName = outputOrderExceptionFolderPath + retailerID + "_order_"
					+ DateUtil.toStringYYYYMMDD(processDate) + ".txt";
			String outputOrderExceptionFileFullPath = outputOrderExceptionFolderPath + outputOrderExceptionFileName;

			File outputOrderExceptionFile = new File(outputOrderExceptionFileFullPath);
			if(outputOrderExceptionFile.exists()){
				outputOrderExceptionFile.delete();
			}
			getLog().info("订单异常文件： " + fileName + "清理完毕");
			
		}

	}

	/**
	 * Convert Order Data
	 * 
	 * @param retailerID
	 * @param startDate
	 * @param endDate
	 * @throws BaseException
	 */
	public void processOrderData(String retailerID, Date startDate, Date endDate) throws BaseException {
		getLog().info("读取收货单数据:" + retailerID);
		// Get Receiving Note
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = getReceivingInfo(retailerID, startDate, endDate);

		if (receivingNoteMap == null || receivingNoteMap.size() == 0) {
			getLog().info("零售商：" + retailerID + " 无收货单数据。");
			return;
		}
		getLog().info("读取收货单数据结束:" + retailerID);

		// Get Order No. List
		getLog().info("读取订单数据:" + retailerID);
		Map<String, OrderTO> orderTOMap = getOrderInfo(retailerID, receivingNoteMap.keySet());

		if (orderTOMap == null || orderTOMap.size() == 0) {
			getLog().info("零售商：" + retailerID + " 无订单单数据。");
			return;
		}
		getLog().info("读取订单数据结束:" + retailerID);

		// go through the receiving map, generate map: key receiving date
		Map<String, List<ReceivingNoteTO>> receivingByDateMap = generateReceivingMapByDate(receivingNoteMap);

		Object[] receivingKeyList = receivingByDateMap.keySet().toArray();
		Arrays.sort(receivingKeyList);

		// Iterator Receiving Map by Date
		for (int i = 0; i < receivingKeyList.length; i++) {
			String processDateStr = (String) receivingKeyList[i];

			getSummaryLog().info("订单收货日期：" + processDateStr);
			List<ReceivingNoteTO> receivingList = receivingByDateMap.get(processDateStr);
			if (!(receivingList.size() == 0)) {
				getLog().info("开始整合. 零售商: " + retailerID + " 日期:" + processDateStr + "订单数量:" + receivingList.size());
				convertOrderData(retailerID, processDateStr, receivingList, orderTOMap);
				getLog().info(
						"整合结束. 零售商: " + retailerID + " 日期:" + processDateStr + "订单数量:" + receivingList.size() + "\n");
			}
		}

		String sourceFilePath = Utils.getProperty(retailerID + Constants.RECEIVING_INBOUND_PATH);
		getLog().info(sourceFilePath);
		String destPath = Utils.getProperty(retailerID + Constants.RECEIVING_PROCESSED_PATH);
		getLog().info(destPath);
		// Copy processed receiving note from inbound to processed folder
		FileUtil.moveFiles(FileUtil.getAllFile(sourceFilePath), sourceFilePath, destPath);
	}

	/**
	 * Get receiving data map
	 * 
	 * @param retailerID
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws BaseException
	 */
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfo(String retailerID, Date startDate, Date endDate) {
		Map receivingNoteMap = new HashMap();

		File receivingInboundFolder = new File(Utils.getProperty(retailerID + Constants.RECEIVING_INBOUND_PATH));

		File[] receivingList = receivingInboundFolder.listFiles();
		if (receivingList != null) {
			for (int i = 0; i < receivingList.length; i++) {

				File receivingFile = receivingList[i];
				getLog().info("收货单文件名: " + receivingFile.getName());

				// Get Receiving Info
				try {
					Map receivingNoteSingleMap = getReceivingInfoFromFile(retailerID, startDate, endDate, receivingFile);

					Utils.putSubMapToMainMap(receivingNoteMap, receivingNoteSingleMap);
				} catch (Exception e) {
					errorLog.error("读取文件失败。请检查文件是否正确。");
					errorLog.error("文件名：" + receivingFile.getName());
				}

			}
		}

		return receivingNoteMap;

	}

	/**
	 * Get Order Info
	 * 
	 * @param retailerID
	 * @param orderNoSet
	 * @return
	 * @throws BaseException
	 */
	protected Map<String, OrderTO> getOrderInfo(String retailerID, Set<String> orderNoSet) {
		File orderInboundFolder = new File(Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH));

		File[] orderList = orderInboundFolder.listFiles();

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
		for (int i = 0; i < orderList.length; i++) {

			File orderFile = orderList[i];
			try {
				orderMap.putAll(getOrderInfoFromFile(retailerID, orderFile));
				getLog().info("订单文件名: " + orderFile.getName());
			} catch (Exception e) {
				errorLog.error("读取文件失败。请检查文件是否正确。");
				errorLog.error("文件名：" + orderFile.getName());
			}

		}

		return orderMap;
	}

	/**
	 * Convert Receiving Map from key: order No.
	 * to key: processing date
	 * 
	 * @param receivingNoteMap
	 * @return
	 * @throws BaseException
	 */
	protected Map<String, List<ReceivingNoteTO>> generateReceivingMapByDate(
			Map<String, List<ReceivingNoteTO>> receivingNoteMap) throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingByDateMap = new HashMap<String, List<ReceivingNoteTO>>();
		for (List<ReceivingNoteTO> receivingNoteList : receivingNoteMap.values()) {

			List<ReceivingNoteTO> receivingNoteByDateList = null;
			for (ReceivingNoteTO receivingNoteTO : receivingNoteList) {
				String processDate = receivingNoteTO.getReceivingDate();
				if (receivingByDateMap.containsKey(processDate)) {
					receivingNoteByDateList = receivingByDateMap.get(processDate);
				} else {
					receivingNoteByDateList = new ArrayList<ReceivingNoteTO>();

				}
				receivingNoteByDateList.add(receivingNoteTO);
				receivingByDateMap.put(processDate, receivingNoteByDateList);

			}
		}

		for (Entry<String, List<ReceivingNoteTO>> entry : receivingByDateMap.entrySet()) {
			String key = entry.getKey();
			List valueList = entry.getValue();

			getLog().info("收货单日期：" + key + " 收货单数量:" + valueList.size());
		}

		return receivingByDateMap;
	}

	/**
	 * Process Data of defined date
	 * 
	 * @param processDate
	 * @throws BaseException
	 */
	private void convertOrderData(String retailerID, String processDate, List<ReceivingNoteTO> receivingList,
			Map<String, OrderTO> orderTOMap) throws BaseException {

		// Convert Receiving info list to map
		// Key: Store ID + Item Code
		getLog().info("转换订单信息。 零售商: " + retailerID + " 日期:" + processDate + " 转换前的收货单数量:" + receivingList.size());
		Map<String, ReceivingNoteTO> receivingNoteByStoreMap = generateReceivingMapForComparison(receivingList);
		getLog().info(
				"转换订单信息。 零售商代码: " + retailerID + " 日期:" + processDate + " 合并后的收货单数量:" + receivingNoteByStoreMap.size());

		// Get matched receiving note by iterate order txt file
		// Merge to one record
		// Write to merged txt file
		getLog().info("开始整合订单和收货单信息. 零售商: " + retailerID + " 日期:" + processDate);
		StringBuffer stringBuffer = mergeOrderAndReceiving(retailerID, DateUtil.toDate(processDate),
				receivingNoteByStoreMap, orderTOMap);
		getLog().info("整合订单和收货单信息结束. 零售商: " + retailerID + " 日期:" + processDate);
		if (stringBuffer.length() != 0) {
			BufferedWriter writer = initOrderOutputFile(retailerID, processDate);

			try {
				writer.write(stringBuffer.toString());

			} catch (IOException e) {
				FileUtil.closeFileWriter(writer);
				throw new BaseException(e);
			}

			// Close the opened file
			FileUtil.closeFileWriter(writer);
		}

	}

	protected BufferedWriter initOrderOutputFile(String retailerID, String processDate) throws BaseException {
		BufferedWriter writer;
		String mergeFolderPath = Utils.getProperty(retailerID + Constants.OUTPUT_ORDER_PATH);

		String mergeFilePath = mergeFolderPath + retailerID + "_order_"
				+ DateUtil.toStringYYYYMMDD(DateUtil.toDate(processDate)) + ".txt";
		getLog().info("初始化整合文本文件. 文件名: " + mergeFilePath);
		File mergeFolder = new File(mergeFolderPath);
		if (!mergeFolder.exists()) {
			mergeFolder.mkdirs();
		}

		File mergeFile = new File(mergeFilePath);

		try {
			FileOutputStream fileOutput = new FileOutputStream(mergeFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
		} catch (IOException e) {
			throw new BaseException(e);
		}

		writerOrderOutputFileHeader(writer);
		return writer;
	}

	protected void writerOrderOutputFileHeader(BufferedWriter writer) throws BaseException {
		String mergedHeader = Utils.getProperty(Constants.OUTPUT_ORDER_HEADER);
		try {
			writer.write(mergedHeader);
			writer.newLine();
		} catch (IOException e) {

			throw new BaseException(e);
		}
	}

	/**
	 * Parse the receiving list of one day to map The key is:
	 * Order Number + Store Name + Item ID
	 * 
	 * @param receivingNoteList
	 * @return
	 * @throws BaseException
	 */
	public Map<String, ReceivingNoteTO> generateReceivingMapForComparison(List<ReceivingNoteTO> receivingNoteList)
			throws BaseException {
		Map<String, ReceivingNoteTO> receivingNoteByStoreMap = new HashMap<String, ReceivingNoteTO>();

		for (int i = 0; i < receivingNoteList.size(); i++) {
			ReceivingNoteTO receivingNoteByStoreTO = receivingNoteList.get(i);
			String storeName = receivingNoteByStoreTO.getStoreName();
			String key = receivingNoteByStoreTO.getOrderNo() + storeName + receivingNoteByStoreTO.getItemID();
			if (receivingNoteByStoreMap.containsKey(key)) {
				ReceivingNoteTO existTO = receivingNoteByStoreMap.get(key);
				existTO.setQuantity(String.valueOf(Double.parseDouble(receivingNoteByStoreTO.getQuantity().replaceAll(
						",", ""))
						+ Double.parseDouble(existTO.getQuantity().replaceAll(",", ""))));
				existTO.setTotalPrice(String.valueOf(Double.parseDouble(receivingNoteByStoreTO.getTotalPrice()
						.replaceAll(",", "")) + Double.parseDouble(existTO.getTotalPrice().replaceAll(",", ""))));
				getLog().info(
						"整合收货单: 原始数量: " + receivingNoteByStoreTO.getQuantity() + " 原始总价: "
								+ receivingNoteByStoreTO.getTotalPrice());

				getLog().info("整合收货单: 整合后数量: " + existTO.getQuantity() + " 整合后总价: " + existTO.getTotalPrice());
			} else {
				receivingNoteByStoreMap.put(key, receivingNoteByStoreTO);
			}

		}
		return receivingNoteByStoreMap;
	}

	/**
	 * Merge Order Info and Receiving Info in the DATE to one txt record
	 * 
	 * @param writer
	 * @param receivingNoteByStoreMap
	 * @param orderTOMap
	 * @throws BaseException
	 */
	private StringBuffer mergeOrderAndReceiving(String retailerID, Date receivingDate,
			Map<String, ReceivingNoteTO> receivingNoteByStoreMap, Map<String, OrderTO> orderTOMap) throws BaseException {
		StringBuffer stringBuffer = new StringBuffer();
		Object[] receivingKeyList = receivingNoteByStoreMap.keySet().toArray();
		Arrays.sort(receivingKeyList);

		
		
		Map<String,Set<String>> mergedOrderMap = new HashMap<String, Set<String>>();
		
		List<ReceivingNoteTO> failedReceivingList = new ArrayList<ReceivingNoteTO>();
		int failedCount = 0;
		int successCount = 0;
		for (int i = 0; i < receivingKeyList.length; i++) {
			String combineKey = (String) receivingKeyList[i];

			ReceivingNoteTO receivingNoteTO = receivingNoteByStoreMap.get(combineKey);

			// check if the item No. and store is matching.
			// if matched then merge order info and receiving note
			// info to txt
			if (orderTOMap.containsKey(combineKey)) {
				OrderTO orderTO = orderTOMap.get(combineKey);
				/*
				 * Order_NO(订单号)， Store_No(收货单明细中的门店号)，
				 * Receiving_Date（收获明细中的收获日期） item_code(),barcode(条形码)，
				 * Item_Name(产品名称)， Oder_Unit（订货明细中的总计数量），
				 * order_amount（订货明细中的总金额）， receive_unit(收获明细中的收获量)，
				 * receive_amount（收获明细中的收获金额）， Unit_Price(单价)，
				 * 下载过程中已经下过的订单信息不能再下，  所有账号下载下来的文件按照日期合并
				 * ，每个date一个文件,文件名为Carrefour_order_YYYYMMDD,Unicode txt
				 */
				String orderNo = orderTO.getOrderNo();
				
				String mergedLine = orderNo
						+ "\t"
						+ receivingNoteTO.getStoreID()
						+ "\t"
						+ receivingNoteTO.getStoreName()
						+ "\t"
						+ receivingNoteTO.getReceivingDate()
						+ "\t"
						+ orderTO.getItemID()
						+ "\t"
						+ orderTO.getBarcode()
						+ "\t"
						+ orderTO.getItemName()
						+ "\t"
						+ orderTO.getQuantity()
						+ "\t"
						+ orderTO.getTotalPrice()
						+ "\t"
						+ receivingNoteTO.getQuantity()
						+ "\t"
						+ receivingNoteTO.getTotalPrice()
						+ "\t"
						+ ((orderTO.getUnitPrice().equals("")) ? receivingNoteTO.getUnitPrice() : orderTO
								.getUnitPrice());
//						+ "\t"
//						+ receivingNoteTO.getUserID();
				stringBuffer.append(mergedLine + "\r\n");

				//生成合并成功的数据，为Account Log准备数据
				Set<String> orderNoSet = new HashSet<String>();
				String mergedOrderKey = retailerID+"--"+receivingNoteTO.getUserID() +"--"+ receivingNoteTO.getReceivingDate();
				if(mergedOrderMap.containsKey(mergedOrderKey)){
					orderNoSet = mergedOrderMap.get(mergedOrderKey);
				}
				orderNoSet.add(orderNo);
				mergedOrderMap.put(mergedOrderKey, orderNoSet);
				
				successCount++;
			} else {
				getLog().info("警告! 查不到收货单对应的订单信息. 收货单信息为: " + receivingNoteTO.toString());
				failedReceivingList.add(receivingNoteTO);
				failedCount++;
			}
		}
		AccountLogUtil.updateProcessedOrderInfo(mergedOrderMap);
		
		exportFailedReceiving(retailerID, receivingDate, failedReceivingList, failedCount);

		getSummaryLog().info("订单合并成功数量：" + successCount);
		getLog().info("订单合并成功数量：" + successCount);
		getLog().info("订单合并失败数量：" + failedCount);
		getSummaryLog().info("订单合并失败数量：" + failedCount);

		return stringBuffer;
	}

	/**
	 * Export the failed receiving data to file
	 * 
	 * @param retailerID
	 * @param receivingDate
	 * @param failedReceivingList
	 * @param failedCount
	 * @throws BaseException
	 */
	public void exportFailedReceiving(String retailerID, Date receivingDate, List<ReceivingNoteTO> failedReceivingList,
			int failedCount) throws BaseException {
		if (failedCount != 0) {
			Utils.exportFailedReceivingToTXT(retailerID, receivingDate, failedReceivingList);
		}
	}

	/**
	 * Convert the sales info by user per running date to by sales date
	 * 
	 * @param retailerID
	 * @param startDate
	 * @param endDate
	 * @throws BaseException
	 */
	public void processSalesData(String retailerID, Date startDate, Date endDate) throws BaseException {

		// getSalesData
		Map<String, List<SalesTO>> salesMap = this.getSalesData(retailerID, startDate, endDate);
		if (salesMap == null || salesMap.size() == 0) {
			getLog().info("零售商：" + retailerID + " 无销售单数据。");
			return;
		}
		// Write Data to output folder
		for (Entry<String, List<SalesTO>> entry : salesMap.entrySet()) {
			String processDateStr = entry.getKey();
			List<SalesTO> salesList = entry.getValue();
			convertSalesData(retailerID, processDateStr, salesList);
			
			//统计销售合并数据
			AccountLogUtil.recordSalesProcessedAmount(retailerID,processDateStr,salesList);
		}
		// Archive
		String sourceFilePath = Utils.getProperty(retailerID + Constants.SALES_INBOUND_PATH);
		getLog().info(sourceFilePath);
		String destPath = Utils.getProperty(retailerID + Constants.SALES_PROCESSED_PATH);

		getLog().info(destPath);
		FileUtil.moveFiles(FileUtil.getAllFile(sourceFilePath), sourceFilePath, destPath);
	}

	private Map<String, List<SalesTO>> getSalesData(String retailerID, Date startDate, Date endDate) {
		Map salesMap = new HashMap();

		File salesInboundFolder = new File(Utils.getProperty(retailerID + Constants.SALES_INBOUND_PATH));

		File[] salesList = salesInboundFolder.listFiles();
		if (salesList != null) {
			for (int i = 0; i < salesList.length; i++) {

				File salesFile = salesList[i];
				try {
					getLog().info("收货单文件名: " + salesFile.getName());
					Map salesSingleMap = getSalesInfoFromFile(retailerID, startDate, endDate, salesFile);

					// receivingNoteMap.putAll(salesSingleMap);
					Utils.putSubMapToMainMap(salesMap, salesSingleMap);
				} catch (Exception e) {
					errorLog.error("读取文件失败。请检查文件是否正确。");
					errorLog.error("文件名：" + salesFile.getName());
				}

			}
		}

		return salesMap;

	}

	private void convertSalesData(String retailerID, String processDateStr, List<SalesTO> salesList)
			throws BaseException {

		getSummaryLog().info("销售单合并日期：" + processDateStr);
		BufferedWriter writer = this.initSalesOutputFile(retailerID, processDateStr);

		for (SalesTO salesTO : salesList) {
			String outputLine = salesTO.toString();
			try {
				writer.write(outputLine);

				writer.newLine();
			} catch (IOException e) {
				FileUtil.closeFileWriter(writer);
				throw new BaseException(e);
			}
		}

		FileUtil.closeFileWriter(writer);

		getSummaryLog().info("销售单合并数量：" + salesList.size());
	}

	protected BufferedWriter initSalesOutputFile(String retailerID, String processDate) throws BaseException {
		BufferedWriter writer;
		String mergeFolderPath = Utils.getProperty(retailerID + Constants.OUTPUT_SALES_PATH);

		String mergeFilePath = mergeFolderPath + retailerID + "_sales_"
				+ DateUtil.toStringYYYYMMDD(DateUtil.toDate(processDate)) + ".txt";
		getLog().info("初始化整合文本文件. 文件名: " + mergeFilePath);
		File mergeFolder = new File(mergeFolderPath);
		if (!mergeFolder.exists()) {
			boolean mk = mergeFolder.mkdirs();
		}

		File mergeFile = new File(mergeFilePath);

		try {
			FileOutputStream fileOutput = new FileOutputStream(mergeFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
		} catch (IOException e) {
			throw new BaseException(e);
		}

		writerSalesOutputFileHeader(writer);
		return writer;
	}

	private void writerSalesOutputFileHeader(BufferedWriter writer) throws BaseException {
		String mergedHeader = Utils.getProperty(Constants.SALES_HEADER);
		try {
			writer.write(mergedHeader);
			writer.newLine();
		} catch (IOException e) {

			throw new BaseException(e);
		}
	}

	public void processOrderData2(String retailerID, Date startDate, Date endDate) throws BaseException {
		// Get Receiving Data
		getLog().info("读取收货单数据:" + retailerID);
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = getReceivingInfo2(retailerID, startDate, endDate);
		if (receivingNoteMap == null || receivingNoteMap.size() == 0) {
			getLog().info("零售商：" + retailerID + " 无收货单数据。");
			return;
		}
		getLog().info("读取收货单数据结束:" + retailerID);

		// Get Order Data

		getLog().info("读取订单数据:" + retailerID);
		Map<String, OrderTO> orderTOMap = getOrderInfo2(retailerID, startDate, endDate);

		if (orderTOMap == null || orderTOMap.size() == 0) {
			getLog().info("零售商：" + retailerID + " 无订单单数据。");
			return;
		}
		getLog().info("读取订单数据结束:" + retailerID);

		// Consolidate Receiving

	}

	public Map<String, List<ReceivingNoteTO>> getReceivingInfo2(String retailerID, Date startDate, Date endDate)
			throws BaseException {
		Map receivingNoteMap = new HashMap();

		File receivingInboundFolder = new File(Utils.getProperty(retailerID + Constants.RECEIVING_INBOUND_PATH));

		File[] receivingList = receivingInboundFolder.listFiles();
		if (receivingList != null) {
			for (int i = 0; i < receivingList.length; i++) {

				File receivingFile = receivingList[i];
				getLog().info("收货单文件名: " + receivingFile.getName());

				// Get Receiving Info
				Map receivingNoteSingleMap = getReceivingInfoFromFile2(retailerID, startDate, endDate, receivingFile);

				Utils.putSubMapToMainMap(receivingNoteMap, receivingNoteSingleMap);

			}
		}

		return receivingNoteMap;

	}

	public Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile2(String retailerID, Date startDate,
			Date endDate, File receivingFile) throws BaseException {

		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		if (receivingFile.exists()) {
			BufferedReader reader = null;
			try {
				// Open the file
				FileInputStream fileInput = new FileInputStream(receivingFile);
				InputStreamReader inputStrReader = new InputStreamReader(fileInput, "UTF-8");
				reader = new BufferedReader(inputStrReader);
				reader.readLine();
				// Read line by line
				String receivingLine = null;
				while ((receivingLine = reader.readLine()) != null) {
					ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO(receivingLine);
					String receivingDate = receivingNoteTO.getReceivingDate();

					List<ReceivingNoteTO> receivingNoteTOList = null;
					if (receivingNoteMap.containsKey(receivingDate)) {
						receivingNoteTOList = receivingNoteMap.get(receivingDate);
					} else {
						receivingNoteTOList = new ArrayList<ReceivingNoteTO>();
						// Test the Hashmap
						receivingNoteMap.put(receivingDate, receivingNoteTOList);
					}

					getLog().debug("收货单详细条目: " + receivingNoteTO.toString());
					receivingNoteTOList.add(receivingNoteTO);

				}

			} catch (FileNotFoundException e) {
				getLog().error(e);
				throw new BaseException(e);
			} catch (IOException e) {

				getLog().error(e);
				throw new BaseException(e);

			} finally {

				FileUtil.closeFileReader(reader);
			}

			getLog().info("收货单: " + receivingFile.getName() + " 包含的详单数量为:" + receivingNoteMap.size());

		}
		return receivingNoteMap;
	}

	public Map<String, OrderTO> getOrderInfo2(String retailerID, Date startDate, Date endDate) throws BaseException {
		Map<String, OrderTO> orderMap = new HashMap();

		File orderFolder = new File(Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH));

		File[] orderList = orderFolder.listFiles();
		if (orderList != null) {
			for (int i = 0; i < orderList.length; i++) {

				File orderFile = orderList[i];

				getLog().info("订单文件名: " + orderFile.getName());

				// Get Receiving Info
				Map<String, OrderTO> orderSingleMap = getOrderInfoFromFile2(orderFile, startDate, endDate);

				orderMap.putAll(orderSingleMap);

			}
		}

		return orderMap;
	}

	private Map<String, OrderTO> getOrderInfoFromFile2(File orderFile, Date startDate, Date endDate)
			throws BaseException {
		BufferedReader reader = null;
		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
		try {
			// Open the file
			FileInputStream fileInput = new FileInputStream(orderFile);
			InputStreamReader inputStrReader = new InputStreamReader(fileInput, "UTF-8");
			reader = new BufferedReader(inputStrReader);
			reader.readLine();
			// Read line by line
			String orderLine = null;

			int countNumber = 0;
			while ((orderLine = reader.readLine()) != null) {
				OrderTO orderTO = new OrderTO(orderLine);
				String key = orderTO.getOrderNo() + orderTO.getStoreID() + orderTO.getItemID();

				orderMap.put(key, orderTO);
				countNumber++;
			}
			//
			getLog().info("订单文件: " + orderFile + " 包含的详单数量为:" + countNumber);

		} catch (FileNotFoundException e) {
			getLog().error(e);
			throw new BaseException(e);
		} catch (IOException e) {

			getLog().error(e);
			throw new BaseException(e);

		} finally {

			FileUtil.closeFileReader(reader);
		}
		return orderMap;

	}
}
