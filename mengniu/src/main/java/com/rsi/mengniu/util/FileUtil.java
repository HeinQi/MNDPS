package com.rsi.mengniu.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;

public class FileUtil {

	/**
	 * Export Receiving Info To inbound txt file
	 * 
	 * @param retailerID
	 * @param userID
	 * @param processingDate
	 * @param receivingList
	 * @throws BaseException
	 */
	public static void exportReceivingNoteToTXT(String retailerID,
			String userID, Date processingDate,
			List<ReceivingNoteTO> receivingList) throws BaseException {
		File receivingInboundFile = new File(Constants.TEST_ROOT_PATH
				+ retailerID + "/receiving/inbound/Receiving_" + retailerID
				+ "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(processingDate) + ".txt");
		String receivingHeader = "Order_No	Receiving_Date	Store_No	Store_Name	Item_Code	Item_Name	Barcode	Receiving_Qty	Unit_Price	Receiving_Total_Price";

		BufferedWriter writer = null;
		if (!receivingInboundFile.exists()) {
			try {
				receivingInboundFile.createNewFile();
				writer = new BufferedWriter(new FileWriter(
						receivingInboundFile, true));
				writer.write(receivingHeader);
				writer.newLine();
			} catch (IOException e) {
				throw new BaseException();
			}
		} else {
			try {
				writer = new BufferedWriter(new FileWriter(
						receivingInboundFile, true));
			} catch (IOException e) {
				throw new BaseException();
			}
		}

		try {

			for (int i = 0; i < receivingList.size(); i++) {
				ReceivingNoteTO receivingNoteTO = receivingList.get(i);
				String receivingNoteRow = receivingNoteTO.toString();
				writer.write(receivingNoteRow);
				writer.newLine();
			}

		} catch (IOException e) {
			closeFileWriter(writer);
			// TODO Auto-generated catch block
			throw new BaseException(e);
		}

		closeFileWriter(writer);

	}

	/**
	 * Export Order info from list to txt file
	 * 
	 * @param retailerID
	 * @param orderID
	 * @param orderList
	 * @throws BaseException
	 */
	public static void exportOrderInfoToTXT(String retailerID, String orderID,
			List<OrderTO> orderList) throws BaseException {
		File orderFile = new File(Constants.TEST_ROOT_PATH + retailerID
				+ "/order/Order_" + retailerID + "_" + orderID + ".txt");
		BufferedWriter writer = null;
		if (!orderFile.exists()) {
			try {
				orderFile.createNewFile();
				String orderHeader = "Order_No	Order_Date	Store_No	Store_Name	Item_Code	Item_Name	Barcode	Quantity	Unit_Price	Total_Price";
				writer = new BufferedWriter(new FileWriter(orderFile, true));
				writer.write(orderHeader);
				writer.newLine();
			} catch (IOException e) {
				throw new BaseException();
			}
		} else {
			try {
				writer = new BufferedWriter(new FileWriter(orderFile, true));
			} catch (IOException e) {
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
			closeFileWriter(writer);
			// TODO Auto-generated catch block
			throw new BaseException(e);
		}

		closeFileWriter(writer);

	}

	public static List<String> getAllFile(String folderPath) {
		File receivingInboundFolder = new File(folderPath);

		List<String> fileNameList = new ArrayList<String>();
		if (receivingInboundFolder.isDirectory()) {

			File[] receivingList = receivingInboundFolder.listFiles();

			for (File receivingFile : receivingList) {
				String receivingFileName = receivingFile.getName();
				fileNameList.add(receivingFileName);
			}
		}

		return fileNameList;
	}

	/**
	 * Copy File
	 * 
	 * @param sourceFileName
	 * @param destPath
	 */
	public static void copyFile(String sourceFileName, String sourcePath,
			String destPath) {
		// 文件原地址
		File oldFile = new File(sourcePath + sourceFileName);
		// 文件新（目标）地址
		// new一个新文件夹
		File fnewpath = new File(destPath);
		// 判断文件夹是否存在
		if (!fnewpath.exists())
			fnewpath.mkdirs();
		// 将文件移到新文件里
		File fnew = new File(destPath + oldFile.getName());
		oldFile.renameTo(fnew);

	}

	public static void copyFiles(List<String> sourceFileNameList,
			String sourcePath, String destPath) {

		for (String sourceFileName : sourceFileNameList) {

			// 文件原地址
			File oldFile = new File(sourcePath + sourceFileName);
			// 文件新（目标）地址
			// new一个新文件夹
			File fnewpath = new File(destPath);
			// 判断文件夹是否存在
			if (!fnewpath.exists())
				fnewpath.mkdirs();
			// 将文件移到新文件里
			File fnew = new File(destPath + oldFile.getName());
			oldFile.renameTo(fnew);
		}

	}

	public static void closeFileWriter(BufferedWriter writer) {
		if (writer != null) {
			try {
				writer.flush();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void closeFileReader(BufferedReader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
