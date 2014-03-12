package com.rsi.mengniu.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.AccountLogTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;

public class AccountLogUtil {
	public static Log errorLog = LogFactory.getLog(Constants.SYS_ERROR);
	public static Log log = LogFactory.getLog(AccountLogUtil.class);
	private static Map<String, AccountLogTO> accountLogMap = new HashMap<String, AccountLogTO>();

	/**
	 * Initial Account Log
	 * 
	 * @param accountLogTO
	 */
	public static void addAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getUserID() + accountLogTO.getProcessDateStr();
		accountLogMap.put(key, accountLogTO);
	}

	/**
	 * Get Account Log
	 * 
	 * @param accountLogTO
	 */
	public static AccountLogTO getAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getUserID() + accountLogTO.getProcessDateStr();
		if (accountLogMap.containsKey(key)) {
			return accountLogMap.get(key);
		}
		return null;
	}

	/**
	 * Update Account Log
	 * 
	 * @param accountLogTO
	 */
	public static void updateAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getUserID() + accountLogTO.getProcessDateStr();
		accountLogMap.put(key, accountLogTO);
	}

	public static void removeAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getUserID() + accountLogTO.getProcessDateStr();
		accountLogMap.remove(key);
	}

	/**
	 * Write Account Log to File
	 * 
	 * @param accountLogTO
	 * @throws BaseException
	 */
	public static void writeAccountLogToFile() throws BaseException {
		if (accountLogMap.size() != 0) {
			writeAccountLogToExcel();

		}
	}

	private static void writeAccountLogToExcel() throws BaseException {
		String filePath = Utils.getProperty(Constants.ACCOUNT_LOG_PATH);
		String fileName = Utils.getProperty(Constants.ACCOUNT_LOG_FILENAME);
		String fieldNames = Utils.getProperty(Constants.ACCOUNT_LOG_FIELDNAME);
		String[] fieldNameList = fieldNames.split(",");
		String processTimeStr = DateUtil.toString(new Date(), "yyyyMMdd-HHmmss");
		fileName = fileName+ processTimeStr + ".xls";
		String fileFullPath = filePath + fileName;
		FileUtil.initExcelXLS(filePath,fileName);

		FileUtil.initExcelHeader(fileFullPath, fieldNameList);
		FileOutputStream fileOut = null;
		File excelFile = new File(fileFullPath);
		HSSFWorkbook workbook;
		try {
			workbook = new HSSFWorkbook(new FileInputStream(fileFullPath));
		} catch (FileNotFoundException e) {
			throw new BaseException(e);
		} catch (IOException e) {
			throw new BaseException(e);
		}
		HSSFSheet sheet = workbook.getSheetAt(0);
		int lastRowNo = sheet.getLastRowNum();
		Object[] accountLogKeyList = accountLogMap.keySet().toArray();

		Arrays.sort(accountLogKeyList);

		// Iterator Receiving Map by Date
		for (int i = 0; i < accountLogKeyList.length; i++,lastRowNo++) {
			String accountLogKey = (String) accountLogKeyList[i];

			AccountLogTO accountLogTO = accountLogMap.get(accountLogKey);
			HSSFRow row = sheet.createRow(lastRowNo + 1);
			row.createCell(0).setCellValue(accountLogTO.getRetailerID());
			row.createCell(1).setCellValue(accountLogTO.getUserID());
			row.createCell(2).setCellValue(accountLogTO.getPassword());
			row.createCell(3).setCellValue(accountLogTO.getLoginInd());
			row.createCell(4).setCellValue(accountLogTO.getProcessDateStr());
			row.createCell(5).setCellValue(accountLogTO.getOrderDownloadAmount());
			row.createCell(6).setCellValue(accountLogTO.getReceivingDownloadAmount());
			row.createCell(7).setCellValue(accountLogTO.getOrderProcessedAmount());
			row.createCell(8).setCellValue(accountLogTO.getSalesDownloadAmount());
			row.createCell(9).setCellValue(accountLogTO.getSalesProcessedAmount());
			row.createCell(10).setCellValue(accountLogTO.getSuccessInd());
			
		}
		try {
			fileOut = new FileOutputStream(excelFile);
			workbook.write(fileOut);
			fileOut.close();
		} catch (IOException e) {
			throw new BaseException(e);
		} finally {
			try {
				if (fileOut != null) {
					fileOut.close();
				}
			} catch (IOException e) {
				throw new BaseException(e);
			}
		}
	}

	/**
	 * Add login failed info to account log map
	 * 
	 * @param accoutLogLoginTO
	 */
	public static void loginFailed(AccountLogTO accountLogTO) {
		accountLogTO.setLoginInd("N");
		accountLogTO.setSuccessInd("N");
		addAccountLogTO(accountLogTO);

	}

	public static void loginSuccess(AccountLogTO accountLogTO) {
		removeAccountLogTO(accountLogTO);

	}

	public static void recordOrderDownloadAmount(AccountLogTO accountLogUpdateTO) {
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setOrderDownloadAmount(accountLogTO.getOrderDownloadAmount()
					+ accountLogUpdateTO.getOrderDownloadAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	public static void recordReceivingDownloadAmount(AccountLogTO accountLogUpdateTO) {
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setReceivingDownloadAmount(accountLogTO.getReceivingDownloadAmount()
					+ accountLogUpdateTO.getReceivingDownloadAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	public static void recordOrderProcessedAmount(AccountLogTO accountLogUpdateTO) {
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setOrderProcessedAmount(accountLogTO.getOrderProcessedAmount()
					+ accountLogUpdateTO.getOrderProcessedAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	public static void recordSalesDownloadAmount(AccountLogTO accountLogUpdateTO) {
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setSalesDownloadAmount(accountLogTO.getSalesDownloadAmount()
					+ accountLogUpdateTO.getSalesDownloadAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	public static void recordSalesProcessedAmount(AccountLogTO accountLogUpdateTO) {
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setSalesProcessedAmount(accountLogTO.getSalesProcessedAmount()
					+ accountLogUpdateTO.getSalesProcessedAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	public static Map<String, Set<String>> getReceivingAmountFromFileForCarrefour(String fileFullPath, Date startDate, Date endDate)
			throws BaseException {
		Map<String,Set<String>> receivingMapByDate = new HashMap<String,Set<String>>();
		try {
			InputStream sourceExcel = new FileInputStream(fileFullPath);

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
						Set<String> orderNoSet = new HashSet<String>();
						if(receivingMapByDate.containsKey(receivingDateStr)){
							orderNoSet = receivingMapByDate.get(receivingDateStr);
						}
						String orderNo = sourceRow.getCell(7).getStringCellValue();
						orderNoSet.add(orderNo);
						receivingMapByDate.put(receivingDateStr, orderNoSet);
					}
				}
			}
			return receivingMapByDate;
		} catch (FileNotFoundException e) {
			log.error(e);
			throw new BaseException(e);
		} catch (IOException e) {
			log.error(e);
			throw new BaseException(e);
		}
	}

	public static void updateProcessedOrderInfo(Map<String, Set<String>> processedOrderMap) {

		for(String key:processedOrderMap.keySet()){
			String[] accountInfo = key.split("--");
			String retailerID = accountInfo[0];
			String userID = accountInfo[1];

			String processDateStr = accountInfo[2];
			AccountLogTO accountLogTO = new AccountLogTO(retailerID, userID, "",processDateStr);
			accountLogTO.setOrderProcessedAmount(processedOrderMap.get(key).size());
			recordOrderProcessedAmount(accountLogTO);
			
		}
		
	}

	public static void recordSalesProcessedAmount(String retailerID, String processDateStr, List<SalesTO> salesList) {
		for(SalesTO salesTO:salesList){
			String userID = salesTO.getUserID();
			AccountLogTO accountLogTO = new AccountLogTO(retailerID,userID,"",processDateStr);
			accountLogTO.setSalesProcessedAmount(1);
			recordSalesProcessedAmount(accountLogTO);
		}
		
	}
}
