package com.rsi.mengniu.retailer.rainbow.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.RainbowReceivingTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class RainbowDataConversionService extends RetailerDataConversionService {

	private Log log = LogFactory.getLog(RainbowDataConversionService.class);

	@Override
	protected String getRetailerID() {
		return Constants.RETAILER_RAINBOW;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			String retailerID, Date startDate, Date endDate, File receivingFile)
			throws BaseException {
		return getReceivingInfoFromFileByStatus(receivingFile);
	}

	private Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFileByStatus(
			File receivingFile) throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		if (receivingFile.exists()) {
			BufferedReader reader = null;
			try {
				// Open the file
				FileInputStream fileInput = new FileInputStream(receivingFile);
				InputStreamReader inputStrReader = new InputStreamReader(
						fileInput, "UTF-8");
				reader = new BufferedReader(inputStrReader);
				reader.readLine();
				// Read line by line
				String receivingLine = null;
				while ((receivingLine = reader.readLine()) != null) {
					RainbowReceivingTO receivingNoteTO = new RainbowReceivingTO(
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

	@Override
	protected Log getLog() {
		return log;
	}

	@Override
	protected Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}
	

	public void processOrderData(String retailerID, Date startDate,
			Date endDate) throws BaseException {
		// Get Receiving Note
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = getReceivingInfo(
				retailerID, startDate, endDate);
		
		//split the map to normal and exception
		
		ArrayList<ReceivingNoteTO> receivingNormalList = new ArrayList<ReceivingNoteTO>();
		ArrayList<ReceivingNoteTO> receivingAbnormalList = new ArrayList<ReceivingNoteTO>();
		
		Map<String, List<ReceivingNoteTO>> receivingNormalMap = new HashMap<String, List<ReceivingNoteTO>>();

		Map<String, List<ReceivingNoteTO>> receivingAbnormalMap = new HashMap<String, List<ReceivingNoteTO>>();
		
		for(List<ReceivingNoteTO> receivingList:receivingNoteMap.values()){
			for(ReceivingNoteTO receivingTO: receivingList){
				if(receivingTO.getStoreID()!=null && !receivingTO.getStoreID().equals("")){
					receivingNormalList.add(receivingTO);
				}else {
					receivingAbnormalList.add(receivingTO);
				}
			}
		}
		receivingNormalMap.put(retailerID, receivingNormalList);
		receivingAbnormalMap.put(retailerID, receivingAbnormalList);


		getLog().info("读取收货单数据结束:" + retailerID);

		
		// go through the receiving map, generate map: key receiving date
		//Normal

		Map<String, List<ReceivingNoteTO>> receivingNormalByDateMap = generateReceivingMapByDate(receivingNormalMap);

		Object[] receivingNormalKeyList = receivingNormalByDateMap.keySet().toArray();
		Arrays.sort(receivingNormalKeyList);

		// Iterator Receiving Map by Date
		for (int i = 0; i < receivingNormalKeyList.length; i++) {
			String processDateStr = (String) receivingNormalKeyList[i];
			List<ReceivingNoteTO> receivingList = receivingNormalByDateMap
					.get(processDateStr);

			if (!(receivingList.size() == 0)) {

				getLog().info(
						"开始整合. 零售商: " + retailerID + " 日期:" + processDateStr
								+ "订单数量:" + receivingList.size());
				retailerDataProcessing(retailerID, processDateStr,
						receivingList);

				getLog().info(
						"整合结束. 零售商: " + retailerID + " 日期:" + processDateStr
								+ "订单数量:" + receivingList.size() + "\n");
			}
		}
		

		// go through the receiving map, generate map: key receiving date
		// Exception
		Map<String, List<ReceivingNoteTO>> receivingAbnormalByDateMap = generateReceivingMapByDate(receivingAbnormalMap);

		Object[] receivingAbnormalKeyList = receivingAbnormalByDateMap.keySet().toArray();
		Arrays.sort(receivingAbnormalKeyList);

		// Iterator Receiving Map by Date
		for (int i = 0; i < receivingAbnormalKeyList.length; i++) {
			String processDateStr = (String) receivingAbnormalKeyList[i];
			List<ReceivingNoteTO> receivingList = receivingAbnormalByDateMap
					.get(processDateStr);

			if (!(receivingList.size() == 0)) {

				getLog().info(
						"开始整合. 零售商: " + retailerID + " 日期:" + processDateStr
								+ "订单数量:" + receivingList.size());
				retailerDataAbnormalProcessing(retailerID, processDateStr,
						receivingList);

				getLog().info(
						"整合结束. 零售商: " + retailerID + " 日期:" + processDateStr
								+ "订单数量:" + receivingList.size() + "\n");
			}
		}
		


		String sourceFilePath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_INBOUND_PATH);
		getLog().info(sourceFilePath);
		String destPath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_PROCESSED_PATH);

		getLog().info(destPath);
		FileUtil.moveFiles(FileUtil.getAllFile(sourceFilePath), sourceFilePath,
				destPath);
	}

	/**
	 * Process Data of defined date
	 * 
	 * @param processDate
	 * @throws BaseException
	 */
	private void retailerDataProcessing(String retailerID, String processDate,
			List<ReceivingNoteTO> receivingList) throws BaseException {

		BufferedWriter writer = initOrderOutputFile(retailerID, processDate);

		// Get matched receiving note by iterate order txt file
		// Merge to one record
		// Write to merged txt file

		getLog().info("开始整合订单和收货单信息. 零售商: " + retailerID + " 日期:" + processDate);
		mergeReceiving(writer, receivingList);

		getLog().info("整合订单和收货单信息结束. 零售商: " + retailerID + " 日期:" + processDate);

		// Close the opened file
		FileUtil.closeFileWriter(writer);

	}


	/**
	 * Process Data of defined date
	 * 
	 * @param processDate
	 * @throws BaseException
	 */
	private void retailerDataAbnormalProcessing(String retailerID, String processDate,
			List<ReceivingNoteTO> receivingList) throws BaseException {

		BufferedWriter writer = initAbnormalOrderOutputFile(retailerID, processDate);

		// Get matched receiving note by iterate order txt file
		// Merge to one record
		// Write to merged txt file

		getLog().info("开始整合订单和收货单信息. 零售商: " + retailerID + " 日期:" + processDate);
		mergeReceiving(writer, receivingList);

		getLog().info("整合订单和收货单信息结束. 零售商: " + retailerID + " 日期:" + processDate);

		// Close the opened file
		FileUtil.closeFileWriter(writer);

	}
	

	private BufferedWriter initAbnormalOrderOutputFile(String retailerID, String processDate)
			throws BaseException {
		BufferedWriter writer;
		String mergeFolderPath = Utils.getProperty(retailerID
				+ Constants.OUTPUT_ORDER_EXCEPTION_PATH);

		String mergeFilePath = mergeFolderPath + retailerID + "_order_"
				+ DateUtil.toStringYYYYMMDD(DateUtil.toDate(processDate))
				+ ".txt";
		getLog().info("初始化整合文本文件. 文件名: " + mergeFilePath);
		File mergeFolder = new File(mergeFolderPath);
		if (!mergeFolder.exists()) {
			mergeFolder.mkdirs();
		}

		File mergeFile = new File(mergeFilePath);

		try {
			FileOutputStream fileOutput = new FileOutputStream(mergeFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput,
					"UTF-8"));
		} catch (IOException e) {
			throw new BaseException(e);
		}

		writerOrderOutputFileHeader(writer);
		return writer;
	}

	/**
	 * Merge Order Info and Receiving Info to one txt record
	 * 
	 * @param writer
	 * @param receivingNotelist
	 * @param orderTOMap
	 * @throws BaseException
	 */
	private void mergeReceiving(BufferedWriter writer,
			List<ReceivingNoteTO> receivingNotelist) throws BaseException {

		int failedCount = 0;
		for (int i = 0; i < receivingNotelist.size(); i++) {

			RainbowReceivingTO receivingNoteTO = (RainbowReceivingTO) receivingNotelist
					.get(i);

			String mergedLine = receivingNoteTO.toString();
			try {
				writer.write(mergedLine);

				writer.newLine();
			} catch (IOException e) {
				FileUtil.closeFileWriter(writer);
				throw new BaseException(e);
			}

		}
		if (failedCount != 0) {
			getLog().info("收货单整合失败数量: " + failedCount);
		}
	}

	/**
	 * Key Date
	 * Value SalesTO List
	 */
	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(
			String retailerID, Date startDate, Date endDate, File salesFile)
			throws BaseException {
		Map<String, List<SalesTO>> salesMap = new HashMap<String, List<SalesTO>>();
		try {
			InputStream sourceExcel = new FileInputStream(salesFile);

			Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);

			Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
			for (int i = 1; i <= sourceSheet.getPhysicalNumberOfRows(); i++) {
				Row sourceRow = sourceSheet.getRow(i);
				if (sourceRow == null) {
					continue;
				}

				String salesDateStr = sourceRow.getCell(8).getStringCellValue();
				Date salesDate = DateUtil.toDate(salesDateStr);

				// If receivingDate is in the date range
				if (DateUtil.isInDateRange(salesDate, startDate, endDate)) {

					SalesTO salesTO = new SalesTO();
					List<SalesTO> salesTOList = null;

					salesTO.setItemID(sourceRow.getCell(1).getStringCellValue());
					salesTO.setItemName(sourceRow.getCell(2).getStringCellValue());
					salesTO.setStoreID(sourceRow.getCell(5).getStringCellValue());
					salesTO.setSalesDate(salesDateStr);
					salesTO.setSalesQuantity(sourceRow.getCell(6).getStringCellValue());
					salesTO.setSalesAmount(sourceRow.getCell(7).getStringCellValue());
					
					
					if (salesMap.containsKey(salesDateStr)) {
						salesTOList = salesMap.get(salesDateStr);
					} else {
						salesTOList = new ArrayList<SalesTO>();
						salesMap.put(salesDateStr, salesTOList);
					}

					log.debug("销售单详细条目: " + salesTO.toString());
					salesTOList.add(salesTO);

				}
			}
		} catch (FileNotFoundException e) {
			log.error(e);
			throw new BaseException(e);
		} catch (IOException e) {
			log.error(e);
			throw new BaseException(e);
		}
		return salesMap;
	}
}
