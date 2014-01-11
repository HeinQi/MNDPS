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

public class DataConversionService {

	private static Log log = LogFactory.getLog(DataConversionService.class);

	public static void main(String[] args) throws BaseException {
		log.info("开始");
		
		FileUtil.testFileAmount("C:/root/yonghui/merged/");
//		retailerDataProcessing(Constants.RETAILER_CARREFOUR,
//				DateUtil.toDate("2013-12-01"), DateUtil.toDate("2014-01-03"));
//
//		retailerDataProcessing(Constants.RETAILER_TESCO,
//				DateUtil.toDate("2013-12-30"), DateUtil.toDate("2014-01-05"));
//
//		retailerDataProcessing(Constants.RETAILER_YONGHUI,
//				DateUtil.toDate("2013-12-25"), DateUtil.toDate("2014-01-11"));
		log.info("结束");
	}

	/**
	 * Process Data by time range
	 * 
	 * @param retailerID
	 * @param startDate
	 * @param endDate
	 * @throws BaseException
	 */
	public static void retailerDataProcessing(String retailerID,
			Date startDate, Date endDate) throws BaseException {

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

		String sourceFilePath = Constants.TEST_ROOT_PATH + retailerID
				+ "/receiving/inbound/";
		String destPath = Constants.TEST_ROOT_PATH + retailerID
				+ "/receiving/processed/";
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

	private static Map<String, List<ReceivingNoteTO>> generateReceivingMapByDate(
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
	 * Get Order Info
	 * 
	 * @param retailerID
	 * @param orderNoSet
	 * @return
	 * @throws BaseException
	 */
	private static Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException {

		Map<String, OrderTO> orderTOMap = null;

		if (retailerID.equals(Constants.RETAILER_CARREFOUR)) {
			orderTOMap = getOrderInfoForCarrefour(retailerID, orderNoSet);
		} else if (retailerID.equals(Constants.RETAILER_TESCO)) {
			orderTOMap = getOrderInfoForTesco(retailerID, orderNoSet);
		} else if (retailerID.equals(Constants.RETAILER_YONGHUI)) {
			orderTOMap = getOrderInfoForYonghui();
		}
		return orderTOMap;
	}

	/**
	 * Get order info
	 * 
	 * @param retailerID
	 * @param orderNo
	 * @param writer
	 * @param receivingNoteByStoreMap
	 * @throws BaseException
	 */
	public static Map<String, OrderTO> getOrderInfoForCarrefour(
			String retailerID, Set<String> orderNoSet) throws BaseException {
		Map<String, OrderTO> orderTOMap = new HashMap<String, OrderTO>();
		for (String orderNo : orderNoSet) {
			// Get order info map
			// Key: Store ID + Item Code
			Map<String, OrderTO> orderMap = null;
			orderMap = getOrderInfoForCarrefour(orderNo);
			log.info("读取订单信息. 订单号:" + orderNo);
			orderTOMap.putAll(orderMap);

			log.info("读取订单信息结束. 订单号:" + orderNo);
		}

		return orderTOMap;

	}

	private static Map<String, OrderTO> getOrderInfoForCarrefour(String orderNo)
			throws BaseException {
		String fileName = Constants.TEST_ROOT_PATH
				+ Constants.RETAILER_CARREFOUR + "/order/Order_"
				+ Constants.RETAILER_CARREFOUR + "_" + orderNo + ".txt";
		File orderFile = new File(fileName);

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();

		if (orderFile.exists()) {
			BufferedReader reader = null;
			try {
				// Open the file
				reader = new BufferedReader(new FileReader(orderFile));
				reader.readLine();
				// Read line by line
				String orderLine = null;
				while ((orderLine = reader.readLine()) != null) {
					OrderTO orderTO = new OrderTO(orderLine);
					String key = orderTO.getOrderNo()
							+ orderTO.getStoreName().substring(3)
							+ orderTO.getItemCode();
					orderMap.put(key, orderTO);

				}
				// orderTOList.add(orderTO);

			} catch (FileNotFoundException e) {
				log.error(e);
				throw new BaseException(e);
			} catch (IOException e) {

				log.error(e);
				throw new BaseException(e);

			} finally {

				FileUtil.closeFileReader(reader);
			}

			log.info("订单: " + orderNo + " 包含的详单数量为:" + orderMap.size());

		}
		return orderMap;
	}

	/**
	 * Get order info
	 * 
	 * @param retailerID
	 * @param orderNo
	 * @param writer
	 * @param receivingNoteByStoreMap
	 * @throws BaseException
	 */
	public static Map<String, OrderTO> getOrderInfoForTesco(String retailerID,
			Set<String> orderNoSet) throws BaseException {
		Map<String, OrderTO> orderTOMap = new HashMap<String, OrderTO>();
		for (String orderNo : orderNoSet) {
			// Get order info map
			// Key: Store ID + Item Code
			Map<String, OrderTO> orderMap = null;
			orderMap = getOrderInfoForTesco(orderNo);
			log.info("读取订单信息. 订单号:" + orderNo);
			orderTOMap.putAll(orderMap);

			log.info("读取订单信息结束. 订单号:" + orderNo);
		}

		return orderTOMap;

	}

	private static Map<String, OrderTO> getOrderInfoForTesco(String orderNo)
			throws BaseException {
		String fileName = Constants.TEST_ROOT_PATH + Constants.RETAILER_TESCO
				+ "/order/Order_" + Constants.RETAILER_TESCO + "_" + orderNo
				+ ".txt";
		File orderFile = new File(fileName);

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();

		if (orderFile.exists()) {
			BufferedReader reader = null;
			try {
				// Open the file
				reader = new BufferedReader(new FileReader(orderFile));
				reader.readLine();
				// Read line by line
				String orderLine = null;
				while ((orderLine = reader.readLine()) != null) {
					OrderTO orderTO = new OrderTO(orderLine);
					String key = orderTO.getOrderNo() + orderTO.getStoreName()
							+ orderTO.getItemCode();
					orderMap.put(key, orderTO);

				}
				// orderTOList.add(orderTO);

			} catch (FileNotFoundException e) {
				log.error(e);
				throw new BaseException(e);
			} catch (IOException e) {

				log.error(e);
				throw new BaseException(e);

			} finally {

				FileUtil.closeFileReader(reader);
			}

			log.info("订单: " + orderNo + " 包含的详单数量为:" + orderMap.size());

		}
		return orderMap;
	}

	private static Map<String, OrderTO> getOrderInfoForYonghui()
			throws BaseException {
		File receivingInboundFolder = new File(Constants.TEST_ROOT_PATH
				+ Constants.RETAILER_YONGHUI + "/order/");

		File[] orderList = receivingInboundFolder.listFiles();

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
		for (int i = 0; i < orderList.length; i++) {

			File orderFile = orderList[i];
			orderMap.putAll(getOrderInfoForYonghui(orderFile));
			log.info("订单文件名: " + orderFile.getName());

		}

		return orderMap;
	}

	private static Map<String, OrderTO> getOrderInfoForYonghui(File orderFile)
			throws BaseException {
		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();

		ExcelReader excelReader = null;
		try {
			excelReader = new ExcelReader();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nl.fountain.xelem.excel.Workbook wb = null;
		try {
			wb = excelReader.getWorkbook(orderFile.getPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		nl.fountain.xelem.excel.Worksheet sourceSheet = wb.getWorksheetAt(0);

		for (int i = 2; i <= sourceSheet.lastRow; i++) {
			nl.fountain.xelem.excel.Row sourceRow = sourceSheet.getRowAt(i);
			if (sourceRow == null) {
				continue;
			}

			OrderTO orderTO = new OrderTO();
			String orderNo = null;

			for (int j = 1; j <= sourceRow.maxCellIndex(); j++) {

				nl.fountain.xelem.excel.Cell sourceCell = sourceRow
						.getCellAt(j);

				switch (j) {
				case 2:
					orderNo = sourceCell.getData$();
					orderTO.setOrderNo(orderNo);

					continue;
				case 5:
					String storeID = sourceCell.getData$();
					orderTO.setStoreNo(storeID);

					continue;
				case 6:

					String storeName = sourceCell.getData$();
					orderTO.setStoreName(storeName);
					continue;
				case 7:
					String orderDate =  sourceCell.getData$();
					orderTO.setOrderDate(orderDate);

					continue;
				case 9:
					String itemCode =  sourceCell.getData$();
					orderTO.setItemCode(itemCode);

					continue;
				case 10:
					String barcode = sourceCell.getData$();
					orderTO.setBarcode(barcode);

					continue;
				case 11:

					String itemName = sourceCell.getData$();
					orderTO.setItemName(itemName);

					continue;
				case 13:
					String quantity = sourceCell.getData$();
					orderTO.setQuantity(quantity);

					continue;
				}

			}

			String key = orderTO.getOrderNo() + orderTO.getStoreName()
					+ orderTO.getItemCode();
			orderMap.put(key, orderTO);

		}

		return orderMap;
	}

	/**
	 * Get receiving data
	 * 
	 * @param retailerID
	 * @param processDate
	 * @return
	 * @throws BaseException
	 */
	public static Map<String, List<ReceivingNoteTO>> getReceivingInfo(
			String retailerID, Date startDate, Date endDate)
			throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		File receivingInboundFolder = new File(Constants.TEST_ROOT_PATH
				+ retailerID + "/receiving/inbound/");

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

	private static Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			String retailerID, Date startDate, Date endDate, File receivingFile)
			throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = null;
		if (retailerID.equals(Constants.RETAILER_CARREFOUR)) {
			receivingNoteMap = getReceivingInfoFromFileForCarrefour(
					receivingFile, startDate, endDate);
		} else if (retailerID.equals(Constants.RETAILER_TESCO)) {
			receivingNoteMap = getReceivingInfoFromFileForTesco(receivingFile,
					startDate, endDate);
		} else if (retailerID.equals(Constants.RETAILER_YONGHUI)) {
			receivingNoteMap = getReceivingInfoFromFileForYonghui(
					receivingFile, startDate, endDate);
		} else if (retailerID.equals(Constants.RETAILER_METRO)) {

		}

		return receivingNoteMap;

	}

	/**
	 * Get Receiving Info from file for Carrefour
	 * 
	 * @param receivingFile
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws BaseException
	 */
	private static Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFileForCarrefour(
			File receivingFile, Date startDate, Date endDate)
			throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();
		try {
			InputStream sourceExcel = new FileInputStream(receivingFile);

			Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);
			/*
			 * Workbook wb = null; if (fileType.equals("xls")) { wb = new
			 * HSSFWorkbook(); } else if(fileType.equals("xlsx")) { wb = new
			 * XSSFWorkbook(); }
			 */
			Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
			for (int i = 1; i <= sourceSheet.getPhysicalNumberOfRows(); i++) {
				Row sourceRow = sourceSheet.getRow(i);
				if (sourceRow == null) {
					continue;
				}

				String receivingDateStr = sourceRow.getCell(6)
						.getStringCellValue();
				Date receivingDate = DateUtil.toDate(receivingDateStr);

				// If receivingDate is in the date range
				if (DateUtil.isInDateRange(receivingDate, startDate, endDate)) {

					ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO();
					String orderNo = null;
					List<ReceivingNoteTO> receivingNoteTOList = null;

					for (int j = 0; j < sourceRow.getLastCellNum(); j++) {

						Cell sourceCell = sourceRow.getCell(j);

						String sourceCellValue = sourceCell
								.getStringCellValue();

						switch (j) {
						case 2:
							receivingNoteTO.setStoreNo(sourceCellValue);

							continue;
						case 3:
							receivingNoteTO.setStoreName(sourceCellValue);

							continue;
						case 6:
							receivingNoteTO.setReceivingDate(sourceCellValue);

							continue;
						case 7:
							receivingNoteTO.setOrderNo(sourceCellValue);
							orderNo = sourceCellValue;

							continue;
						case 8:
							receivingNoteTO.setItemCode(sourceCellValue);

							continue;
						case 9:
							receivingNoteTO.setItemName(sourceCellValue);

							continue;
						case 11:
							receivingNoteTO.setQuantity(sourceCellValue);

							continue;
						case 12:
							receivingNoteTO.setTotalPrice(sourceCellValue);

							continue;

						}

					}
					if (receivingNoteMap.containsKey(orderNo)) {
						receivingNoteTOList = receivingNoteMap.get(orderNo);
					} else {
						receivingNoteTOList = new ArrayList<ReceivingNoteTO>();
						// Test the Hashmap
						receivingNoteMap.put(orderNo, receivingNoteTOList);
					}

					log.debug("收货单详细条目: " + receivingNoteTO.toString());
					receivingNoteTOList.add(receivingNoteTO);

				}
			}
		} catch (FileNotFoundException e) {
			log.error(e);
			throw new BaseException(e);
		} catch (IOException e) {
			log.error(e);
			throw new BaseException(e);
		}
		return receivingNoteMap;
	}

	/**
	 * Get receiving info from file for Tesco
	 * 
	 * @param receivingFile
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws BaseException
	 */
	private static Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFileForTesco(
			File receivingFile, Date startDate, Date endDate)
			throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();
		try {
			InputStream sourceExcel = new FileInputStream(receivingFile);

			Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);
			String orderNo = null;
			String storeID = null;
			String storeName = null;
			String receivingDateStr = null;
			Date receivingDate = null;
			Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
			for (int i = 1; i < (sourceSheet.getPhysicalNumberOfRows() - 1); i++) {
				Row sourceRow = sourceSheet.getRow(i);
				if (sourceRow == null) {
					continue;
				}

				if (sourceRow.getCell(11).getStringCellValue() != null
						&& !sourceRow.getCell(11).getStringCellValue()
								.equals("")) {

					receivingDateStr = sourceRow.getCell(11)
							.getStringCellValue();
					receivingDate = DateUtil.toDate(receivingDateStr);
				}
				// If receivingDate is in the date range
				if (DateUtil.isInDateRange(receivingDate, startDate, endDate)) {

					ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO();
					List<ReceivingNoteTO> receivingNoteTOList = null;

					for (int j = 0; j < sourceRow.getLastCellNum(); j++) {
						Cell sourceCell = sourceRow.getCell(j);
						String sourceCellValue = null;

						int cellType = sourceCell.getCellType();
						if (cellType == Cell.CELL_TYPE_NUMERIC) {
							sourceCellValue = Double.valueOf(
									sourceCell.getNumericCellValue())
									.toString();
						} else {

							sourceCellValue = sourceCell.getStringCellValue();
						}
						switch (j) {
						case 3:
							if (sourceCellValue != null
									&& !sourceCellValue.equals("")) {
								storeID = sourceCellValue;
							}
							receivingNoteTO.setStoreNo(storeID);
							continue;
						case 4:

							if (sourceCellValue != null
									&& !sourceCellValue.equals("")) {
								storeName = sourceCellValue;
							}
							receivingNoteTO.setStoreName(storeName);
							continue;
						case 5:

							if (sourceCellValue != null
									&& !sourceCellValue.equals("")) {
								sourceCellValue = Utils
										.trimPrefixZero(sourceCellValue);
								orderNo = sourceCellValue;
							}
							receivingNoteTO.setOrderNo(orderNo);
							continue;
						case 11:
							receivingNoteTO.setReceivingDate(receivingDateStr);
							continue;
						case 14:
							receivingNoteTO.setItemCode(sourceCellValue);
							continue;
						case 15:
							receivingNoteTO.setItemName(sourceCellValue);
							continue;
						case 16:
							receivingNoteTO.setQuantity(sourceCellValue);
							continue;
						case 17:
							receivingNoteTO.setUnitPrice(sourceCellValue);
							continue;
						case 18:
							receivingNoteTO.setTotalPrice(sourceCellValue);
							continue;
						}
					}
					if (receivingNoteMap.containsKey(orderNo)) {
						receivingNoteTOList = receivingNoteMap.get(orderNo);
					} else {
						receivingNoteTOList = new ArrayList<ReceivingNoteTO>();
						// Test the Hashmap
						receivingNoteMap.put(orderNo, receivingNoteTOList);
					}

					log.debug("收货单详细条目: " + receivingNoteTO.toString());
					receivingNoteTOList.add(receivingNoteTO);

				}
			}
		} catch (FileNotFoundException e) {
			log.error(e);
			throw new BaseException(e);
		} catch (IOException e) {
			log.error(e);
			throw new BaseException(e);
		}

