package com.rsi.mengniu.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.AccountLogTO;
import com.rsi.mengniu.retailer.module.SalesTO;

public class AccountLogUtil {
	public static Log errorLog = LogFactory.getLog(Constants.SYS_ERROR);
	public static Log log = LogFactory.getLog(AccountLogUtil.class);
	private static Map<String, AccountLogTO> accountLogMap = new HashMap<String, AccountLogTO>();
	private static Map<String, AccountLogTO> roundLogMap;

	public static void mergeRoundLogMapToAccountLogMap() {
		Object[] roundLogKeyList = roundLogMap.keySet().toArray();
		Arrays.sort(roundLogKeyList);
		for (Object roundLog : roundLogKeyList) {
			String combineKey = (String) roundLog;
			AccountLogTO accountLogTO = roundLogMap.get(combineKey);
			if (accountLogTO.getErrorMessage() == "") {
				accountLogTO.setSuccessInd("Y");
			} else {
				accountLogTO.setSuccessInd("N");
			}

			if (accountLogTO.getProcessDateStr() == "") {
				if (accountLogMap.containsKey(combineKey)) {
					accountLogMap.get(combineKey).setErrorMessage(accountLogTO.getErrorMessage());
					accountLogMap.get(combineKey).setOrderDownloadAmount(
							accountLogTO.getOrderDownloadAmount()
									+ accountLogMap.get(combineKey).getOrderDownloadAmount());
					accountLogMap.get(combineKey).setReceivingDownloadAmount(
							accountLogTO.getReceivingDownloadAmount()
									+ accountLogMap.get(combineKey).getReceivingDownloadAmount());
					accountLogMap.get(combineKey).setSalesDownloadAmount(
							accountLogTO.getSalesDownloadAmount()
									+ accountLogMap.get(combineKey).getSalesDownloadAmount());
					accountLogMap.get(combineKey).setSuccessInd(accountLogTO.getSuccessInd());
				} else {
					accountLogMap.put(combineKey, accountLogTO);
				}
			} else {
				String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() + accountLogTO.getUserID();
				if (accountLogMap.containsKey(key)) {
					accountLogMap.remove(key);
					accountLogMap.put(combineKey, accountLogTO);
				} else {
					if (accountLogMap.containsKey(combineKey)) {
						accountLogMap.get(combineKey).setErrorMessage(accountLogTO.getErrorMessage());
						accountLogMap.get(combineKey).setOrderDownloadAmount(
								accountLogTO.getOrderDownloadAmount()
										+ accountLogMap.get(combineKey).getOrderDownloadAmount());
						accountLogMap.get(combineKey).setReceivingDownloadAmount(
								accountLogTO.getReceivingDownloadAmount()
										+ accountLogMap.get(combineKey).getReceivingDownloadAmount());
						accountLogMap.get(combineKey).setSalesDownloadAmount(
								accountLogTO.getSalesDownloadAmount()
										+ accountLogMap.get(combineKey).getSalesDownloadAmount());
						accountLogMap.get(combineKey).setSuccessInd(accountLogTO.getSuccessInd());
					} else {
						accountLogMap.put(combineKey, accountLogTO);
					}
				}
			}
		}
	}

	public static void initRoundLogMap() {
		roundLogMap = new HashMap<String, AccountLogTO>();
	}

	/**
	 * Initial Account Log
	 * 
	 * @param accountLogTO
	 */

