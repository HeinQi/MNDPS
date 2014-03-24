package com.rsi.mengniu.util;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.xml.parsers.ParserConfigurationException;

import nl.fountain.xelem.lex.ExcelReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.DataPullTaskPool;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.AccountLogTO;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.RainbowReceivingTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.retailer.module.User;

public class Utils {
	private static Properties properties;
	private static Log errorLog = LogFactory.getLog(Constants.SYS_ERROR);
	private static Log log = LogFactory.getLog(Utils.class);
	private static Map<String, String> rainbowStoreMap = new HashMap<String, String>();
	private static Map<String, String> carrefourOrderStoreMap = new HashMap<String, String>();
	private static Map<String, String> carrefourReceivingStoreMap = new HashMap<String, String>();

	public static void main(String[] args) throws BaseException {
	}

	public void setProperties(Properties p) {
		properties = p;
	}

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}

	public static CloseableHttpClient createHttpClient(String retailerID) {
		int timeout = Integer.parseInt(getProperty(retailerID+"."+Constants.CONNECTION_TIMEOUT_TIME));
		RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY)
				.setConnectionRequestTimeout(timeout).setConnectTimeout(timeout).build();
		CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(globalConfig).build();
		return httpClient;
	}

	public static Date getStartDate(String retailerId) {
		if ("N".equalsIgnoreCase(getProperty(retailerId + ".daterange.enable"))) {
			return DateUtil.getDateAfter(new Date(), -1);
		} else {
			try {
				return DateUtil.toDate(getProperty(retailerId + ".daterange.startDate"));
			} catch (BaseException e) {
				errorLog.error("日期转换错误", e);
				return null;
			}
		}
	}

	public static Date getEndDate(String retailerId) {
		if ("N".equalsIgnoreCase(getProperty(retailerId + ".daterange.enable"))) {
			return DateUtil.getDateAfter(new Date(), -1);
		} else {
			try {
				return DateUtil.toDate(getProperty(retailerId + ".daterange.endDate"));
			} catch (BaseException e) {
				errorLog.error("日期转换错误", e);
				return null;
			}
		}
	}

	public static int getSleepTime(String retailerId) {
		return Integer.parseInt(getProperty(retailerId + ".sleep.time"));
	}

	public static String getTrace(Throwable t) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer = stringWriter.getBuffer();
		return buffer.toString();
	}

	public static String HttpExecute(CloseableHttpClient httpClient, HttpUriRequest request, String expectStr)
			throws Exception {
		CloseableHttpResponse response = httpClient.execute(request);
		HttpEntity entity = response.getEntity();
		String responseStr = EntityUtils.toString(entity);
		response.close();
		if (!StringUtils.isEmpty(expectStr) && !StringUtils.isEmpty(responseStr)) {
			if (responseStr.contains(expectStr)) {
				return responseStr;
			} else {
				throw new Exception("页面加载失败!");
			}
		} else {
			return responseStr;
		}
	}

	public static void binaryImage(String imageName) throws IOException {
		File file = new File(imageName + ".jpg");
		BufferedImage image = ImageIO.read(file);
		int width = image.getWidth();
		int height = image.getHeight();
		BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int rgb = image.getRGB(i, j);
				grayImage.setRGB(i, j, rgb);
			}
		}
		File newFile = new File(imageName + ".png");
		ImageIO.write(grayImage, "png", newFile);
	}

	public static String trimPrefixZero(String sourceStr) {
		int indexNo = sourceStr.indexOf("0");
		if (indexNo == 0) {
			sourceStr = sourceStr.substring(indexNo + 1);
			sourceStr = trimPrefixZero(sourceStr);

		}

		return sourceStr;
	}

	public static String getRainbowStoreIDByName(String storeName) throws BaseException {
		Map<String, String> rainbowStoreMapping = getRainbowStoreMapping();
		String storeID = rainbowStoreMapping.get(storeName);
		return storeID == null ? "" : storeID;
	}

	public static Map<String, String> getRainbowStoreMapping() throws BaseException {
		if (rainbowStoreMap.size() != 0)
			return rainbowStoreMap;
		InputStream sourceExcel = DataPullTaskPool.class.getResourceAsStream("/data/Rainbow_store_mapping.xls");
		Workbook sourceWorkbook;
		try {
			sourceWorkbook = new HSSFWorkbook(sourceExcel);
		} catch (IOException e) {
			throw new BaseException(e);
		}

		Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
		log.info("Total Rows of Store Mapping: " + sourceSheet.getPhysicalNumberOfRows());

		for (int i = 1; i <= sourceSheet.getPhysicalNumberOfRows(); i++) {
			Row sourceRow = sourceSheet.getRow(i);
			if (sourceRow == null) {
				continue;
			}
			Cell sourceCell = sourceRow.getCell(0);
			int cellType = sourceCell.getCellType();
			String sourceCellValue;

			if (cellType == Cell.CELL_TYPE_NUMERIC) {
				sourceCellValue = Double.valueOf(sourceCell.getNumericCellValue()).toString();
			} else {

				sourceCellValue = sourceCell.getStringCellValue();
			}

			String storeID = sourceCellValue;
			String storeName = sourceRow.getCell(1).getStringCellValue();

			rainbowStoreMap.put(storeName, storeID);
		}
		return rainbowStoreMap;
	}

	public static String getCarrefourStoreIDByOrderStoreName(String orderStoreName) throws BaseException {
		Map<String, String> carrefourStoreMapping = getCarrefourOrderStoreMapping();
		String storeID = carrefourStoreMapping.get(orderStoreName);
		return storeID == null ? "EmptyStoreMapping" : storeID;
	}

	public static Map<String, String> getCarrefourOrderStoreMapping() throws BaseException {
		if (carrefourOrderStoreMap.size() != 0)
			return carrefourOrderStoreMap;
		InputStream sourceExcel = DataPullTaskPool.class.getResourceAsStream("/data/Carrefour_store_mapping.xls");
		Workbook sourceWorkbook;
		try {
			sourceWorkbook = new HSSFWorkbook(sourceExcel);
		} catch (IOException e) {
			throw new BaseException(e);
		}

		Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
		log.info("Total Rows of Store Mapping: " + sourceSheet.getPhysicalNumberOfRows());

		for (int i = 1; i <= sourceSheet.getPhysicalNumberOfRows(); i++) {
			Row sourceRow = sourceSheet.getRow(i);
			if (sourceRow == null) {
				continue;
			}
			Cell sourceCell = sourceRow.getCell(1);
			int cellType = sourceCell.getCellType();
			String sourceCellValue;

			if (cellType == Cell.CELL_TYPE_NUMERIC) {
				sourceCellValue = Double.valueOf(sourceCell.getNumericCellValue()).toString();
			} else {

				sourceCellValue = sourceCell.getStringCellValue();
			}

			String storeID = sourceCellValue;
			String storeName = sourceRow.getCell(0).getStringCellValue();

			carrefourOrderStoreMap.put(storeName, storeID);
		}
		return carrefourOrderStoreMap;
	}

	public static String getCarrefourStoreIDByReceivingStoreID(String receivingStoreID) throws BaseException {
		Map<String, String> carrefourStoreMapping = getCarrefourReceivingStoreMapping();
		String storeID = carrefourStoreMapping.get(receivingStoreID);
		return storeID == null ? "EmptyStoreMapping" : storeID;
	}

	public static Map<String, String> getCarrefourReceivingStoreMapping() throws BaseException {
		if (carrefourReceivingStoreMap.size() != 0)
			return carrefourReceivingStoreMap;
		InputStream sourceExcel = DataPullTaskPool.class.getResourceAsStream("/data/Carrefour_store_mapping.xls");
		Workbook sourceWorkbook;
		try {
			sourceWorkbook = new HSSFWorkbook(sourceExcel);
		} catch (IOException e) {
			throw new BaseException(e);
		}

		Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
		log.info("Total Rows of Store Mapping: " + sourceSheet.getPhysicalNumberOfRows());

		for (int i = 1; i <= sourceSheet.getPhysicalNumberOfRows(); i++) {
			Row sourceRow = sourceSheet.getRow(i);
			if (sourceRow == null) {
				continue;
			}
			Cell sourceCell = sourceRow.getCell(1);
			int cellType = sourceCell.getCellType();
			String sourceCellValue;

			if (cellType == Cell.CELL_TYPE_NUMERIC) {
				sourceCellValue = Double.valueOf(sourceCell.getNumericCellValue()).toString();
			} else {

				sourceCellValue = sourceCell.getStringCellValue();
			}

			String storeID = sourceCellValue;
			String receivingStoreID = sourceRow.getCell(2).getStringCellValue();

			carrefourReceivingStoreMap.put(receivingStoreID, storeID);
		}
		return carrefourReceivingStoreMap;
	}

	public static void putSubMapToMainMap(Map<String, List> mainMap, Map<String, List> subMap) {
		for (Entry<String, List> entry : subMap.entrySet()) {
			String key = entry.getKey();
			List valueList = entry.getValue();
			if (mainMap.containsKey(key)) {
				mainMap.get(key).addAll(valueList);
			} else {
				mainMap.put(key, valueList);
			}
		}
	}

	/**
	 * Export Order info from list to txt file
	 * 
	 * @param retailerID
	 * @param orderID
	 * @param orderList
	 * @param userID
	 * @param orderDate
	 * @throws BaseException
	 */
	public static void exportOrderInfoToTXT(String retailerID, String userID, String orderID, Date orderDate,
			List<OrderTO> orderList) throws BaseException {

		String orderFolderPath = Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH);
		FileUtil.createFolder(orderFolderPath);
		String orderFilePath = orderFolderPath + "Order_" + retailerID + "_" + userID + "_" + orderID + "_"
				+ DateUtil.toStringYYYYMMDD(orderDate) + ".txt";

		File orderFile = new File(orderFilePath);
		BufferedWriter writer = null;
		if (!orderFile.exists()) {
			try {
				orderFile.createNewFile();
			} catch (IOException e) {
				FileUtil.closeFileWriter(writer);
				throw new BaseException(e);
			}
		}

		try {
			FileOutputStream fileOutput = new FileOutputStream(orderFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));

			String orderHeader = Utils.getProperty(Constants.ORDER_HEADER);
			writer.write(orderHeader);
			writer.newLine();
		} catch (IOException e) {
			FileUtil.closeFileWriter(writer);
			throw new BaseException();
		}

		try {
			for (int i = 0; i < orderList.size(); i++) {
				OrderTO orderTO = orderList.get(i);
				String orderRow = orderTO.toString();
				writer.write(orderRow);
				writer.newLine();
			}
		} catch (IOException e) {
			throw new BaseException(e);
		} finally {
			FileUtil.closeFileWriter(writer);
		}

	}

	/**
	 * Export Order info from list to txt file
	 * 
	 * @param retailerID
	 * @param orderID
	 * @param orderList
	 * @throws BaseException
	 */
	// public static void exportOrderInfoListToTXT(String retailerID, String
	// userID,String orderID, Date orderDate,
	// List<OrderTO> orderList) throws BaseException {
	// Map <String,List<OrderTO>> orderMap = new HashMap<String,
	// List<OrderTO>>();
	// List<OrderTO> tempOrderList = null;
	// for(OrderTO orderTO : orderList){
	// String orderNo = orderTO.getOrderNo();
	//
	// if(orderMap.containsKey(orderNo)){
	// tempOrderList = orderMap.get(orderNo);
	// } else {
	// tempOrderList = new ArrayList<OrderTO>();
	// orderMap.put(orderNo, tempOrderList);
	// }
	// tempOrderList.add(orderTO);
	// }
	// for(Entry<String, List<OrderTO>> entry:orderMap.entrySet()){
	// exportOrderInfoToTXT(retailerID,userID,orderID,orderDate,entry.getValue());
	// }
	// }

	/**
	 * Export Receiving Info from list to txt file
	 * 
	 * @param retailerID
	 * @param userID
	 * @param receivingList
	 * @throws BaseException
	 */
	public static void exportReceivingInfoToTXT(String retailerID, String userID, Date receivingDate,
			List<ReceivingNoteTO> receivingList) throws BaseException {

		String receivingInboundFolderPath = Utils.getProperty(retailerID + Constants.RECEIVING_INBOUND_PATH);
		FileUtil.createFolder(receivingInboundFolderPath);
		String receivingFilePath = receivingInboundFolderPath + "Receiving_" + retailerID + "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(receivingDate) + ".txt";

		File receivingFile = new File(receivingFilePath);

		BufferedWriter writer = null;

		try {
			receivingFile.createNewFile();
			String receivingHeader = Utils.getProperty(Constants.RECEIVING_HEADER);

			FileOutputStream fileOutput = new FileOutputStream(receivingFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
			writer.write(receivingHeader);
			writer.newLine();

			for (int i = 0; i < receivingList.size(); i++) {
				ReceivingNoteTO receivingNoteTO = receivingList.get(i);
				String receivingRow = receivingNoteTO.toString();
				writer.write(receivingRow);
				writer.newLine();
			}

		} catch (IOException e) {
			throw new BaseException(e);
		} finally {

			FileUtil.closeFileWriter(writer);

		}

	}

	/**
	 * Export Receiving Info from list to txt file
	 * 
	 * @param retailerID
	 * @param userID
	 * @param receivingList
	 * @throws BaseException
	 */
	public static void exportReceivingInfoToTXTForRainbow(String retailerID, String userID, Date receivingDate,
			List<ReceivingNoteTO> receivingList) throws BaseException {

		String receivingInboundFolderPath = getProperty(retailerID + Constants.RECEIVING_INBOUND_PATH);
		FileUtil.createFolder(receivingInboundFolderPath);
		String receivingFilePath = receivingInboundFolderPath + "Receiving_" + retailerID + "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(receivingDate) + ".txt";

		File receivingFile = new File(receivingFilePath);

		BufferedWriter writer = null;

		try {
			receivingFile.createNewFile();
			String receivingHeader = getProperty(Constants.OUTPUT_ORDER_HEADER);

			FileOutputStream fileOutput = new FileOutputStream(receivingFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
			writer.write(receivingHeader);
			writer.newLine();

			for (int i = 0; i < receivingList.size(); i++) {
				RainbowReceivingTO receivingNoteTO = (RainbowReceivingTO) receivingList.get(i);
				String receivingRow = receivingNoteTO.toString();
				writer.write(receivingRow);
				writer.newLine();
			}

		} catch (IOException e) {
			throw new BaseException(e);
		} finally {

			FileUtil.closeFileWriter(writer);

		}

	}

	/**
	 * Export Receiving Info from list to txt file
	 * 
	 * @param retailerID
	 * @param userID
	 * @param receivingList
	 * @throws BaseException
	 */
	public static void exportSalesInfoToTXT(String retailerID, String userID, Date slaesDate, List<SalesTO> salesList)
			throws BaseException {

		String salesInboundFolderPath = Utils.getProperty(retailerID + Constants.SALES_INBOUND_PATH);
		FileUtil.createFolder(salesInboundFolderPath);
		String salesFilePath = salesInboundFolderPath + "Sales_" + retailerID + "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(slaesDate) + ".txt";

		File salesFile = new File(salesFilePath);

		BufferedWriter writer = null;

		try {
			salesFile.createNewFile();
			String salesHeader = Utils.getProperty(Constants.SALES_HEADER);

			FileOutputStream fileOutput = new FileOutputStream(salesFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
			writer.write(salesHeader);
			writer.newLine();

			for (int i = 0; i < salesList.size(); i++) {
				SalesTO salesTO = (SalesTO) salesList.get(i);
				String salesRow = salesTO.toString();
				writer.write(salesRow);
				writer.newLine();
			}

		} catch (IOException e) {
			throw new BaseException(e);
		} finally {

			FileUtil.closeFileWriter(writer);

		}

	}

	public static void exportSalesInfoToTXTForHualian(String retailerID, String districtName, User user,
			Date slaesDate, List<SalesTO> salesList) throws BaseException {

		String salesInboundFolderPath = Utils.getProperty(retailerID + Constants.SALES_INBOUND_PATH);
		FileUtil.createFolder(salesInboundFolderPath);

		String userAgency = user.getAgency();
		String salesFilePath = salesInboundFolderPath + "Sales_" + retailerID + "_"
				+ (districtName.equals("") ? "" : (districtName + "_")) + (userAgency.equals("") ? "" : (userAgency + "_"))
				+ user.getUserId() + "_" + DateUtil.toStringYYYYMMDD(slaesDate) + ".txt";

		File salesFile = new File(salesFilePath);

		BufferedWriter writer = null;

		try {
			salesFile.createNewFile();
			String salesHeader = Utils.getProperty(Constants.SALES_HEADER);

			FileOutputStream fileOutput = new FileOutputStream(salesFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
			writer.write(salesHeader);
			writer.newLine();

			for (int i = 0; i < salesList.size(); i++) {
				SalesTO salesTO = (SalesTO) salesList.get(i);
				String salesRow = salesTO.toString();
				writer.write(salesRow);
				writer.newLine();
			}

		} catch (IOException e) {
			throw new BaseException(e);
		} finally {

			FileUtil.closeFileWriter(writer);

		}
		
		//记录下载数量
		AccountLogTO accountLogTO = new AccountLogTO(user.getRetailer(), user.getUserId(), user.getPassword(), DateUtil.toString(slaesDate), user.getUrl(), user.getDistrict(), user.getAgency(), user.getLoginNm(), user.getStoreNo());
		accountLogTO.setSalesDownloadAmount(salesList.size());
		AccountLogUtil.recordSalesDownloadAmount(accountLogTO);

	}

	public static boolean isSalesFileExistForHualian(String retailerID, String districtName, String agency,
			String userID, Date orderDate) {
		String folderPath = Utils.getProperty(retailerID + Constants.SALES_INBOUND_PATH);
		String txtFileName = null;
		String excelFileName = null;
		txtFileName = "Sales_" + retailerID + "_" + (districtName.equals("") ? "" : (districtName + "_"))
				+ (agency.equals("") ? "" : (agency + "_")) + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate)
				+ ".txt";
		excelFileName = "Sales_" + retailerID + "_" + (districtName.equals("") ? "" : (districtName + "_"))
				+ (agency.equals("") ? "" : (agency + "_")) + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate)
				+ ".xls";
		File txtFile = new File(folderPath + txtFileName);
		File excelFile = new File(folderPath + excelFileName);

		if (txtFile.exists() || excelFile.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static synchronized void recordIncorrectUser(User user) throws BaseException {

		String filePath = getProperty(Constants.INCORRECT_USER_PATH);
		FileUtil.createFolder(filePath);
		String fileFullPath = filePath + user.getRetailer() + ".xls";
		File excelFile = new File(fileFullPath);
		if (!excelFile.exists()) {
			// init Excel File
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet(user.getRetailer());
			HSSFRow header = sheet.createRow(0);
			header.createCell(0).setCellValue("序号");
			header.createCell(1).setCellValue("用户名");
			header.createCell(2).setCellValue("密码");
			header.createCell(3).setCellValue("登录网站");
			header.createCell(4).setCellValue("登录日期");

			FileOutputStream fileOut = null;
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

		FileOutputStream fileOut = null;

		try {
			HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(fileFullPath));
			HSSFSheet sheet = workbook.getSheet(user.getRetailer());
			int lastRowNo = sheet.getLastRowNum();
			for (int i = 1; i <= lastRowNo; i++) {

				HSSFRow row = sheet.getRow(i);
				if (row.getCell(1).getStringCellValue().equals(user.getUserId())
						&& row.getCell(3).getStringCellValue().equals(user.getUrl())) {
					return;
				}
			}
			HSSFRow row = sheet.createRow(lastRowNo + 1);
			row.createCell(0).setCellValue(String.valueOf(lastRowNo + 1));
			row.createCell(1).setCellValue(user.getUserId());
			row.createCell(2).setCellValue(user.getPassword());
			row.createCell(3).setCellValue(user.getUrl());
			row.createCell(4).setCellValue(DateUtil.toString(new Date()));

			fileOut = new FileOutputStream(excelFile);
			workbook.write(fileOut);
			fileOut.close();
		} catch (FileNotFoundException e) {
			throw new BaseException(e);
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
	 * Export the fialed receiving data to exception folder
	 * 
	 * @param retailerID
	 * @param receivingDate
	 * @param failedReceivingList
	 * @throws BaseException
	 */
	public static void exportFailedReceivingToTXT(String retailerID, Date receivingDate,
			List<ReceivingNoteTO> failedReceivingList) throws BaseException {
		String receivingExceptionFolderPath = Utils.getProperty(retailerID + Constants.RECEIVING_EXCEPTION_PATH);
		FileUtil.createFolder(receivingExceptionFolderPath);
		String receivingExceptionFilePath = receivingExceptionFolderPath + "Receiving_" + retailerID + "_"
				+ DateUtil.toStringYYYYMMDD(receivingDate) + ".txt";

		File receivingFile = new File(receivingExceptionFilePath);

		BufferedWriter writer = null;

		try {
			receivingFile.createNewFile();
			String receivingHeader = Utils.getProperty(Constants.RECEIVING_HEADER);

			FileOutputStream fileOutput = new FileOutputStream(receivingFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
			writer.write(receivingHeader);
			writer.newLine();

			for (int i = 0; i < failedReceivingList.size(); i++) {
				ReceivingNoteTO receivingNoteTO = failedReceivingList.get(i);
				String receivingRow = receivingNoteTO.toString();
				writer.write(receivingRow);
				writer.newLine();
			}

		} catch (IOException e) {
			throw new BaseException(e);
		} finally {

			FileUtil.closeFileWriter(writer);

		}

	}

	/**
	 * Export the fialed receiving data to exception folder
	 * 
	 * @param retailerID
	 * @param receivingDate
	 * @param failedReceivingList
	 * @throws BaseException
	 */
	public static void exportFailedReceivingToTXTForCarrefour(String retailerID, Date receivingDate,
			List<ReceivingNoteTO> failedReceivingList) throws BaseException {
		String receivingInboundFolderPath = Utils.getProperty(retailerID + Constants.RECEIVING_EXCEPTION_PATH);
		FileUtil.createFolder(receivingInboundFolderPath);
		String receivingFilePath = receivingInboundFolderPath + "Receiving_" + retailerID + "_"
				+ DateUtil.toStringYYYYMMDD(receivingDate) + ".txt";

		File receivingFile = new File(receivingFilePath);

		BufferedWriter writer = null;

		try {
			receivingFile.createNewFile();
			String receivingHeader = Utils.getProperty(Constants.RECEIVING_HEADER);

			FileOutputStream fileOutput = new FileOutputStream(receivingFile);
			writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
			writer.write(receivingHeader);
			writer.newLine();

			for (int i = 0; i < failedReceivingList.size(); i++) {
				ReceivingNoteTO receivingNoteTO = failedReceivingList.get(i);
				if (receivingNoteTO.getStoreID() == null || receivingNoteTO.getStoreID().equals("")) {
					receivingNoteTO.setRemarks("找不到门店ID与名称的mapping配置");
				} else {
					receivingNoteTO.setRemarks("找不到收货单对应的订单信息");
				}
				String receivingRow = receivingNoteTO.toStringForCarrefourException();
				writer.write(receivingRow);
				writer.newLine();
			}

		} catch (IOException e) {
			throw new BaseException(e);
		} finally {

			FileUtil.closeFileWriter(writer);

		}

	}

	public static boolean isOrderFileExistForCarrefour(String retailerID, String userID, Date orderDate) {
		File folder = new File(Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH));

		File[] fileList = folder.listFiles();
		if (fileList != null) {
			for (int i = 0; i < fileList.length; i++) {

				File file = fileList[i];
				String fileName = file.getName();

				if ((fileName.indexOf(userID) != -1) && (fileName.indexOf(DateUtil.toStringYYYYMMDD(orderDate)) != -1)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isOrderFileExist(String retailerID, String userID, String orderID, Date orderDate) {
		String folderPath = Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH);
		String txtFileName = null;
		String excelFileName = null;
		if (orderID != null && !orderID.equals("")) {
			txtFileName = "Order_" + retailerID + "_" + userID + "_" + orderID + "_"
					+ DateUtil.toStringYYYYMMDD(orderDate) + ".txt";
			excelFileName = "Order_" + retailerID + "_" + userID + "_" + orderID + "_"
					+ DateUtil.toStringYYYYMMDD(orderDate) + ".xls";
		} else {
			txtFileName = "Order_" + retailerID + "_" + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate) + ".txt";
			excelFileName = "Order_" + retailerID + "_" + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate) + ".xls";
		}
		File txtFile = new File(folderPath + txtFileName);
		File excelFile = new File(folderPath + excelFileName);

		if (txtFile.exists() || excelFile.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isReceivingFileExist(String retailerID, String userID, Date orderDate) {
		String folderPath = Utils.getProperty(retailerID + Constants.RECEIVING_INBOUND_PATH);
		String txtFileName = null;
		String excelFileName = null;
		txtFileName = "Receiving_" + retailerID + "_" + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate) + ".txt";
		excelFileName = "Receiving_" + retailerID + "_" + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate) + ".xls";
		File txtFile = new File(folderPath + txtFileName);
		File excelFile = new File(folderPath + excelFileName);

		if (txtFile.exists() || excelFile.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isSalesFileExist(String retailerID, String userID, Date orderDate) {
		String folderPath = Utils.getProperty(retailerID + Constants.SALES_INBOUND_PATH);
		String txtFileName = null;
		String excelFileName = null;
		txtFileName = "Sales_" + retailerID + "_" + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate) + ".txt";
		excelFileName = "Sales_" + retailerID + "_" + userID + "_" + DateUtil.toStringYYYYMMDD(orderDate) + ".xls";
		File txtFile = new File(folderPath + txtFileName);
		File excelFile = new File(folderPath + excelFileName);

		if (txtFile.exists() || excelFile.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isOrderFileExist(String retailerID, String fileName) {
		String folderPath = Utils.getProperty(retailerID + Constants.ORDER_INBOUND_PATH);

		File file = new File(folderPath + fileName);

		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isReceivingFileExist(String retailerID, String fileName) {
		String folderPath = Utils.getProperty(retailerID + Constants.RECEIVING_INBOUND_PATH);

		File file = new File(folderPath + fileName);

		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isSalesFileExist(String retailerID, String fileName) {
		String folderPath = Utils.getProperty(retailerID + Constants.SALES_INBOUND_PATH);

		File file = new File(folderPath + fileName);

		if (file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public static void exportOrderInfoListToTXT(String retailerID, String userId, Date orderDate,
			List<OrderTO> orderList) throws BaseException {
		Map<String, List<OrderTO>> orderMap = new HashMap<String, List<OrderTO>>();
		List<OrderTO> tempOrderList = null;
		for (OrderTO orderTO : orderList) {
			String orderNo = orderTO.getOrderNo();

			if (orderMap.containsKey(orderNo)) {
				tempOrderList = orderMap.get(orderNo);
			} else {
				tempOrderList = new ArrayList<OrderTO>();
				orderMap.put(orderNo, tempOrderList);
			}
			tempOrderList.add(orderTO);
		}
		for (Entry<String, List<OrderTO>> entry : orderMap.entrySet()) {
			exportOrderInfoToTXT(retailerID, userId, entry.getKey(), orderDate, entry.getValue());
		}
	}

	// public static RequestConfig getTimeoutConfig() {
	// return
	// RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();//
	// 设置请求和传输超时时间
	// }

	public static String getUrlRoot(String url) {
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
		if (url.matches("http://(.*?)/(.*?)")) {
			return url.substring(0, url.lastIndexOf("/") + 1);
		} else {
			return url + "/";
		}
	}

	public static List<HashMap<String, String>> json2List(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(json, List.class);
		} catch (IOException e) {
			errorLog.error("Json Parse Error!", e);
		}
		return null;
	}

	public static List<ReceivingNoteTO> getReceivingNoteTOListFromFileForTesco(File receivingFile) throws BaseException {
		List<ReceivingNoteTO> allReceivingNoteTOList = new ArrayList<ReceivingNoteTO>();

		try {
			if (receivingFile.exists()) {

				String fileName = receivingFile.getName();
				String[] splitStr = fileName.split("_");
				String userID = splitStr[2];
				InputStream sourceExcel = new FileInputStream(receivingFile);
				Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);
				String orderNo = null;
				String storeID = null;
				String storeName = null;
				String receivingDateStr = null;
				Date receivingDate = null;
				if (sourceWorkbook.getNumberOfSheets() != 0) {
					Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
					for (int i = 1; i < (sourceSheet.getPhysicalNumberOfRows() - 1); i++) {
						Row sourceRow = sourceSheet.getRow(i);
						if (sourceRow == null) {
							continue;
						}

						if (sourceRow.getCell(11).getStringCellValue() != null
								&& !sourceRow.getCell(11).getStringCellValue().equals("")) {

							receivingDateStr = sourceRow.getCell(11).getStringCellValue();
							receivingDate = DateUtil.toDate(receivingDateStr);
						}
						// If receivingDate is in the date range

						ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO();
						receivingNoteTO.setUserID(userID);
						for (int j = 0; j < sourceRow.getLastCellNum(); j++) {
							Cell sourceCell = sourceRow.getCell(j);
							String sourceCellValue = null;

							int cellType = sourceCell.getCellType();
							if (cellType == Cell.CELL_TYPE_NUMERIC) {
								sourceCellValue = Double.valueOf(sourceCell.getNumericCellValue()).toString();
							} else {

								sourceCellValue = sourceCell.getStringCellValue();
							}
							switch (j) {
							case 3:
								if (sourceCellValue != null && !sourceCellValue.equals("")) {
									storeID = sourceCellValue.substring(0, sourceCellValue.indexOf("."));
								}
								receivingNoteTO.setStoreID(storeID);
								continue;
							case 4:

								if (sourceCellValue != null && !sourceCellValue.equals("")) {
									storeName = sourceCellValue;
								}
								receivingNoteTO.setStoreName(storeName);
								continue;
							case 5:

								if (sourceCellValue != null && !sourceCellValue.equals("")) {
									sourceCellValue = Utils.trimPrefixZero(sourceCellValue);
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

						allReceivingNoteTOList.add(receivingNoteTO);
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
		return allReceivingNoteTOList;
	}

	public static List<ReceivingNoteTO> getReceivingNoteTOListFromFileForRainbow(File receivingFile)
			throws BaseException {
		List<ReceivingNoteTO> receivingNoteList = new ArrayList<ReceivingNoteTO>();
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
					RainbowReceivingTO receivingNoteTO = new RainbowReceivingTO(receivingLine);
					receivingNoteTO.setUserID(userID);
					receivingNoteList.add(receivingNoteTO);
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

			log.info("收货单: " + receivingFile.getName() + " 包含的详单数量为:" + receivingNoteList.size());

		}
		return receivingNoteList;
	}

	public static List<SalesTO> getSalesTOListFromFileForRainbow(File salesFile) throws BaseException {
		List<SalesTO> salesTOList = new ArrayList<SalesTO>();

		if (salesFile.exists()) {
			String fileName = salesFile.getName();
			String[] splitStr = fileName.split("_");
			String userID = splitStr[2];
			try {
				InputStream sourceExcel = new FileInputStream(salesFile);

				Workbook sourceWorkbook = new HSSFWorkbook(sourceExcel);
				if (sourceWorkbook.getNumberOfSheets() != 0) {
					Sheet sourceSheet = sourceWorkbook.getSheetAt(0);
					for (int i = 1; i <= sourceSheet.getPhysicalNumberOfRows(); i++) {
						Row sourceRow = sourceSheet.getRow(i);
						if (sourceRow == null) {
							continue;
						}

						String salesDateStr = sourceRow.getCell(8).getStringCellValue();

						SalesTO salesTO = new SalesTO();

						salesTO.setItemID(sourceRow.getCell(1).getStringCellValue());
						salesTO.setItemName(sourceRow.getCell(2).getStringCellValue());
						salesTO.setStoreID(sourceRow.getCell(5).getStringCellValue());
						salesTO.setSalesDate(salesDateStr);
						salesTO.setSalesQuantity(sourceRow.getCell(6).getStringCellValue());
						salesTO.setSalesAmount(sourceRow.getCell(7).getStringCellValue());
						salesTO.setUserID(userID);
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
		}
		return salesTOList;
	}

	public static List<SalesTO> getSalesTOListFromFileForRenrenle(File salesFile) throws BaseException {

		List<SalesTO> salesTOList = new ArrayList<SalesTO>();

		if (salesFile.exists()) {
			String fileName = salesFile.getName();
			String[] splitStr = fileName.split("_");
			String userID = splitStr[2];
			String salesDate = fileName.substring(fileName.lastIndexOf("_") + 1, fileName.indexOf("."));
			salesDate = DateUtil.toString(DateUtil.toDate(salesDate, "yyyyMMdd"));

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
						salesTO.setUserID(userID);

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
		}
		return salesTOList;
	}

	public static List<OrderTO> getOrderTOListFromFileForYonghui(File orderFile) {

		List<OrderTO> orderTOList = new ArrayList<OrderTO>();
		if (orderFile.exists()) {
			String fileName = orderFile.getName();
			String[] splitStr = fileName.split("_");
			String userID = splitStr[2];
			ExcelReader excelReader = null;
			try {
				excelReader = new ExcelReader();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				errorLog.error(e);
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				errorLog.error(e);
			}
			nl.fountain.xelem.excel.Workbook wb = null;
			try {
				wb = excelReader.getWorkbook(orderFile.getPath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				errorLog.error(e);
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				errorLog.error(e);
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

					nl.fountain.xelem.excel.Cell sourceCell = sourceRow.getCellAt(j);

					switch (j) {
					case 2:
						orderNo = sourceCell.getData$();
						orderTO.setOrderNo(orderNo);

						continue;
					case 5:
						String storeID = sourceCell.getData$();
						orderTO.setStoreID(storeID);

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
						orderTO.setItemID(itemCode);

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
				orderTO.setUserID(userID);
				orderTOList.add(orderTO);

			}
		}
		return orderTOList;
	}

	public static int getConlidatedOrderNoAmountByReceivingNoteTO(List<ReceivingNoteTO> receivingNoteList) {
		int orderDownloadAmount = 0;
		Set<String> orderNoSet = new HashSet<String>();
		for (ReceivingNoteTO receivingNoteTO : receivingNoteList) {
			orderNoSet.add(receivingNoteTO.getOrderNo());
		}
		orderDownloadAmount = orderNoSet.size();
		return orderDownloadAmount;
	}

	public static int getConlidatedOrderNoAmountByOrderTO(List<OrderTO> orderTOList) {
		int orderDownloadAmount = 0;
		Set<String> orderNoSet = new HashSet<String>();
		for (OrderTO orderTO : orderTOList) {
			orderNoSet.add(orderTO.getOrderNo());
		}
		orderDownloadAmount = orderNoSet.size();
		return orderDownloadAmount;
	}

}
