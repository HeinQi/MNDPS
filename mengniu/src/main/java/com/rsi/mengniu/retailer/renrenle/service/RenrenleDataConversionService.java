package com.rsi.mengniu.retailer.renrenle.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.AccountLogUtil;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class RenrenleDataConversionService extends RetailerDataConversionService {

	private Log log = LogFactory.getLog(RenrenleDataConversionService.class);
	private Log summaryLog = LogFactory.getLog(Constants.SUMMARY_RENRENLE);

	@Override
	protected Log getSummaryLog() {
		return summaryLog;
	}

	@Override
	protected String getRetailerID() {
		return Constants.RETAILER_RENRENLE;
	}

	@Override
	protected Log getLog() {
		return log;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(String retailerID, Date startDate,
			Date endDate, File receivingFile) throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}

	// @Override
	// protected Map<String, OrderTO> getOrderInfo(String retailerID,
	// Set<String> orderNoSet) throws BaseException {
	// // TODO Auto-generated method stub
	// return null;
	// }

	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(String retailerID, Date startDate, Date endDate,
			File salesFile) throws BaseException {

		List<SalesTO> salesTOList = Utils.getSalesTOListFromFileForRenrenle(salesFile);

		// log.info("销售单生成日期：" + salesDate + "。销售开始日期：" +
		// DateUtil.toString(startDate) + "。销售截至日期："
		// + DateUtil.toString(endDate));
		Map<String, List<SalesTO>> salesMap = new HashMap<String, List<SalesTO>>();

		for (SalesTO salesTO : salesTOList) {
			String salesDate = salesTO.getSalesDate();

			List<SalesTO> salesList = null;
			if (salesMap.containsKey(salesDate)) {
				salesList = salesMap.get(salesDate);
			} else {
				salesList = new ArrayList<SalesTO>();
				// Test the Hashmap
				salesMap.put(salesDate, salesList);
			}

			log.debug("销售单详细条目: " + salesTO.toString());
			salesList.add(salesTO);
		}

		return salesMap;
	}

	public void processOrderData(String retailerID, Date startDate, Date endDate) throws BaseException {
		// Get Receiving Data
		// Map<String, List<ReceivingNoteTO>> receivingNoteMap =
		// getReceivingInfo(retailerID, startDate, endDate);

		// Get Order Data
		Map<String, List<OrderTO>> orderMap = this.getOrderInfo(retailerID, startDate, endDate);
		// Match

		// Generate Output file
		convertOrderData(retailerID, orderMap);

		// Move Order file to Processed

		String sourceFilePath = Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH);
		getLog().info(sourceFilePath);
		String destPath = Utils.getProperty(retailerID + Constants.ORDER_PROCESSED_PATH);
		getLog().info(destPath);
		// Copy processed receiving note from inbound to processed folder
		FileUtil.moveFiles(FileUtil.getAllFile(sourceFilePath), sourceFilePath, destPath);

	}

	private void convertOrderData(String retailerID, Map<String, List<OrderTO>> orderMap) throws BaseException {
		Map<String, Set<String>> mergedOrderMap = new HashMap<String, Set<String>>();
		for (String orderDateStr : orderMap.keySet()) {
			BufferedWriter writer = initOrderOutputFile(retailerID, orderDateStr);
			List<OrderTO> orderList = orderMap.get(orderDateStr);
			for (OrderTO orderTO : orderList) {
				String outputLine = orderTO.getOrderNo() + "\t" + orderTO.getStoreID() + "\t" + orderTO.getStoreName()
						+ "\t" + orderTO.getOrderDate() + "\t" + orderTO.getItemID() + "\t" + orderTO.getBarcode()
						+ "\t" + orderTO.getItemName() + "\t" + orderTO.getQuantity() + "\t" + orderTO.getTotalPrice()
						+ "\t" + "" + "\t" + "" + "\t" + orderTO.getUnitPrice();
				try {
					writer.write(outputLine);
					writer.newLine();
				} catch (IOException e) {
					FileUtil.closeFileWriter(writer);
					throw new BaseException(e);
				}

				// 生成合并成功的数据，为Account Log准备数据
				Set<String> orderNoSet = new HashSet<String>();
				String mergedOrderKey = retailerID + "--" + orderTO.getUserID() + "--" + orderTO.getOrderDate();
				if (mergedOrderMap.containsKey(mergedOrderKey)) {
					orderNoSet = mergedOrderMap.get(mergedOrderKey);
				}
				orderNoSet.add(orderTO.getOrderNo());
				mergedOrderMap.put(mergedOrderKey, orderNoSet);
			}
			FileUtil.closeFileWriter(writer);

		}
		AccountLogUtil.updateProcessedOrderInfo(mergedOrderMap);

	}

	protected void writerOrderOutputFileHeader(BufferedWriter writer) throws BaseException {
		String mergedHeader = Utils.getProperty(Constants.RENRENLE_OUTPUT_ORDER_HEADER);
		try {
			writer.write(mergedHeader);
			writer.newLine();
		} catch (IOException e) {

			throw new BaseException(e);
		}
	}

	private Map<String, List<OrderTO>> getOrderInfo(String retailerID, Date startDate, Date endDate)
			throws BaseException {
		Map orderMap = new HashMap();

		File orderFolder = new File(Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH));

		File[] orderList = orderFolder.listFiles();
		if (orderList != null) {
			for (int i = 0; i < orderList.length; i++) {

				File orderFile = orderList[i];
				try {
					String fileName = orderFile.getName();
					getLog().info("订单文件名: " + orderFile.getName());

					String orderDateStr = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.indexOf("."));

					Date orderDate = DateUtil.toDate(orderDateStr, "yyyyMMdd");

					orderDateStr = DateUtil.toString(orderDate);

					if (DateUtil.isInDateRange(orderDate, startDate, endDate)) {

						// Get Receiving Info
						Map<String, List> orderSingleMap = getOrderInfoFromFile(orderFile, orderDateStr);

						Utils.putSubMapToMainMap(orderMap, orderSingleMap);
					}

				} catch (Exception e) {
					errorLog.error("读取文件失败。请检查文件是否正确。");
					errorLog.error("文件名：" + orderFile.getName());
				}
			}
		}

		return orderMap;

	}

	private Map<String, List> getOrderInfoFromFile(File orderFile, String orderDateStr) throws BaseException {
		BufferedReader reader = null;
		Map<String, List> orderMap = new HashMap<String, List>();
		try {

			String fileName = orderFile.getName();
			String[] splitStr = fileName.split("_");
			String userID = splitStr[2];

			// Open the file
			FileInputStream fileInput = new FileInputStream(orderFile);
			InputStreamReader inputStrReader = new InputStreamReader(fileInput, "UTF-8");
			reader = new BufferedReader(inputStrReader);
			reader.readLine();
			// Read line by line
			String orderLine = null;
			List<OrderTO> orderTOList = new ArrayList<OrderTO>();
			while ((orderLine = reader.readLine()) != null) {
				this.getLog().info("订单内容： " + orderLine);
				OrderTO orderTO = new OrderTO(orderLine);
				orderTO.setUserID(userID);
				orderTOList.add(orderTO);

			}
			//
			log.info("订单日期: " + orderDateStr + " 包含的详单数量为:" + orderTOList.size());
			orderMap.put(orderDateStr, orderTOList);

		} catch (FileNotFoundException e) {
			log.error(e);
			throw new BaseException(e);
		} catch (IOException e) {

			log.error(e);
			throw new BaseException(e);

		} finally {

			FileUtil.closeFileReader(reader);
		}
		return orderMap;

	}

	@Override
	protected Map<String, OrderTO> getOrderInfoFromFile(String retailerID, File orderFile) throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}

}
