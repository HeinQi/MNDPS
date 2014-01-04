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
import java.text.ParseException;
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
import com.rsi.mengniu.util.FileUtil;

public class DataConversionService {

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
			writer.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new BaseException(e);
		}

		// Get Receiving Note
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = getReceivingInfo(
				retailerID, processDate);

		// Get Order Info
		for (Map.Entry<String, List<ReceivingNoteTO>> entry : receivingNoteMap
				.entrySet()) {
			String orderNo = entry.getKey();

			// Convert Receiving info list to map
			// Key: Store ID + Item Code
			List<ReceivingNoteTO> receivingList = entry.getValue();
			Map<String, ReceivingNoteTO> receivingNoteByStoreMap = parseReceivingListToMap(receivingList);

			// Get order info map
			// Key: Store ID + Item Code
			Map<String, OrderTO> orderTOMap = getOrderInfo(retailerID, orderNo);

			// Get matched receiving note by iterate order txt file
			// Merge to one record
			// Write to merged txt file
			mergeOrderAndReceiving(writer, receivingNoteByStoreMap, orderTOMap);
		}

		// Close the opened file
		FileUtil.closeFileWriter(writer);

		// Copy processed receiving note from inbound to processed folder

		// TODO
		// Move the merged data to completed folder
	}

	/**
	 * Get receiving data
	 * 
	 * @param retailerID
	 * @param processDate
	 * @return
	 */
	public static Map<String, List<ReceivingNoteTO>> getReceivingInfo(
			String retailerID, Date processDate) {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		File receivingInboundFolder = new File(Constants.TEST_ROOT_PATH
				+ retailerID + "/receiving/inbound/");

		File[] receivingList = receivingInboundFolder.listFiles();

		for (int i = 0; i < receivingList.length; i++) {
			File receivingFile = receivingList[i];

			Map<String, List<ReceivingNoteTO>> receivingNoteSingleMap = getReceivingInfoFromFile(receivingFile);

			receivingNoteMap.putAll(receivingNoteSingleMap);
		}

		return receivingNoteMap;

	}

	private static Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			File receivingFile) {

		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();
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
	 * Merge Order & Receiving Note to Txt file
	 * 
	 * @param retailID
	 * @param orderNo
	 * @param writer
	 * @param receivingNoteByStoreMap
	 */
	public static Map<String, OrderTO> getOrderInfo(String retailID,
			String orderNo) {
		String fileName = Constants.TEST_ROOT_PATH + retailID + "/order/Order_" + retailID + "_" 
				+ orderNo + ".txt";
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
					String key = orderTO.getStoreName().substring(3) + orderTO.getItemCode();

					orderMap.put(key, orderTO);

				}
				// orderTOList.add(orderTO);

				// Save merged file
				// Save updated order file

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {

				FileUtil.closeFileReader(reader);
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			FileUtil.closeFileReader(reader);

		}

		return orderMap;

	}

	private static Map<String, ReceivingNoteTO> parseReceivingListToMap(
			List<ReceivingNoteTO> receivingNoteList) throws BaseException {
		Map<String, ReceivingNoteTO> receivingNoteByStoreMap = new HashMap<String, ReceivingNoteTO>();

		for (int i = 0; i < receivingNoteList.size(); i++) {
			ReceivingNoteTO receivingNoteByStoreTO = receivingNoteList.get(i);
			String storeName = receivingNoteByStoreTO.getStoreName();
			storeName = storeName.substring(storeName.indexOf("-")+1);
			String key = 
					storeName+ receivingNoteByStoreTO.getItemCode();
			if (receivingNoteByStoreMap.containsKey(key)) {
				throw new BaseException();
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
	 */
	private static void mergeOrderAndReceiving(BufferedWriter writer,
			Map<String, ReceivingNoteTO> receivingNoteByStoreMap,
			Map<String, OrderTO> orderTOMap) {
		for (Map.Entry<String, OrderTO> orderEntry : orderTOMap.entrySet()) {
			String combineKey = orderEntry.getKey();

			// check if the item No. and store is matching.
			// if matched then merge order info and receiving note
			// info to txt
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
						+ orderTO.getItemCode() + "\t" + orderTO.getBarcode()
						+ "\t" + orderTO.getItemName() + "\t"
						+ orderTO.getQuantity() + "\t"
						+ orderTO.getTotalPrice() + "\t"
						+ receivingNoteTO.getQuantity() + "\t"
						+ receivingNoteTO.getTotalPrice() + "\t"
						+ orderTO.getUnitPrice();
				try {
					writer.write(mergedLine);

					writer.newLine();
				} catch (IOException e) {
					FileUtil.closeFileWriter(writer);
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
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
