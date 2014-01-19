package com.rsi.mengniu.retailer.rainbow.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.carrefour.service.CarrefourDataConversionService;
import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.RainbowReceivingTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
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

	public void retailerDataProcessing() throws BaseException {
		String retailerID = this.getRetailerID();
		Date startDate = Utils.getStartDate(retailerID);
		Date endDate = Utils.getEndDate(retailerID);

		getLog().info("开始处理数据");

		getLog().info("读取收货单数据:" + retailerID);
		// Get Receiving Note
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = getReceivingInfo(
				retailerID, startDate, endDate);

		getLog().info("读取收货单数据结束:" + retailerID);

		// go through the receiving map, generate map: key receiving date

		Map<String, List<ReceivingNoteTO>> receivingByDateMap = generateReceivingMapByDate(receivingNoteMap);

		Object[] receivingKeyList = receivingByDateMap.keySet().toArray();
		Arrays.sort(receivingKeyList);

		// Iterator Receiving Map by Date
		for (int i = 0; i < receivingKeyList.length; i++) {
			String processDateStr = (String) receivingKeyList[i];
			List<ReceivingNoteTO> receivingList = receivingByDateMap
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

		String sourceFilePath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_INBOUND_PATH);
		getLog().info(sourceFilePath);
		String destPath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_PROCESSED_PATH);
		;
		getLog().info(destPath);
		// Copy processed receiving note from inbound to processed folder
		// TODO
		FileUtil.moveFiles(FileUtil.getAllFile(sourceFilePath), sourceFilePath,
				destPath);

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

		getLog().info("数据处理结束");

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

	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(
			String retailerID, Date startDate, Date endDate, File salesFile)
			throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}
}
