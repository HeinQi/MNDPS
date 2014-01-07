package com.rsi.mengniu.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;

public class FileUtil {

	public static void main(String[] args) throws BaseException {
		try {
			unzip("C:/test/source/20140106214712.zip", "C:/test/dest/", "");
		} catch (ZipException e) {
			throw new BaseException(e);
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

				closeFileWriter(writer);
				throw new BaseException(e);
			}
		} else {
			try {
				writer = new BufferedWriter(new FileWriter(orderFile, true));
			} catch (IOException e) {

				closeFileWriter(writer);
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

			closeFileWriter(writer);

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
	public static void exportReceivingInfoToTXT(String retailerID,
			String userID, List<ReceivingNoteTO> receivingList)
			throws BaseException {
		File receivingFile = new File(Constants.TEST_ROOT_PATH + retailerID
				+ "/receiving/inbound/Receiving_" + retailerID + "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(new Date()) + ".txt");
		BufferedWriter writer = null;

		try {
			receivingFile.createNewFile();
			String orderHeader = "Order_No	Order_Date	Store_No	Store_Name	Item_Code	Item_Name	Barcode	Quantity	Unit_Price	Total_Price";
			writer = new BufferedWriter(new FileWriter(receivingFile, true));
			writer.write(orderHeader);
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

			closeFileWriter(writer);

		}

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

	/**
	 * Copy selected files from sourcePath to dest path
	 * 
	 * @param sourceFileNameList
	 * @param sourcePath
	 * @param destPath
	 */
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

	/**
	 * Close File Writer
	 * 
	 * @param writer
	 */
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

	/**
	 * Close File Reader
	 * 
	 * @param reader
	 */
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

	public static void unzip(String fileName, String dest, String passwd)
			throws ZipException {

		File zipFile = new File(fileName);
		ZipFile zFile = new ZipFile(zipFile); // 首先创建ZipFile指向磁盘上的.zip文件
		// zFile.setFileNameCharset("GBK"); // 设置文件名编码，在GBK系统中需要设置
		if (!zFile.isValidZipFile()) { // 验证.zip文件是否合法，包括文件是否存在、是否为zip文件、是否被损坏等
			throw new ZipException("压缩文件不合法,可能被损坏.");
		}
		File destDir = new File(dest); // 解压目录
		if (destDir.isDirectory() && !destDir.exists()) {
			destDir.mkdir();
		}
		if (zFile.isEncrypted()) {
			zFile.setPassword(passwd.toCharArray()); // 设置密码
		}
		zFile.extractAll(dest); // 将文件抽出到解压目录(解压)
	}

}
