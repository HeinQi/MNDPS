package com.rsi.mengniu.retailer.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
import org.xml.sax.SAXException;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.util.FileUtil;
import com.rsi.mengniu.util.Utils;

public class YonghuiDataConversionService extends RetailerDataConversionService {

	private Log log = LogFactory.getLog(YonghuiDataConversionService.class);
	
	@Override
	protected String getRetailerID() {
		
		return Constants.RETAILER_YONGHUI;
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
				reader = new BufferedReader(new FileReader(receivingFile));
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

	@Override
	protected Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException {
		File receivingInboundFolder = new File(
				Utils.getProperty(Constants.RETAILER_YONGHUI
						+ Constants.ORDER_PATH));

		File[] orderList = receivingInboundFolder.listFiles();

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
		for (int i = 0; i < orderList.length; i++) {

			File orderFile = orderList[i];
			orderMap.putAll(getOrderInfoForYonghui(orderFile));
			log.info("订单文件名: " + orderFile.getName());

		}

		return orderMap;
	}
	private Map<String, OrderTO> getOrderInfoForYonghui(File orderFile)
			throws BaseException {
		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();

		ExcelReader excelReader = null;
		try {
			excelReader = new ExcelReader();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nl.fountain.xelem.excel.Workbook wb = null;
		try {
			wb = excelReader.getWorkbook(orderFile.getPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		nl.fountain.xelem.excel.Worksheet sourceSheet = wb.getWorksheetAt(0);

		for (int i = 2; i <= sourceSheet.lastRow; i++) {
			nl.fountain.xelem.excel.Row sourceRow = sourceSheet.getRowAt(i);
			if (sourceRow == null) {
				continue;
			}

			OrderTO orderTO = new OrderTO();
			String orderNo = null;

			for (int j = 1; j <= sourceRow.maxCellIndex(); j++) {

				nl.fountain.xelem.excel.Cell sourceCell = sourceRow
						.getCellAt(j);

				switch (j) {
				case 2:
					orderNo = sourceCell.getData$();
					orderTO.setOrderNo(orderNo);

					continue;
				case 5:
					String storeID = sourceCell.getData$();
					orderTO.setStoreNo(storeID);

					continue;
				case 6:

					String storeName = sourceCell.getData$();
					orderTO.setStoreName(storeName);
					continue;
				case 7:
					String orderDate = sourceCell.getData$();
					orderTO.setOrderDate(orderDate);

					continue;
				case 9:
					String itemCode = sourceCell.getData$();
					orderTO.setItemCode(itemCode);

					continue;
				case 10:
					String barcode = sourceCell.getData$();
					orderTO.setBarcode(barcode);

					continue;
				case 11:

					String itemName = sourceCell.getData$();
					orderTO.setItemName(itemName);

					continue;
				case 13:
					String quantity = sourceCell.getData$();
					orderTO.setQuantity(quantity);

					continue;
				}

			}

			String key = orderTO.getOrderNo() + orderTO.getStoreName()
					+ orderTO.getItemCode();
			orderMap.put(key, orderTO);

		}

		return orderMap;
	}
}