	// 内部
	public static void addAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() + accountLogTO.getUserID()
				+ accountLogTO.getProcessDateStr();
		roundLogMap.put(key, accountLogTO);
	}

	/**
	 * Get Account Log
	 * 
	 * @param accountLogTO
	 */
	// 内部
	public static AccountLogTO getAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() + accountLogTO.getUserID()
				+ accountLogTO.getProcessDateStr();
		if (roundLogMap.containsKey(key)) {
			return roundLogMap.get(key);
		}
		return null;
	}

	/**
	 * Update Account Log
	 * 
	 * @param accountLogTO
	 */
	// public static void updateAccountLogTO(AccountLogTO accountLogTO) {
	// String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() +
	// accountLogTO.getUserID() + accountLogTO.getProcessDateStr();
	// accountLogMap.put(key, accountLogTO);
	// }

	// public static void removeAccountLogTO(AccountLogTO accountLogTO) {
	// String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() +
	// accountLogTO.getUserID() + accountLogTO.getProcessDateStr();
	// accountLogMap.remove(key);
	// }
	// 内部
	public static void removeloginAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() + accountLogTO.getUserID();
		roundLogMap.remove(key);
	}

	/**
	 * Add login failed info to account log map
	 * 
	 * @param accoutLogLoginTO
	 */
	// 登录失败
	public static void loginFailed(AccountLogTO accountLogTO) {
		removeloginAccountLogTO(accountLogTO);
		accountLogTO.setLoginInd("N");
		addAccountLogTO(accountLogTO);
	}

	// 登录成功
	public static void loginSuccess(AccountLogTO accountLogTO) {
		removeloginAccountLogTO(accountLogTO);
		accountLogTO.setLoginInd("Y");
		addAccountLogTO(accountLogTO);
	}

	// 记录下载订单成功数量
	public static void recordOrderDownloadAmount(AccountLogTO accountLogUpdateTO) {
		removeloginAccountLogTO(accountLogUpdateTO);
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setOrderDownloadAmount(accountLogUpdateTO.getOrderDownloadAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	// 记录下载收货单成功数量
	public static void recordReceivingDownloadAmount(AccountLogTO accountLogUpdateTO) {
		removeloginAccountLogTO(accountLogUpdateTO);
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setReceivingDownloadAmount(accountLogUpdateTO.getReceivingDownloadAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	// 记录下载销售单成功数量
	public static void recordSalesDownloadAmount(AccountLogTO accountLogUpdateTO) {
		removeloginAccountLogTO(accountLogUpdateTO);
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setSalesDownloadAmount(accountLogUpdateTO.getSalesDownloadAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	// 记录下载失败的ErrorMessage
	public static void failureDownload(AccountLogTO accountLogUpdateTO) {
		removeloginAccountLogTO(accountLogUpdateTO);
		AccountLogTO accountLogTO = getAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setErrorMessage(accountLogUpdateTO.getErrorMessage());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAccountLogTO(accountLogUpdateTO);
		}
	}

	// ////////////////////////////////////////////////////////////////下面为merage阶段
	// 外部merage
	public static void updateProcessedOrderInfo(Map<String, Set<String>> processedOrderMap) {
		for (String key : processedOrderMap.keySet()) {
			String[] accountInfo = key.split("--");
			String retailerID = accountInfo[0];
			String userID = accountInfo[1];
			String processDateStr = accountInfo[2];
			AccountLogTO accountLogTO = new AccountLogTO(retailerID, userID, "", processDateStr);
			accountLogTO.setOrderProcessedAmount(processedOrderMap.get(key).size());
			recordOrderProcessedAmount(accountLogTO);
		}
	}

	// 外部merage
	public static void recordSalesProcessedAmount(String retailerID, String processDateStr, List<SalesTO> salesList) {
		for (SalesTO salesTO : salesList) {
			String userID = salesTO.getUserID();
			String agency = salesTO.getAgency();
			AccountLogTO accountLogTO = new AccountLogTO(retailerID, userID, "", processDateStr, "", "", agency, "", "");
			accountLogTO.setSalesProcessedAmount(1);
			recordSalesProcessedAmount(accountLogTO);
		}
	}

	// 外部merage1
	public static void recordOrderProcessedAmount(AccountLogTO accountLogUpdateTO) {
		removeloginAccountLogTO(accountLogUpdateTO);
		AccountLogTO accountLogTO = getAllAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setOrderProcessedAmount(accountLogTO.getOrderProcessedAmount()
					+ accountLogUpdateTO.getOrderProcessedAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAllAccountLogTO(accountLogUpdateTO);
		}
	}

	// 外部merage1
	public static void recordSalesProcessedAmount(AccountLogTO accountLogUpdateTO) {
		removeloginAccountLogTO(accountLogUpdateTO);
		AccountLogTO accountLogTO = getAllAccountLogTO(accountLogUpdateTO);
		if (accountLogTO != null) {
			accountLogTO.setSalesProcessedAmount(accountLogTO.getSalesProcessedAmount()
					+ accountLogUpdateTO.getSalesProcessedAmount());
		} else {
			accountLogUpdateTO.setLoginInd("Y");
			addAllAccountLogTO(accountLogUpdateTO);
		}
	}

	/**
	 * Initial Account Log
	 * 
	 * @param accountLogTO
	 */

	// 外部merage2
	public static void addAllAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() + accountLogTO.getUserID()
				+ accountLogTO.getProcessDateStr();
		accountLogMap.put(key, accountLogTO);
	}

	/**
	 * Get Account Log
	 * 
	 * @param accountLogTO
	 */
	// 外部merage2
	public static AccountLogTO getAllAccountLogTO(AccountLogTO accountLogTO) {
		String key = accountLogTO.getRetailerID() + accountLogTO.getAgency() + accountLogTO.getUserID()
				+ accountLogTO.getProcessDateStr();
		if (accountLogMap.containsKey(key)) {
			return accountLogMap.get(key);
		}
		return null;
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
		fileName = fileName + processTimeStr + ".xls";
		String fileFullPath = filePath + fileName;
		FileUtil.initExcelXLS(filePath, fileName);
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
		for (int i = 0; i < accountLogKeyList.length; i++, lastRowNo++) {
			String accountLogKey = (String) accountLogKeyList[i];

			AccountLogTO accountLogTO = accountLogMap.get(accountLogKey);
			HSSFRow row = sheet.createRow(lastRowNo + 1);
			row.createCell(0).setCellValue(accountLogTO.getRetailerID());
			row.createCell(1).setCellValue(accountLogTO.getUserID());
			row.createCell(2).setCellValue(accountLogTO.getPassword());
			row.createCell(3).setCellValue(accountLogTO.getUrl());
			row.createCell(4).setCellValue(accountLogTO.getDistrict());
			row.createCell(5).setCellValue(accountLogTO.getAgency());
			row.createCell(6).setCellValue(accountLogTO.getLoginNm());
			row.createCell(7).setCellValue(accountLogTO.getStoreNo());
			row.createCell(8).setCellValue(accountLogTO.getLoginInd());
			row.createCell(9).setCellValue(accountLogTO.getProcessDateStr());
			row.createCell(10).setCellValue(accountLogTO.getOrderDownloadAmount());
			row.createCell(11).setCellValue(accountLogTO.getReceivingDownloadAmount());
			row.createCell(12).setCellValue(accountLogTO.getOrderProcessedAmount());
			row.createCell(13).setCellValue(accountLogTO.getSalesDownloadAmount());
			row.createCell(14).setCellValue(accountLogTO.getSalesProcessedAmount());
			row.createCell(15).setCellValue(accountLogTO.getSuccessInd());
			if (accountLogTO.getSuccessInd() == "N") {
				row.createCell(16).setCellValue(accountLogTO.getErrorMessage());
			}
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
}
