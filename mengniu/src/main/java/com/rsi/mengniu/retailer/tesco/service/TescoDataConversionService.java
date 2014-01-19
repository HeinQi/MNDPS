package com.rsi.mengniu.retailer.tesco.service;

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
import java.util.Map.Entry;
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
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class TescoDataConversionService extends RetailerDataConversionService {

	private Log log = LogFactory.getLog(TescoDataConversionService.class);

	@Override
	protected String getRetailerID() {

		return Constants.RETAILER_TESCO;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			String retailerID, Date startDate, Date endDate, File receivingFile)
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
								storeID = sourceCellValue.substring(0, sourceCellValue.indexOf("."));
							}
							receivingNoteTO.setStoreID(storeID);
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
							receivingNoteTO.setItemID(sourceCellValue);
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

	@Override
	protected Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException {
		Map<String, OrderTO> orderTOMap = new HashMap<String, OrderTO>();
		for (String orderNo : orderNoSet) {
			// Get order info map
			// Key: Store ID + Item Code
			Map<String, OrderTO> orderMap = null;
			orderMap = getOrderInfo(orderNo);
			log.info("读取订单信息. 订单号:" + orderNo);
			orderTOMap.putAll(orderMap);

			log.info("读取订单信息结束. 订单号:" + orderNo);
		}

		return orderTOMap;
	}
	
	private Map<String, OrderTO> getOrderInfo(String orderNo)
			throws BaseException {
		String fileName = Utils.getProperty(Constants.RETAILER_TESCO
				+ Constants.ORDER_PATH)
				+ "Order_" + Constants.RETAILER_TESCO + "_" + orderNo + ".txt";
		File orderFile = new File(fileName);

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
					String key = orderTO.getOrderNo() + orderTO.getStoreName()
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

			log.info("订单: " + orderNo + " 包含的详单数量为:" + orderMap.size());

		}
		return orderMap;
	}

	@Override
	protected Log getLog() {
		return log;
	}

}
