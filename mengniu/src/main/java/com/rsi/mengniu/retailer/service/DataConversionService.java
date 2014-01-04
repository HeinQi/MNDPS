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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.util.DateUtil;

public class DataConversionService {

	private static String root_path = "C:/root/";

	public static void main(String[] args) throws BaseException {

		retailerDataProcessing(Constants.RETAILER_CARREFOUR,
				DateUtil.toDate("2013-12-01"), DateUtil.toDate("2013-12-01"));

	}

	/**
	 * Process Data by time range
	 * 
	 * @param startDate
	 * @param endDate
	 * @throws BaseException
	 */
	public static void retailerDataProcessing(String retailerID,
			Date startDate, Date endDate) throws BaseException {
		List<Date> dateList = null;
		try {
			dateList = DateUtil.getDateArrayByRange(startDate, endDate);
			for (int i = 0; i < dateList.size(); i++) {
				Date processDate = dateList.get(i);
				retailerDataProcessing(retailerID, processDate);
			}

		} catch (ParseException e) {
			throw new BaseException(e);
		}

	}

	/**
	 * Process Data of defined date
	 * 
	 * @param processDate
	 * @throws BaseException
	 */
	public static void retailerDataProcessing(String retailerID,
			Date processDate) throws BaseException {
		File mergeFile = new File("C:/root/" + retailerID + "/merged/"
				+ retailerID + "_order_"
				+ DateUtil.toStringYYYYMMDD(processDate) + ".txt");
		String mergedHeader = "Order_No	Store_No	Receiving_Date	Item_Code	Barcode	Item_Name	Order_Qty	Order_Total_Price	Receiving_Qty	Receiving_Total_Price	Unit_Price";

		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(mergeFile));
			writer.write(mergedHeader);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new BaseException(e);
		}

		// Process Receiving Note
		// Get data from excel
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = processReceiving(
				retailerID, processDate);

		// Get matched order info from TXT
		for (Map.Entry<String, List<ReceivingNoteTO>> entry : receivingNoteMap
				.entrySet()) {
			String orderNo = entry.getKey();

			// prepare the map which key is "store No + item code"
			Map<String, ReceivingNoteTO> receivingNoteByStoreMap = parseToMapByStoreAndItem(entry
					.getValue());

			// Get order info txt file by receiving note order No
			// Get matched receiving note by iterate order txt file
			// Merge to one record
			// Write to merged txt file

			Map<String, OrderTO> orderTOMap = getOrderInfo(retailerID, orderNo);

			for (Map.Entry<String, OrderTO> orderEntry : orderTOMap.entrySet()) {
				String combineKey = orderEntry.getKey();
				if (receivingNoteByStoreMap.containsKey(combineKey)) {

					OrderTO orderTO = orderEntry.getValue();
					ReceivingNoteTO receivingNoteTO = receivingNoteByStoreMap
							.get(combineKey);
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
							+ orderTO.getStoreNo() + "\t"
							+ receivingNoteTO.getReceivingDate() + "\t"
							+ orderTO.getItemCode() + "\t"
							+ orderTO.getBarcode() + "\t"
							+ orderTO.getItemName() + "\t"
							+ orderTO.getQuantity() + "\t"
							+ orderTO.getTotalPrice() + "\t"
							+ receivingNoteTO.getQuantity() + "\t"
							+ receivingNoteTO.getTotalPrice() + "\t"
							+ orderTO.getUnitPrice();
					try {
						writer.write(mergedLine);

						writer.newLine();
					} catch (IOException e) {
						closeFileWriter(writer);
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		// check if the item No. and store is matching.
		// if matched then merge order info and receiving note
		// info to txt

		// Close the opened file
		closeFileWriter(writer);

		// Copy processed receiving note and order file from inbound to
		// processed folder

		// Move the merged data to completed folder
	}

	/**
	 * Merge Order & Receiving Note to Txt file
	 * 
	 * @param retailID
	 * @param orderNo
	 * @param writer
	 * @param receivingNoteByStoreMap
	 */
	public static Map<String, OrderTO> getOrderInfo(String retailID,
			String orderNo) {
		String fileName = root_path + retailID + "/order/" + orderNo + ".txt";
		File orderFile = new File(fileName);

		if (orderFile.exists()) {
			BufferedReader reader = null;
			Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
			try {
				// Open the file
				reader = new BufferedReader(new FileReader(orderFile));
				reader.readLine();
				// Read line by line
				String orderLine = null;
				while ((orderLine = reader.readLine()) != null) {
					OrderTO orderTO = new OrderTO(orderLine);
					String key = orderTO.getStoreNo() + orderTO.getItemCode();

					orderMap.put(key, orderTO);

				}
				// orderTOList.add(orderTO);

				// Save merged file
				// Save updated order file

				closeFileReader(reader);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return orderMap;

		}

		return null;

	}

	private static Map<String, ReceivingNoteTO> parseToMapByStoreAndItem(
			List<ReceivingNoteTO> receivingNoteList) throws BaseException {
		Map<String, ReceivingNoteTO> receivingNoteByStoreMap = new HashMap<String, ReceivingNoteTO>();

		for (int i = 0; i < receivingNoteList.size(); i++) {
			ReceivingNoteTO receivingNoteByStoreTO = receivingNoteList.get(i);
			String key = receivingNoteByStoreTO.getStoreNo()
					+ receivingNoteByStoreTO.getItemCode();
			if (receivingNoteByStoreMap.containsKey(key)) {
				throw new BaseException();
			} else {

				receivingNoteByStoreMap.put(key, receivingNoteByStoreTO);
			}

		}
		return receivingNoteByStoreMap;
	}

	private static void closeFileWriter(BufferedWriter writer) {
		if (writer != null) {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void closeFileReader(BufferedReader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get receiving data
	 * 
	 * @param retailerID
	 * @param processDate
	 * @return
	 */
	public static Map<String, List<ReceivingNoteTO>> processReceiving(
			String retailerID, Date processDate) {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		File receivingInboundFolder = new File(root_path + retailerID
				+ "/receivingnote/excel/inbound/");

		File[] receivingList = receivingInboundFolder.listFiles();

		for (int i = 0; i < receivingList.length; i++) {
			File receivingFile = receivingList[i];

			Map<String, List<ReceivingNoteTO>> receivingNoteSingleMap = getReceivingInfoFromFile(receivingFile);

			receivingNoteMap.putAll(receivingNoteSingleMap);
		}

		return receivingNoteMap;

	}

	/*
	 * public static void exportReceivingNoteToTXT( Map<String,
	 * List<ReceivingNoteTO>> receivingNoteMap) {
	 * 
	 * for (Map.Entry<String, List<ReceivingNoteTO>> entry : receivingNoteMap
	 * .entrySet()) { String orderNo = entry.getKey(); List<ReceivingNoteTO>
	 * receivingNoteList = entry.getValue();
	 * 
	 * File destFile = new File(
	 * "C:/root/carrefour/receivingnote/generated/inbound/" + orderNo + ".txt");
	 * // TODO 移动文件覆盖时要考虑合并相同订单号的文件
	 * 
	 * BufferedWriter writer = null; try { writer = new BufferedWriter(new
	 * FileWriter(destFile));
	 * 
	 * for (ReceivingNoteTO receivingNoteTO : receivingNoteList) { String
	 * receivingNoteRow = receivingNoteTO.toString(); if (receivingNoteRow !=
	 * null) { writer.write(receivingNoteRow); writer.newLine(); } }
	 * 
	 * writer.flush(); writer.close();
	 * 
	 * } catch (IOException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } finally { closeFileWriter(writer); }
	 * 
	 * } }
	 */

	private static Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			File receivingFile) {

		Map<String, List<ReceivingNoteTO>> receivingNoteMap = null;
		try {
			InputStream sourceExcel = new FileInputStream(receivingFile);

			Workbook sourceWorkbook = new XSSFWorkbook(sourceExcel);
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
				ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO();
				// check if the row is in the date range
				Cell receivingDateCell = sourceRow.getCell(6);
				String receivingDateStr = receivingDateCell
						.getStringCellValue();

				// convert string to date: yyyy-mm-dd

				String orderNo = null;
				List<ReceivingNoteTO> receivingNoteTOList = null;

				for (int j = 0; j < sourceRow.getLastCellNum(); j++) {

					Cell sourceCell = sourceRow.getCell(j);

					String sourceCellValue = sourceCell.getStringCellValue();

					switch (j) {
					case 0:
					case 1:
					case 4:
					case 5:
					case 10:
						continue;
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
					if (receivingNoteMap.containsKey(orderNo)) {
						receivingNoteTOList = receivingNoteMap.get(orderNo);
					} else {
						receivingNoteTOList = new ArrayList<ReceivingNoteTO>();
						// Test the Hashmap
						receivingNoteMap.put(orderNo, receivingNoteTOList);
					}

					receivingNoteTOList.add(receivingNoteTO);

				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return receivingNoteMap;

	}

	/**
	 * Export Under Info to TXT file Start Date End Date
	 * 
	 */
	public void exportOrderInfoToTXT() {
		// Get order info from database

		// Export order info to TXT
		// One user one day one txt file
	}

	/**
	 * Export Receiving Note from Excel to DB
	 */
	public void importReceivingNoteInfoFromExcel() {
		// Filter data in excel based on start end date

		// Update the data to DB
	}

	public void copyFile() {
		// 文件原地址
		File oldFile = new File("c:/test.xls");
		// 文件新（目标）地址
		String newPath = "c:/test/";
		// new一个新文件夹
		File fnewpath = new File(newPath);
		// 判断文件夹是否存在
		if (!fnewpath.exists())
			fnewpath.mkdirs();
		// 将文件移到新文件里
		File fnew = new File(newPath + oldFile.getName());
		oldFile.renameTo(fnew);

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
	 * (FileNotFoundException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } catch (ParseException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); }
	 * 
	 * // Write to txt file exportReceivingNoteToTXT(receivingNoteMap);
	 * 
	 * return receivingNoteMap;
	 * 
	 * }
	 */
}
