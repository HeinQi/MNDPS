package com.rsi.mengniu.retailer.renrenle.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import com.rsi.mengniu.retailer.hualian.service.HualianDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
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

		String fileName = salesFile.getName();
		String salesDate = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.indexOf("."));
		salesDate = DateUtil.toString(DateUtil.toDate(salesDate, "yyyyMMdd"));
		log.info("销售单生成日期：" + salesDate + "。销售开始日期：" + DateUtil.toString(startDate) + "。销售截至日期："
				+ DateUtil.toString(endDate));
		Map<String, List<SalesTO>> salesMap = new HashMap<String, List<SalesTO>>();
		try {
			InputStream sourceExcel = new FileInputStream(salesFile);

			Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);

			if (sourceWorkbook.getNumberOfSheets() != 0) {
				Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
				for (int i = 3; i < sourceSheet.getPhysicalNumberOfRows() - 1; i++) {
					Row sourceRow = sourceSheet.getRow(i);
					if (sourceRow == null) {
						continue;
					}
					SalesTO salesTO = new SalesTO();
					salesTO.setSalesDate(salesDate);
					List<SalesTO> salesTOList = null;

					for (int j = 0; j < sourceRow.getLastCellNum(); j++) {

						Cell sourceCell = sourceRow.getCell(j);

						String sourceCellValue;
						if (sourceCell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
							sourceCellValue = Double.valueOf(sourceCell.getNumericCellValue()).toString();
						} else {

							sourceCellValue = sourceCell.getStringCellValue();
						}

						switch (j) {
						case 0:
							salesTO.setStoreID(sourceCellValue);

							continue;
						case 2:
							salesTO.setItemName(sourceCellValue);

							continue;
						case 3:
							salesTO.setItemID(sourceCellValue);

							continue;
						case 6:
							salesTO.setSalesQuantity(sourceCellValue);

							continue;
						case 7:
							salesTO.setSalesAmount(sourceCellValue);

							continue;

						}

					}

					if (salesMap.containsKey(salesDate)) {
						salesTOList = salesMap.get(salesDate);
					} else {
						salesTOList = new ArrayList<SalesTO>();
						// Test the Hashmap
						salesMap.put(salesDate, salesTOList);
					}

					log.debug("收货单详细条目: " + salesTO.toString());
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
		for (String orderDateStr : orderMap.keySet()) {
			BufferedWriter writer = initOrderOutputFile(retailerID, orderDateStr);
			List<OrderTO> orderList = orderMap.get(orderDateStr);
			for (OrderTO orderTO : orderList) {
				String outputLine = orderTO.getOrderNo() + "\t" + orderTO.getStoreID() + "\t" + orderTO.getStoreName()
						+ "\t" + "" + "\t" + orderTO.getItemID() + "\t" + orderTO.getBarcode() + "\t"
						+ orderTO.getItemName() + "\t" + orderTO.getQuantity() + "\t" + orderTO.getTotalPrice() + "\t"
						+ "" + "\t" + "" + "\t" + orderTO.getUnitPrice();
				try {
					writer.write(outputLine);
					writer.newLine();
				} catch (IOException e) {
					FileUtil.closeFileWriter(writer);
					throw new BaseException(e);
				}
			}

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

			}
		}

		return orderMap;

	}

	private Map<String, List> getOrderInfoFromFile(File orderFile, String orderDateStr) throws BaseException {
		BufferedReader reader = null;
		Map<String, List> orderMap = new HashMap<String, List>();
		try {
			// Open the file
			FileInputStream fileInput = new FileInputStream(orderFile);
			InputStreamReader inputStrReader = new InputStreamReader(fileInput, "UTF-8");
			reader = new BufferedReader(inputStrReader);
			reader.readLine();
			// Read line by line
			String orderLine = null;
			List<OrderTO> orderTOList = new ArrayList<OrderTO>();
			while ((orderLine = reader.readLine()) != null) {
				OrderTO orderTO = new OrderTO(orderLine);
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
