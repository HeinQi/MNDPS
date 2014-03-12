package com.rsi.mengniu.retailer.carrefour.service;

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
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class CarrefourDataConversionService extends RetailerDataConversionService {

	private Log log = LogFactory.getLog(CarrefourDataConversionService.class);
	private Log summaryLog = LogFactory.getLog(Constants.SUMMARY_CARREFOUR);

	@Override
	protected Log getSummaryLog() {
		return summaryLog;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(String retailerID, Date startDate,
			Date endDate, File receivingFile) throws BaseException {
		Map<String, List<ReceivingNoteTO>> receivingNoteMap = new HashMap<String, List<ReceivingNoteTO>>();
		String fileName = receivingFile.getName();
		String[] splitStr = fileName.split("_");
		String userID = splitStr[2];
		try {
			InputStream sourceExcel = new FileInputStream(receivingFile);

			Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);
			if (sourceWorkbook.getNumberOfSheets() != 0) {
				Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
				for (int i = 1; i <= sourceSheet.getPhysicalNumberOfRows(); i++) {
					Row sourceRow = sourceSheet.getRow(i);
					if (sourceRow == null) {
						continue;
					}

					String receivingDateStr = sourceRow.getCell(6).getStringCellValue();
					Date receivingDate = DateUtil.toDate(receivingDateStr);

					// If receivingDate is in the date range
					if (DateUtil.isInDateRange(receivingDate, startDate, endDate)) {

						ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO();
						receivingNoteTO.setUserID(userID);
						String orderNo = null;
						List<ReceivingNoteTO> receivingNoteTOList = null;

						for (int j = 0; j < sourceRow.getLastCellNum(); j++) {

							Cell sourceCell = sourceRow.getCell(j);

							String sourceCellValue = sourceCell.getStringCellValue();

							switch (j) {
							case 2:
								//Get store ID by receiving store number
								String storeID = Utils.getCarrefourStoreIDByReceivingStoreID(sourceCellValue);
								receivingNoteTO.setStoreID(storeID);
								receivingNoteTO.setReceivingStoreNo(sourceCellValue);
								continue;
							case 3:

								//sourceCellValue = sourceCellValue.substring(sourceCellValue.indexOf("-") + 1);
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
								receivingNoteTO.setItemID(sourceCellValue);

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

	@Override
	protected String getRetailerID() {

		return Constants.RETAILER_CARREFOUR;
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

	// private Map<String, OrderTO> getOrderInfo(String orderNo) throws
	// BaseException {
	// String fileName = Utils.getProperty(Constants.RETAILER_CARREFOUR +
	// Constants.ORDER_INBOUND_PATH) + "Order_"
	// + Constants.RETAILER_CARREFOUR + "_" + orderNo + ".txt";
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
	// String key = orderTO.getOrderNo() + orderTO.getStoreName().substring(3) +
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Map<String, OrderTO> getOrderInfoFromFile(String retailerID, File orderFile) throws BaseException {
		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
		String fileName = orderFile.getName();
		String[] splitStr = fileName.split("_");
		String userID = splitStr[2];
		if (orderFile.exists()) {
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
					String orderStoreName = orderTO.getStoreName();
					String storeID = Utils.getCarrefourStoreIDByOrderStoreName(orderStoreName);
					String key = orderTO.getOrderNo() + storeID + orderTO.getItemID();
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

	/**
	 * Export the failed receiving data to file
	 * @param retailerID
	 * @param receivingDate
	 * @param failedReceivingList
	 * @param failedCount
	 * @throws BaseException
	 */
	public void exportFailedReceiving(String retailerID, Date receivingDate, List<ReceivingNoteTO> failedReceivingList,
			int failedCount) throws BaseException {
		if (failedCount != 0) {
			Utils.exportFailedReceivingToTXTForCarrefour(retailerID, receivingDate, failedReceivingList);
		}
	}
}
