package com.rsi.mengniu.retailer.hualian.service;

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

public class HualianDataConversionService extends RetailerDataConversionService {
	private Log log = LogFactory.getLog(HualianDataConversionService.class);

	@Override
	protected String getRetailerID() {

		return Constants.RETAILER_HUALIAN;
	}

	@Override
	protected Log getLog() {
		return log;
	}

	@Override
	protected Map<String, List<ReceivingNoteTO>> getReceivingInfoFromFile(
			String retailerID, Date startDate, Date endDate, File receivingFile)
			throws BaseException {
		return null;
	}


	@Override
	protected Map<String, List<SalesTO>> getSalesInfoFromFile(
			String retailerID, Date startDate, Date endDate, File salesFile)
			throws BaseException {
		String fileName = salesFile.getName();
		// String salesDate =
		// fileName.substring(fileName.lastIndexOf("_")+1,fileName.indexOf("."));
		// salesDate = DateUtil.toString(DateUtil.toDate(salesDate,"yyyyMMdd"));
		// log.info("销售单生成日期："+ salesDate +"。销售开始日期：" +
		// DateUtil.toString(startDate) +"。销售截至日期："+
		// DateUtil.toString(endDate));
		Map<String, List<SalesTO>> salesMap = new HashMap<String, List<SalesTO>>();

		if (salesFile.exists()) {
			BufferedReader reader = null;
			try {
				// Open the file
				FileInputStream fileInput = new FileInputStream(salesFile);
				InputStreamReader inputStrReader = new InputStreamReader(
						fileInput, "UTF-8");
				reader = new BufferedReader(inputStrReader);
				reader.readLine();
				// Read line by line
				String salesLine = null;
				while ((salesLine = reader.readLine()) != null) {
					SalesTO salesTO = new SalesTO(salesLine);
					String salesDateStr = salesTO.getSalesDate();
					Date salesDate = DateUtil.toDate(salesDateStr);
					if (DateUtil.isInDateRange(salesDate, startDate, endDate)) {

						List<SalesTO> salesTOList = null;
						if (salesMap.containsKey(salesDateStr)) {
							salesTOList = salesMap.get(salesDateStr);
						} else {
							salesTOList = new ArrayList<SalesTO>();
							// Test the Hashmap
							salesMap.put(salesDateStr, salesTOList);
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

			} finally {

				FileUtil.closeFileReader(reader);
			}

			log.info("收货单: " + salesFile.getName() + " 包含的详单数量为:"
					+ salesMap.size());

		}
		return salesMap;
	}

	@Override
	protected Map<String, OrderTO> getOrderInfo(String retailerID,
			Set<String> orderNoSet) throws BaseException {
		// TODO Auto-generated method stub
		return null;
	}

}
