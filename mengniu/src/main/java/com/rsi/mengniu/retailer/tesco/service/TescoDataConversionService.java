package com.rsi.mengniu.retailer.tesco.service;

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
import java.util.Map.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class TescoDataConversionService extends RetailerDataConversionService {

	private Log log = LogFactory.getLog(TescoDataConversionService.class);
	private Log summaryLog = LogFactory.getLog(Constants.SUMMARY_TESCO);

	@Override
	protected Log getSummaryLog() {
		return summaryLog;
	}

	@Override
	protected String getRetailerID() {

		return Constants.RETAILER_TESCO;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(String retailerID, Date startDate,
			Date endDate, File receivingFile) throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();
		

		List<ReceivingNoteTO> allReceivingNoteTOList = Utils.getReceivingNoteTOListFromFileForTesco(receivingFile);

		for (ReceivingNoteTO receivingNoteTO : allReceivingNoteTOList) {
			Date receivingDate = DateUtil.toDate(receivingNoteTO.getReceivingDate());
			if (DateUtil.isInDateRange(receivingDate, startDate, endDate)) {
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
		}

		for (Entry<String, List<ReceivingNoteTO>> entry : receivingNoteMap.entrySet()) {
			String key = entry.getKey();
			List valueList = entry.getValue();

			log.info("收货单对应订单编号：" + key + " 收货单数量:" + valueList.size());
		}

		return receivingNoteMap;
	}

	

	// @Override
	// protected Map<String, OrderTO> getOrderInfo(String retailerID,
	// Set<String> orderNoSet) throws BaseException {
	// Map<String, OrderTO> orderTOMap = new HashMap<String, OrderTO>();
	// for (String orderNo : orderNoSet) {
	// // Get order info map
	// // Key: Store ID + Item Code
	// Map<String, OrderTO> orderMap = null;
	// orderMap = getOrderInfo(orderNo);
	// log.info("读取订单信息. 订单号:" + orderNo);
	// orderTOMap.putAll(orderMap);
	//
	// log.info("读取订单信息结束. 订单号:" + orderNo);
	// }
	//
	// return orderTOMap;
	// }

	@Override
	protected Map<String, OrderTO> getOrderInfoFromFile(String retailerID, File orderFile) throws BaseException {
		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();

		if (orderFile.exists()) {

			String fileName = orderFile.getName();
			String[] splitStr = fileName.split("_");
			String userID = splitStr[2];
			
			BufferedReader reader = null;
			try {
				// Open the file
				FileInputStream fileInput = new FileInputStream(orderFile);
				InputStreamReader inputStrReader = new InputStreamReader(fileInput, "UTF-8");
				reader = new BufferedReader(inputStrReader);
				reader.readLine();
				// Read line by line
				String orderLine = null;
				while ((orderLine = reader.readLine()) != null) {
					OrderTO orderTO = new OrderTO(orderLine);
					orderTO.setUserID(userID);
					String key = orderTO.getOrderNo() + orderTO.getStoreName() + orderTO.getItemID();
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

			log.info("订单文件: " + orderFile + " 包含的详单数量为:" + orderMap.size());

		}
		return orderMap;
	}

	// private Map<String, OrderTO> getOrderInfo(String orderNo) throws
	// BaseException {
	// String fileName = Utils.getProperty(Constants.RETAILER_TESCO +
	// Constants.ORDER_INBOUND_PATH) + "Order_"
	// + Constants.RETAILER_TESCO + "_" + orderNo + ".txt";
	// File orderFile = new File(fileName);
	//
	// Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
	//
	// if (orderFile.exists()) {
	// BufferedReader reader = null;
	// try {
	// // Open the file
	// FileInputStream fileInput = new FileInputStream(orderFile);
	// InputStreamReader inputStrReader = new InputStreamReader(fileInput,
	// "UTF-8");
	// reader = new BufferedReader(inputStrReader);
	// reader.readLine();
	// // Read line by line
	// String orderLine = null;
	// while ((orderLine = reader.readLine()) != null) {
	// OrderTO orderTO = new OrderTO(orderLine);
	// String key = orderTO.getOrderNo() + orderTO.getStoreName() +
	// orderTO.getItemID();
	// orderMap.put(key, orderTO);
	//
	// }
	// // orderTOList.add(orderTO);
	//
	// } catch (FileNotFoundException e) {
	// log.error(e);
	// throw new BaseException(e);
	// } catch (IOException e) {
	//
	// log.error(e);
	// throw new BaseException(e);
	//
	// } finally {
	//
	// FileUtil.closeFileReader(reader);
	// }
	//
	// log.info("订单: " + orderNo + " 包含的详单数量为:" + orderMap.size());
	//
	// }
	// return orderMap;
	// }

	@Override
	protected Log getLog() {
		return log;
	}

	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(String retailerID, Date startDate, Date endDate,
			File salesFile) throws BaseException {
		Map<String, List<SalesTO>> salesMap = new HashMap<String, List<SalesTO>>();

		if (salesFile.exists()) {

			String fileName = salesFile.getName();
			String[] splitStr = fileName.split("_");
			String userID = splitStr[2];
			
			
			BufferedReader reader = null;
			try {
				// Open the file
				FileInputStream fileInput = new FileInputStream(salesFile);
				InputStreamReader inputStrReader = new InputStreamReader(fileInput, "UTF-8");
				reader = new BufferedReader(inputStrReader);
				reader.readLine();
				// Read line by line
				String salesLine = null;
				while ((salesLine = reader.readLine()) != null) {
					SalesTO salesTO = new SalesTO(salesLine);
					salesTO.setUserID(userID);
					String salesDateStr = salesTO.getSalesDate();
					List<SalesTO> salesTOList = null;
					if (salesMap.containsKey(salesDateStr)) {
						salesTOList = salesMap.get(salesDateStr);
					} else {
						salesTOList = new ArrayList<SalesTO>();
						salesMap.put(salesDateStr, salesTOList);
					}

					log.debug("销售单详细条目: " + salesTO.toString());
					salesTOList.add(salesTO);

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

			log.info("销售单包含的详单数量为:" + salesMap.size());

		}
		return salesMap;
	}

}
