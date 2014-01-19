package com.rsi.mengniu.retailer.renrenle.service;

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
import com.rsi.mengniu.retailer.hualian.service.HualianDataConversionService;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.util.DateUtil;
import com.rsi.mengniu.util.FileUtil;

public class RenrenleDataConversionService extends
		RetailerDataConversionService {

	private Log log = LogFactory.getLog(RenrenleDataConversionService.class);

	@Override
	protected String getRetailerID() {
		return Constants.RETAILER_RENRENLE;
	}

	@Override
	protected Log getLog() {
		return log;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			String retailerID, Date startDate, Date endDate, File receivingFile)
			throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(
			String retailerID, Date startDate, Date endDate, File salesFile)
			throws BaseException {

		String fileName = salesFile.getName();
		String salesDate = fileName.substring(fileName.lastIndexOf("_") + 1,
				fileName.indexOf("."));
		salesDate = DateUtil.toString(DateUtil.toDate(salesDate, "yyyyMMdd"));
		log.info("销售单生成日期：" + salesDate + "。销售开始日期："
				+ DateUtil.toString(startDate) + "。销售截至日期："
				+ DateUtil.toString(endDate));
		Map<String, List<SalesTO>> salesMap = new HashMap<String, List<SalesTO>>();
		try {
			InputStream sourceExcel = new FileInputStream(salesFile);

			Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);

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
						sourceCellValue = Double.valueOf(
								sourceCell.getNumericCellValue()).toString();
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
