package com.rsi.mengniu.util;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.StringUtils;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.DataPullTaskPool;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.RainbowReceivingTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;
import com.rsi.mengniu.retailer.module.User;

public class Utils {
	private static Properties properties;

	public void setProperties(Properties p) {
		properties = p;
	}

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}

	public static Date getStartDate(String retailerId) throws BaseException {
		if ("N".equalsIgnoreCase(getProperty(retailerId + ".daterange.enable"))) {
			return DateUtil.getDateAfter(new Date(), -1);
		} else {
			return DateUtil.toDate(getProperty(retailerId + ".daterange.startDate"));
		}
	}

	public static Date getEndDate(String retailerId) throws BaseException {
		if ("N".equalsIgnoreCase(getProperty(retailerId + ".daterange.enable"))) {
			return DateUtil.getDateAfter(new Date(), -1);
		} else {
			return DateUtil.toDate(getProperty(retailerId + ".daterange.endDate"));
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

	public static String getRaimbowStoreIDByName(String storeName) throws BaseException {
		String storeID = getRainbowStoreMapping().get(storeName);
		return storeID == null ? "" : storeID;
	}

	public static Map<String, String> getRainbowStoreMapping() throws BaseException {

		Map<String, String> storeMap = new HashMap<String, String>();
		InputStream sourceExcel = DataPullTaskPool.class.getResourceAsStream("/data/Rainbow_store_mapping.xls");

		Workbook sourceWorkbook;
		try {
			sourceWorkbook = new HSSFWorkbook(sourceExcel);
		} catch (IOException e) {
			throw new BaseException(e);
		}

		Sheet sourceSheet = sourceWorkbook.getSheetAt(0);

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

			storeMap.put(storeName, storeID);
		}
		return storeMap;
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
	public static void exportOrderInfoToTXT(String retailerID,String userID, String orderID,  Date orderDate,
			List<OrderTO> orderList) throws BaseException {

		String orderFolderPath = Utils.getProperty(retailerID
				+ Constants.ORDER_INBOUND_PATH);
		FileUtil.createFolder(orderFolderPath);
		String orderFilePath = orderFolderPath + "Order_" + retailerID + "_" + userID + "_"+ orderID + "_"
				+ DateUtil.toStringYYYYMMDD(orderDate) + ".txt";

		File orderFile = new File(orderFilePath);
		BufferedWriter writer = null;
		if (!orderFile.exists()) {
			try {
				orderFile.createNewFile();
				String orderHeader = Utils.getProperty(Constants.ORDER_HEADER);
				FileOutputStream fileOutput = new FileOutputStream(orderFile,true);
				writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
				writer.write(orderHeader);
				writer.newLine();
			} catch (IOException e) {

				FileUtil.closeFileWriter(writer);
				throw new BaseException(e);
			}
		} else {
			try {

				// TODO consider that re-run action
				FileOutputStream fileOutput = new FileOutputStream(orderFile,true);
				writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
			} catch (IOException e) {

				FileUtil.closeFileWriter(writer);
				throw new BaseException();
			}
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
//	public static void exportOrderInfoListToTXT(String retailerID, String userID,String orderID, Date orderDate,
//			List<OrderTO> orderList) throws BaseException {
//		Map <String,List<OrderTO>> orderMap = new HashMap<String, List<OrderTO>>();
//		List<OrderTO> tempOrderList = null;
//		for(OrderTO orderTO : orderList){
//			String orderNo = orderTO.getOrderNo();
//			
//			if(orderMap.containsKey(orderNo)){
//				tempOrderList = orderMap.get(orderNo);
//			} else {
//				tempOrderList = new ArrayList<OrderTO>();
//				orderMap.put(orderNo, tempOrderList);
//			}
//			tempOrderList.add(orderTO);
//		}
//		for(Entry<String, List<OrderTO>> entry:orderMap.entrySet()){
//			exportOrderInfoToTXT(retailerID,userID,orderID,orderDate,entry.getValue());
//		}
//	}

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

	public static void recordIncorrectUser(User user) throws BaseException {

		String filePath = getProperty(Constants.INCORRECT_USER_HEADER);
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
			HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(excelFile));
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

}
