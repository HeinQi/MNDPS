package com.rsi.mengniu.retailer.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import nl.fountain.xelem.excel.ss.XLWorkbook;
import nl.fountain.xelem.lex.ExcelReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.xml.sax.SAXException;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public abstract class RetailerDataConversionService {

	private Log log = LogFactory.getLog(RetailerDataConversionService.class);

	// private void main(String[] args) throws BaseException {
	// log.info("开始");
	//
	// FileUtil.testFileAmount("C:/root/yonghui/merged/");
	// retailerDataProcessing(Constants.RETAILER_CARREFOUR,
	// DateUtil.toDate("2013-12-01"), DateUtil.toDate("2014-01-03"));
	//
	// retailerDataProcessing(Constants.RETAILER_TESCO,
	// DateUtil.toDate("2013-12-30"), DateUtil.toDate("2014-01-05"));
	//
	// retailerDataProcessing(Constants.RETAILER_YONGHUI,
	// DateUtil.toDate("2013-12-25"), DateUtil.toDate("2014-01-11"));
	// log.info("结束");
	// }

	protected abstract String getRetailerID();



	protected abstract Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			String retailerID, Date startDate, Date endDate, File receivingFile)
			throws BaseException;

	/**
	 * Get Order Info
	 * 
	 * @param retailerID
	 * @param orderNoSet
	 * @return
	 * @throws BaseException
	 */
	protected abstract Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException ;
	/**
	 * Process Data by time range
	 * 
	 * @param retailerID
	 * @param startDate
	 * @param endDate
	 * @throws BaseException
	 */
	public void retailerDataProcessing() throws BaseException {
		String retailerID = this.getRetailerID();
		Date startDate = Utils.getStartDate();
		Date endDate = Utils.getEndDate();

		log.info("开始处理数据");

		log.info("读取收货单数据:" + retailerID);
		// Get Receiving Note
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = getReceivingInfo(
				retailerID, startDate, endDate);

		log.info("读取收货单数据结束:" + retailerID);

		// Get Order No. List

		Map<String, OrderTO> orderTOMap = getOrderInfo(retailerID,
				receivingNoteMap.keySet());

		// go through the receiving map, generate map: key receiving date

		Map<String, List<ReceivingNoteTO>> receivingByDateMap = generateReceivingMapByDate(receivingNoteMap);

		Object[] receivingKeyList = receivingByDateMap.keySet().toArray();
		Arrays.sort(receivingKeyList);

		// Iterator Receiving Map by Date
		for (int i = 0; i < receivingKeyList.length; i++) {
			String processDateStr = (String) receivingKeyList[i];
			List<ReceivingNoteTO> receivingList = receivingByDateMap
					.get(processDateStr);

			log.info("开始整合. 零售商: " + retailerID + " 日期:" + processDateStr
					+ "订单数量:" + receivingList.size());
			retailerDataProcessing(retailerID, processDateStr, receivingList,
					orderTOMap);

			log.info("整合结束. 零售商: " + retailerID + " 日期:" + processDateStr
					+ "订单数量:" + receivingList.size() + "\n");
		}

		String sourceFilePath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_INBOUND_PATH);
		String destPath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_PROCESSED_PATH);
		;
		// Copy processed receiving note from inbound to processed folder
		// TODO
		// FileUtil.copyFiles(FileUtil.getAllFile(sourceFilePath),
		// sourceFilePath,
		// destPath);

		// create file by date
		// populate the new receiving map: key:storename+itemid
		// match with order map
		/*
		 * List<Date> dateList = null; try { dateList =
		 * DateUtil.getDateArrayByRange(startDate, endDate); for (int i = 0; i <
		 * dateList.size(); i++) { Date processDate = dateList.get(i);
		 * 
		 * retailerDataProcessing(retailerID, processDate); }
		 * 
		 * } catch (ParseException e) { throw new BaseException(e); }
		 */

		log.info("数据处理结束");

	}

	private Map<String, List<ReceivingNoteTO>> generateReceivingMapByDate(
			Map<String, List<ReceivingNoteTO>> receivingNoteMap) {
		Map<String, List<ReceivingNoteTO>> receivingByDateMap = new HashMap<String, List<ReceivingNoteTO>>();
		for (List<ReceivingNoteTO> receivingNoteList : receivingNoteMap
				.values()) {

			List<ReceivingNoteTO> receivingNoteByDateList = null;
			for (ReceivingNoteTO receivingNoteTO : receivingNoteList) {
				String processDate = receivingNoteTO.getReceivingDate();
				if (receivingByDateMap.containsKey(processDate)) {
					receivingNoteByDateList = receivingByDateMap
							.get(processDate);
				} else {
					receivingNoteByDateList = new ArrayList<ReceivingNoteTO>();

				}
				receivingNoteByDateList.add(receivingNoteTO);
				receivingByDateMap.put(processDate, receivingNoteByDateList);

			}
		}

		for (Entry<String, List<ReceivingNoteTO>> entry : receivingByDateMap
				.entrySet()) {
			String key = entry.getKey();
			List valueList = entry.getValue();

			log.info("收货单日期：" + key + " 收货单数量:" + valueList.size());
		}

		return receivingByDateMap;
	}

	



	/**
	 * Get receiving data
	 * 
	 * @param retailerID
	 * @param processDate
	 * @return
	 * @throws BaseException
	 */
	private Map<String, List<ReceivingNoteTO>> getReceivingInfo(
			String retailerID, Date startDate, Date endDate)
			throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		File receivingInboundFolder = new File(
				Utils.getProperty(Constants.RETAILER_TESCO
						+ Constants.RECEIVING_INBOUND_PATH));

		File[] receivingList = receivingInboundFolder.listFiles();

		for (int i = 0; i < receivingList.length; i++) {

			File receivingFile = receivingList[i];
			log.info("收货单文件名: " + receivingFile.getName());
			Map<String, List<ReceivingNoteTO>> receivingNoteSingleMap = getReceivingInfoFromFile(
					retailerID, startDate, endDate, receivingFile);

			receivingNoteMap.putAll(receivingNoteSingleMap);
		}

		return receivingNoteMap;

	}



	/**
	 * Process Data of defined date
	 * 
	 * @param processDate
	 * @throws BaseException
	 */
	private void retailerDataProcessing(String retailerID, String processDate,
			List<ReceivingNoteTO> receivingList, Map<String, OrderTO> orderTOMap)
			throws BaseException {

		BufferedWriter writer = initMergeFile(retailerID, processDate);

		// Convert Receiving info list to map
		// Key: Store ID + Item Code

		log.info("转换订单信息。 零售商: " + retailerID + " 日期:" + processDate
				+ " 转换前的收货单数量:" + receivingList.size());
		Map<String, ReceivingNoteTO> receivingNoteByStoreMap = parseReceivingListToMap(receivingList);

		log.info("转换订单信息。 零售商代码: " + retailerID + " 日期:" + processDate
				+ " 合并后的收货单数量:" + receivingList.size());

		// Get matched receiving note by iterate order txt file
		// Merge to one record
		// Write to merged txt file

		log.info("开始整合订单和收货单信息. 零售商: " + retailerID + " 日期:" + processDate);
		mergeOrderAndReceiving(writer, receivingNoteByStoreMap, orderTOMap);

		log.info("整合订单和收货单信息结束. 零售商: " + retailerID + " 日期:" + processDate);

		// Close the opened file
		FileUtil.closeFileWriter(writer);

	}

	private BufferedWriter initMergeFile(String retailerID, String processDate)
			throws BaseException {
		BufferedWriter writer;
		String mergeFolderPath = Utils.getProperty(retailerID
				+ Constants.OUTPUT_PATH);

		String mergeFilePath = mergeFolderPath + retailerID + "_order_"
				+ DateUtil.toStringYYYYMMDD(DateUtil.toDate(processDate))
				+ ".txt";
		log.info("初始化整合文本文件. 文件名: " + mergeFilePath);
		File mergeFolder = new File(mergeFolderPath);
		if (!mergeFolder.exists()) {
			mergeFolder.mkdir();
		}

		File mergeFile = new File(mergeFilePath);

		try {
			writer = new BufferedWriter(new FileWriter(mergeFile));
		} catch (IOException e) {
			throw new BaseException(e);
		}

		writerMergeFileHeader(writer);
		return writer;
	}

	private void writerMergeFileHeader(BufferedWriter writer)
			throws BaseException {
		String mergedHeader = Utils.getProperty(Constants.OUTPUT_HEADER);
		try {
			writer.write(mergedHeader);
			writer.newLine();
		} catch (IOException e) {

			throw new BaseException(e);
		}
	}

	private Map<String, ReceivingNoteTO> parseReceivingListToMap(
			List<ReceivingNoteTO> receivingNoteList) throws BaseException {
		Map<String, ReceivingNoteTO> receivingNoteByStoreMap = new HashMap<String, ReceivingNoteTO>();

		for (int i = 0; i < receivingNoteList.size(); i++) {
			ReceivingNoteTO receivingNoteByStoreTO = receivingNoteList.get(i);
			String storeName = receivingNoteByStoreTO.getStoreName();
			storeName = storeName.substring(storeName.indexOf("-") + 1);
			String key = receivingNoteByStoreTO.getOrderNo() + storeName
					+ receivingNoteByStoreTO.getItemCode();
			if (receivingNoteByStoreMap.containsKey(key)) {
				ReceivingNoteTO existTO = receivingNoteByStoreMap.get(key);
				existTO.setQuantity(String.valueOf(Double
						.parseDouble(receivingNoteByStoreTO.getQuantity())
						+ Double.parseDouble(existTO.getQuantity())));
				existTO.setTotalPrice(String.valueOf(Double
						.parseDouble(receivingNoteByStoreTO.getTotalPrice())
						+ Double.parseDouble(existTO.getTotalPrice())));
				log.info("整合收货单: 原始数量: " + receivingNoteByStoreTO.getQuantity()
						+ " 原始总价: " + receivingNoteByStoreTO.getTotalPrice());

				log.info("整合收货单: 整合后数量: " + existTO.getQuantity() + " 整合后总价: "
						+ existTO.getTotalPrice());
			} else {
				receivingNoteByStoreMap.put(key, receivingNoteByStoreTO);
			}

		}
		return receivingNoteByStoreMap;
	}

	/**
	 * Merge Order Info and Receiving Info to one txt record
	 * 
	 * @param writer
	 * @param receivingNoteByStoreMap
	 * @param orderTOMap
	 * @throws BaseException
	 */
	private void mergeOrderAndReceiving(BufferedWriter writer,
			Map<String, ReceivingNoteTO> receivingNoteByStoreMap,
			Map<String, OrderTO> orderTOMap) throws BaseException {

		Object[] receivingKeyList = receivingNoteByStoreMap.keySet().toArray();
		Arrays.sort(receivingKeyList);

		int failedCount = 0;
		for (int i = 0; i < receivingKeyList.length; i++) {
			String combineKey = (String) receivingKeyList[i];

			ReceivingNoteTO receivingNoteTO = receivingNoteByStoreMap
					.get(combineKey);

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

				String mergedLine = orderTO.getOrderNo()
						+ "\t"
						+ receivingNoteTO.getStoreName()
						+ "\t"
						+ receivingNoteTO.getReceivingDate()
						+ "\t"
						+ orderTO.getItemCode()
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
						+ ((orderTO.getUnitPrice().equals("")) ? receivingNoteTO
								.getUnitPrice() : orderTO.getUnitPrice());
				try {
					writer.write(mergedLine);

					writer.newLine();
				} catch (IOException e) {
					FileUtil.closeFileWriter(writer);
					throw new BaseException(e);
				}
			} else {
				log.info("警告! 查不到收货单对应的订单信息. 收货单信息为: "
						+ receivingNoteTO.toString());
				failedCount++;
			}
		}

		log.info("收货单整合失败数量: " + failedCount);
	}
}
