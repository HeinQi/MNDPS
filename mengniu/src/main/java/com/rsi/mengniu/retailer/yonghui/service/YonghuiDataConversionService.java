package com.rsi.mengniu.retailer.yonghui.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import nl.fountain.xelem.lex.ExcelReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.xml.sax.SAXException;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.common.service.RetailerDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class YonghuiDataConversionService extends RetailerDataConversionService {

	private Log log = LogFactory.getLog(YonghuiDataConversionService.class);
	private Log summaryLog = LogFactory.getLog(Constants.SUMMARY_YONGHUI);

	@Override
	protected Log getSummaryLog() {
		return summaryLog;
	}

	@Override
	protected String getRetailerID() {

		return Constants.RETAILER_YONGHUI;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(String retailerID, Date startDate,
			Date endDate, File receivingFile) throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();

		if (receivingFile.exists()) {

			String fileName = receivingFile.getName();
			String[] splitStr = fileName.split("_");
			String userID = splitStr[2];
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
					receivingNoteTO.setUserID(userID);
					
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

			log.info("收货单: " + receivingFile.getName() + " 包含的详单数量为:" + receivingNoteMap.size());

		}
		return receivingNoteMap;
	}

	// @Override
	// protected Map<String, OrderTO> getOrderInfo(String retailerID,
	// Set<String> orderNoSet) throws BaseException {
	// File orderInboundFolder = new File(
	// Utils.getProperty(Constants.RETAILER_YONGHUI
	// + Constants.ORDER_INBOUND_PATH));
	//
	// File[] orderList = orderInboundFolder.listFiles();
	//
	// Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
	// for (int i = 0; i < orderList.length; i++) {
	//
	// File orderFile = orderList[i];
	// orderMap.putAll(getOrderInfoForYonghui(orderFile));
	// log.info("订单文件名: " + orderFile.getName());
	//
	// }
	//
	// return orderMap;
	// }

	@Override
	protected Map<String, OrderTO> getOrderInfoFromFile(String retailerID, File orderFile) throws BaseException {
		return this.getOrderInfoForYonghui(orderFile);
	}

	private Map<String, OrderTO> getOrderInfoForYonghui(File orderFile) throws BaseException {

		List<OrderTO> orderTOList = Utils.getOrderTOListFromFileForYonghui(orderFile);

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();

		for (OrderTO orderTO : orderTOList) {
			String key = orderTO.getOrderNo() + orderTO.getStoreID() + orderTO.getItemID();
			orderMap.put(key, orderTO);
		}

		return orderMap;
	}

	@Override
	protected Log getLog() {
		return log;
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
			String storeID = receivingNoteByStoreTO.getStoreID();
			String key = receivingNoteByStoreTO.getOrderNo() + storeID + receivingNoteByStoreTO.getItemID();
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