		for (Entry<String, List<ReceivingNoteTO>> entry : receivingNoteMap
				.entrySet()) {
			String key = entry.getKey();
			List valueList = entry.getValue();

			log.info("收货单对应订单编号：" + key + " 收货单数量:" + valueList.size());
		}

		return receivingNoteMap;
	}

	private static Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFileForYonghui(
			File receivingFile, Date startDate, Date endDate)
			throws BaseException {

		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		if (receivingFile.exists()) {
			BufferedReader reader = null;
			try {
				// Open the file
				reader = new BufferedReader(new FileReader(receivingFile));
				reader.readLine();
				// Read line by line
				String receivingLine = null;
				while ((receivingLine = reader.readLine()) != null) {
					ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO(
							receivingLine);
					String orderNo = receivingNoteTO.getOrderNo();

					List<ReceivingNoteTO> receivingNoteTOList = null;
					if (receivingNoteMap.containsKey(orderNo)) {
						receivingNoteTOList = receivingNoteMap.get(orderNo);
					} else {
						receivingNoteTOList = new ArrayList<ReceivingNoteTO>();
						// Test the Hashmap
						receivingNoteMap.put(orderNo, receivingNoteTOList);
					}

					log.debug("收货单详细条目: " + receivingNoteTO.toString());
					receivingNoteTOList.add(receivingNoteTO);

				}

			} catch (FileNotFoundException e) {
				log.error(e);
				throw new BaseException(e);
			} catch (IOException e) {

				log.error(e);
				throw new BaseException(e);

			} finally {

				FileUtil.closeFileReader(reader);
			}

			log.info("收货单: " + receivingFile.getName() + " 包含的详单数量为:"
					+ receivingNoteMap.size());

		}
		return receivingNoteMap;
	}

	/**
	 * Process Data of defined date
	 * 
	 * @param processDate
	 * @throws BaseException
	 */
	private static void retailerDataProcessing(String retailerID,
			String processDate, List<ReceivingNoteTO> receivingList,
			Map<String, OrderTO> orderTOMap) throws BaseException {

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

	private static BufferedWriter initMergeFile(String retailerID,
			String processDate) throws BaseException {
		BufferedWriter writer;
		String mergeFolderPath = Constants.TEST_ROOT_PATH + retailerID
				+ "/merged/";

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

	private static void writerMergeFileHeader(BufferedWriter writer)
			throws BaseException {
		String mergedHeader = "Order_No	Store_No	Receiving_Date	Item_Code	Barcode	Item_Name	Order_Qty	Order_Total_Price	Receiving_Qty	Receiving_Total_Price	Unit_Price";

		try {
			writer.write(mergedHeader);
			writer.newLine();
		} catch (IOException e) {

			throw new BaseException(e);
		}
	}

	private static Map<String, ReceivingNoteTO> parseReceivingListToMap(
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
	private static void mergeOrderAndReceiving(BufferedWriter writer,
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

				String mergedLine = orderTO.getOrderNo() + "\t"
						+ receivingNoteTO.getStoreName() + "\t"
						+ receivingNoteTO.getReceivingDate() + "\t"
						+ orderTO.getItemCode() + "\t" + orderTO.getBarcode()
						+ "\t" + orderTO.getItemName() + "\t"
						+ orderTO.getQuantity() + "\t"
						+ orderTO.getTotalPrice() + "\t"
						+ receivingNoteTO.getQuantity() + "\t"
						+ receivingNoteTO.getTotalPrice() + "\t"
						+ ((orderTO.getUnitPrice().equals(""))?receivingNoteTO.getUnitPrice():orderTO.getUnitPrice());
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

	/**
	 * Export Order from Excel to DB Start Date End Date Excel Name
	 */
	/*
	 * public static Map<String, List<ReceivingNoteTO>> processReceivingNoteXLS(
	 * String retailerID,Date processDate) { Map<String, List<ReceivingNoteTO>>
	 * receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();
	 * 
	 * // Filter the data in excel based on start end date try { InputStream
	 * sourceExcel = new FileInputStream(
	 * "C:/root/carrefour/receivingnote/source/inbound/" + "20131201.xlsx");
	 * 
	 * HSSFWorkbook sourceWorkbook = new HSSFWorkbook(sourceExcel);
	 * 
	 * HSSFSheet sourceSheet = sourceWorkbook.getSheetAt(0); for (int i = 1; i
	 * <= sourceSheet.getPhysicalNumberOfRows(); i++) { HSSFRow sourceRow =
	 * sourceSheet.getRow(i); if (sourceRow == null) { continue; }
	 * ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO(); // check if the
	 * row is in the date range HSSFCell receivingDateCell =
	 * sourceRow.getCell(6); String receivingDateStr = receivingDateCell
	 * .getStringCellValue();
	 * 
	 * // convert string to date: yyyy-mm-dd DateFormat dateFormat = new
	 * SimpleDateFormat("yyyy-MM-dd"); Date receivingDate =
	 * dateFormat.parse(receivingDateStr);
	 * 
	 * if (receivingDate.equals(receivingDate)) {
	 * 
	 * for (int j = 0; j < sourceRow.getLastCellNum(); j++) {
	 * 
	 * HSSFCell sourceCell = sourceRow.getCell(j);
	 * 
	 * String sourceCellValue = sourceCell .getStringCellValue(); String orderNo
	 * = null; List<ReceivingNoteTO> receivingNoteTOList = null;
	 * 
	 * switch (j) { case 0: case 1: case 4: case 5: case 10: continue; case 2:
	 * receivingNoteTO.setStoreNo(sourceCellValue); case 3:
	 * receivingNoteTO.setStoreName(sourceCellValue); case 6:
	 * receivingNoteTO.setReceivingDate(sourceCellValue); case 7:
	 * receivingNoteTO.setOrderNo(sourceCellValue); orderNo = sourceCellValue;
	 * case 8: receivingNoteTO.setItemCode(sourceCellValue); case 9:
	 * receivingNoteTO.setItemName(sourceCellValue); case 11:
	 * receivingNoteTO.setQuantity(sourceCellValue); case 12:
	 * receivingNoteTO.setTotalPrice(sourceCellValue);
	 * 
	 * } if (receivingNoteMap.containsKey(orderNo)) { receivingNoteTOList =
	 * receivingNoteMap.get(orderNo); } else { receivingNoteTOList = new
	 * ArrayList<ReceivingNoteTO>(); // Test the Hashmap
	 * receivingNoteMap.put(orderNo, receivingNoteTOList); }
	 * 
	 * receivingNoteTOList.add(receivingNoteTO); } } } } catch
	 * (FileNotFoundException e) { // TODO Auto-generated catch block throw new
	 * BaseException(e); } catch (IOException e) { // TODO Auto-generated catch
	 * block throw new BaseException(e); } catch (ParseException e) { // TODO
	 * Auto-generated catch block throw new BaseException(e); }
	 * 
	 * // Write to txt file exportReceivingNoteToTXT(receivingNoteMap);
	 * 
	 * return receivingNoteMap;
	 * 
	 * }
	 */
}
