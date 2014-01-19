package com.rsi.mengniu.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.rsi.mengniu.DataPullTaskPool;
import com.rsi.mengniu.exception.BaseException;


public class Utils {
	private static Properties properties; 

	public void setProperties(Properties p) {
		properties = p;
	}
	public static String getProperty(String key) {
		return properties.getProperty(key);
	}

	public static Date getStartDate(String retailerId) throws BaseException {
		if ("N".equalsIgnoreCase(getProperty(retailerId+".daterange.enable"))) {
			return DateUtil.getDateAfter(new Date(), -1);
		} else {
			return DateUtil.toDate(getProperty(retailerId+".daterange.startDate"));
		}
	}
	public static Date getEndDate(String retailerId) throws BaseException {
		if ("N".equalsIgnoreCase(getProperty(retailerId+".daterange.enable"))) {
			return DateUtil.getDateAfter(new Date(), -1);
		} else {
			return DateUtil.toDate(getProperty(retailerId+".daterange.endDate"));
		}		
	}
	public static int getSleepTime(String retailerId) {
		return Integer.parseInt(getProperty(retailerId+".sleep.time"));
	}
	public static String getTrace(Throwable t) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		t.printStackTrace(writer);
		StringBuffer buffer = stringWriter.getBuffer();
		return buffer.toString();
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
	
	public static String trimPrefixZero(String sourceStr){
		int indexNo = sourceStr.indexOf("0");
		if(indexNo==0){
			sourceStr = sourceStr.substring(indexNo+1);
			sourceStr = trimPrefixZero(sourceStr);
			
		}
		
		return sourceStr;
	}
	
	public static String getRaimbowStoreIDByName(String storeName) throws BaseException{
		String storeID =  getRainbowStoreMapping().get(storeName);
		return storeID==null?storeName:storeID;
	}
	
	public static Map<String, String> getRainbowStoreMapping() throws BaseException{

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
				sourceCellValue = Double.valueOf(
						sourceCell.getNumericCellValue())
						.toString();
			} else {

				sourceCellValue = sourceCell.getStringCellValue();
			}
			
			String storeID = 
					sourceCellValue;
			String storeName = sourceRow.getCell(1)
					.getStringCellValue();
			
			storeMap.put(storeName,storeID);
		}
		return storeMap;
	}

	public static void putSubMapToMainMap(Map<String, List> mainMap,
			Map<String, List> subMap) {
		for (Entry<String, List> entry : subMap
				.entrySet()) {
			String key = entry.getKey();
			List valueList = entry.getValue();
			if (mainMap.containsKey(key)) {
				mainMap.get(key).addAll(valueList);
			} else {
				mainMap.put(key, valueList);
			}
		}
	}
}
