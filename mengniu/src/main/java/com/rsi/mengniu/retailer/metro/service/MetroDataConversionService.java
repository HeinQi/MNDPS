package com.rsi.mengniu.retailer.metro.service;

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
import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class MetroDataConversionService extends RetailerDataConversionService {
	private static Log log = LogFactory.getLog(MetroDataConversionService.class);
	private Log summaryLog = LogFactory.getLog(Constants.SUMMARY_METRO);

	@Override
	protected Log getSummaryLog() {
		return summaryLog;
	}
	@Override
	protected String getRetailerID() {
		return Constants.RETAILER_METRO;
	}

	@Override
	protected Log getLog() {
		return log;
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
	 * Parse the receiving list of one day to map
	 * The key is: Order Number + Store Name + Item ID
	 * @param receivingNoteList
	 * @return
	 * @throws BaseException
	 */
	public Map<String, ReceivingNoteTO> generateReceivingMapForComparison(
			List<ReceivingNoteTO> receivingNoteList) throws BaseException {
		Map<String, ReceivingNoteTO> receivingNoteByStoreMap = new HashMap<String, ReceivingNoteTO>();
	
		for (int i = 0; i < receivingNoteList.size(); i++) {
			ReceivingNoteTO receivingNoteByStoreTO = receivingNoteList.get(i);
			
			String key = receivingNoteByStoreTO.getOrderNo() + receivingNoteByStoreTO.getStoreID()
					+ receivingNoteByStoreTO.getItemID();
			
			if (receivingNoteByStoreMap.containsKey(key)) {
				ReceivingNoteTO existTO = receivingNoteByStoreMap.get(key);
				existTO.setQuantity(String.valueOf(Double
						.parseDouble(receivingNoteByStoreTO.getQuantity().replaceAll(",", ""))
						+ Double.parseDouble(existTO.getQuantity().replaceAll(",", ""))));
				existTO.setTotalPrice(String.valueOf(Double
						.parseDouble(receivingNoteByStoreTO.getTotalPrice().replaceAll(",", ""))
						+ Double.parseDouble(existTO.getTotalPrice().replaceAll(",", ""))));
				getLog().info(
						"整合收货单: 原始数量: " + receivingNoteByStoreTO.getQuantity()
								+ " 原始总价: "
								+ receivingNoteByStoreTO.getTotalPrice());
	
				getLog().info(
						"整合收货单: 整合后数量: " + existTO.getQuantity() + " 整合后总价: "
								+ existTO.getTotalPrice());
			} else {
				receivingNoteByStoreMap.put(key, receivingNoteByStoreTO);
			}
	
		}
		return receivingNoteByStoreMap;
	}
	
//	@Override
//	protected Map<String, OrderTO> getOrderInfo(String retailerID,
//			Set<String> orderNoSet) throws BaseException {
//		Map<String, OrderTO> orderTOMap = new HashMap<String, OrderTO>();
//		for (String orderNo : orderNoSet) {
//			// Get order info map
//			// Key: Order Number + Store ID + Item Code
//			Map<String, OrderTO> orderMap = null;
//			
//			// Get order info map
//			orderMap = getOrderInfo(orderNo);
//			log.info("读取订单信息. 订单号:" + orderNo);
//			
//			orderTOMap.putAll(orderMap);
//
//			log.info("读取订单信息结束. 订单号:" + orderNo);
//		}
//
//		return orderTOMap;
//	}

//	private Map<String, OrderTO> getOrderInfo(String orderNo)
//			throws BaseException {
//		String fileName = Utils.getProperty(Constants.RETAILER_METRO
//				+ Constants.ORDER_INBOUND_PATH)
//				+ "Order_"
//				+ Constants.RETAILER_METRO
//				+ "_"
//				+ orderNo
//				+ ".txt";
//		log.info(fileName);
//		File orderFile = new File(fileName);
//
//		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
//
//		if (orderFile.exists()) {
//			BufferedReader reader = null;
//			try {
//				// Open the file
//				FileInputStream fileInput = new FileInputStream(orderFile);
//				InputStreamReader inputStrReader = new InputStreamReader(
//						fileInput, "UTF-8");
//				reader = new BufferedReader(inputStrReader);
//				reader.readLine();
//				// Read line by line
//				String orderLine = null;
//				while ((orderLine = reader.readLine()) != null) {
//					OrderTO orderTO = new OrderTO(orderLine);
//					String key = orderTO.getOrderNo()
//							+ orderTO.getStoreID()
//							+ orderTO.getItemID();
//					orderMap.put(key, orderTO);
//
//				}
//				// orderTOList.add(orderTO);
//
//			} catch (FileNotFoundException e) {
//				log.error(e);
//				throw new BaseException(e);
//			} catch (IOException e) {
//
//				log.error(e);
//				throw new BaseException(e);
//
//			} finally {
//
//				FileUtil.closeFileReader(reader);
//			}
//
//			log.info("订单: " + orderNo + " 包含的详单数量为:" + orderMap.size());
//
//		}
//		return orderMap;
//	}

	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(
			String retailerID, Date startDate, Date endDate, File salesFile)
			throws BaseException {
		return null;
	}
	
	protected Map<String, List<ReceivingNoteTO>> generateReceivingMapByDate(
			Map<String, List<ReceivingNoteTO>> receivingNoteMap) throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingByDateMap = new HashMap<String, List<ReceivingNoteTO>>();
		for (List<ReceivingNoteTO> receivingNoteList : receivingNoteMap
				.values()) {

			List<ReceivingNoteTO> receivingNoteByDateList = null;
			for (ReceivingNoteTO receivingNoteTO : receivingNoteList) {
				String processDate = receivingNoteTO.getReceivingDate();
				processDate = DateUtil.toString(DateUtil.toDate(processDate, "yyyy-M-d"));
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

			getLog().info("收货单日期：" + key + " 收货单数量:" + valueList.size());
		}

		return receivingByDateMap;
	}
	@Override
	protected Map<String, OrderTO> getOrderInfoFromFile(String retailerID, File orderFile) throws BaseException {
		

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();

		if (orderFile.exists()) {
			BufferedReader reader = null;
			try {
				// Open the file
				FileInputStream fileInput = new FileInputStream(orderFile);
				InputStreamReader inputStrReader = new InputStreamReader(
						fileInput, "UTF-8");
				reader = new BufferedReader(inputStrReader);
				reader.readLine();
				// Read line by line
				String orderLine = null;
				while ((orderLine = reader.readLine()) != null) {
					OrderTO orderTO = new OrderTO(orderLine);
					String key = orderTO.getOrderNo()
							+ orderTO.getStoreID()
							+ orderTO.getItemID();
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

			log.info("订单文件: " + orderFile.getName() + " 包含的详单数量为:" + orderMap.size());

		}
		return orderMap;
	}
}
