package com.rsi.mengniu;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.rsi.mengniu.retailer.module.User;

public class DataPullTaskPool {
	private static Log log = LogFactory.getLog(DataPullTaskPool.class);
	private static List<List<User>> taskPool = new ArrayList<List<User>>();
	private static int a = 0;
	private static int retailerCount = 0;

	public static void initTaskPool(String retailer) throws IOException {
		log.info("Read User name and Password from User.xls");
		if (DataPullTaskPool.class.getResource("/User.xls") == null) {
			throw new IOException("The User Configuration File does not exist!");
		}
		InputStream is = DataPullTaskPool.class.getResourceAsStream("/User.xls");
		HSSFWorkbook workbook = new HSSFWorkbook(is);
		if ("ALL".equals(retailer)) {
			retailerCount = workbook.getNumberOfSheets();
			for (int sheetIndex = 0; sheetIndex < retailerCount; sheetIndex++) {
				HSSFSheet sheet = workbook.getSheetAt(sheetIndex);
				taskPool.add(getUsersFormSheet(sheet));
			}
		} else {
			retailerCount = 1;
			HSSFSheet sheet = workbook.getSheet(retailer);
			taskPool.add(getUsersFormSheet(sheet));
		}
	}

	private static List<User> getUsersFormSheet(HSSFSheet sheet) {
		List<User> userList = new ArrayList<User>();
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			HSSFRow row = sheet.getRow(rowIndex);
			if (row == null) {
				break;
			}
			if (!"Y".equalsIgnoreCase(getCellValueAsStr(row.getCell(2)))) {
				continue;
			}
			User user = new User();
			user.setUserId(getCellValueAsStr(row.getCell(0)));
			user.setPassword(getCellValueAsStr(row.getCell(1)));
			user.setRetailer(sheet.getSheetName());
			log.debug(user);
			userList.add(user);
		}
		return userList;
	}

	private static String getCellValueAsStr(HSSFCell cell) {
		HSSFDataFormatter format = new HSSFDataFormatter();
		return format.formatCellValue(cell).trim();
	}

	public static synchronized User getTask() {
		List<User> userList = (List<User>) taskPool.get(a % retailerCount);
		while (userList.size() == 0 && retailerCount > 1) {
			taskPool.remove(a % retailerCount);
			retailerCount--;
			userList = (List<User>) taskPool.get(a % retailerCount);
		}
		a++;
		int userSize = userList.size();
		if (userSize > 0) {
			return userList.remove(userList.size() - 1);
		}
		return null;
	}

}
