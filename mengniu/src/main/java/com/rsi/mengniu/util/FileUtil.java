package com.rsi.mengniu.util;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.RainbowReceivingTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;
import com.rsi.mengniu.retailer.module.SalesTO;

public class FileUtil {

	public static void main(String[] args) throws BaseException {
		//testFileAmount("C:/mengniu/yonghui/output/");
		List<String> stringList = getAllFile("C:/mengniu/carrefour/receiving/inbound/");
		copyFiles(stringList, "C:/mengniu/carrefour/receiving/inbound/", "C:/mengniu/carrefour/receiving/processed/");
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

		String orderFolderPath = Utils.getProperty(retailerID
				+ Constants.ORDER_PATH);
		createFolder(orderFolderPath);
		String orderFilePath = orderFolderPath + "Order_" + retailerID + "_"
				+ orderID + ".txt";
		// log.info("初始化整合文本文件. 文件名: " + receivingInboundFolderPath);
		// File receivingInboundFolder = new File(orderFolderPath);
		// if (!receivingInboundFolder.exists()) {
		// receivingInboundFolder.mkdir();
		// }

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

				closeFileWriter(writer);
				throw new BaseException(e);
			}
		} else {
			try {

				// TODO consider that re-run action
				FileOutputStream fileOutput = new FileOutputStream(orderFile,true);
				writer = new BufferedWriter(new OutputStreamWriter(fileOutput, "UTF-8"));
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

		String receivingInboundFolderPath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_INBOUND_PATH);
		createFolder(receivingInboundFolderPath);
		String receivingFilePath = receivingInboundFolderPath + "Receiving_"
				+ retailerID + "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(new Date()) + ".txt";
		// log.info("初始化整合文本文件. 文件名: " + receivingInboundFolderPath);
		// File receivingInboundFolder = new File(receivingInboundFolderPath);
		// if (!receivingInboundFolder.exists()) {
		// receivingInboundFolder.mkdir();
		// }

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
	public static void exportReceivingInfoToTXTForRainbow(String retailerID,
			String userID, List<ReceivingNoteTO> receivingList)
			throws BaseException {

		String receivingInboundFolderPath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_INBOUND_PATH);
		createFolder(receivingInboundFolderPath);
		String receivingFilePath = receivingInboundFolderPath + "Receiving_"
				+ retailerID + "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(new Date()) + ".txt";
		// log.info("初始化整合文本文件. 文件名: " + receivingInboundFolderPath);
		// File receivingInboundFolder = new File(receivingInboundFolderPath);
		// if (!receivingInboundFolder.exists()) {
		// receivingInboundFolder.mkdir();
		// }

		File receivingFile = new File(receivingFilePath);

		BufferedWriter writer = null;

		try {
			receivingFile.createNewFile();
			String receivingHeader = Utils.getProperty(Constants.OUTPUT_ORDER_HEADER);

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
	public static void exportSalesInfoToTXT(String retailerID,
			String userID, List<SalesTO> salesList)
			throws BaseException {

		String salesInboundFolderPath = Utils.getProperty(retailerID
				+ Constants.RECEIVING_INBOUND_PATH);
		createFolder(salesInboundFolderPath);
		String salesFilePath = salesInboundFolderPath + "Sales_"
				+ retailerID + "_" + userID + "_"
				+ DateUtil.toStringYYYYMMDD(new Date()) + ".txt";
		// log.info("初始化整合文本文件. 文件名: " + salesInboundFolderPath);
		// File salesInboundFolder = new File(salesInboundFolderPath);
		// if (!salesInboundFolder.exists()) {
		// salesInboundFolder.mkdir();
		// }

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
	 * @throws BaseException 
	 */
	public static void copyFiles(List<String> sourceFileNameList,
			String sourcePath, String destPath) throws BaseException {

		for (String sourceFileName : sourceFileNameList) {
			File fnewpath = new File(destPath);
			if (!fnewpath.exists())	fnewpath.mkdirs();
			copyFile(sourcePath + sourceFileName, destPath + sourceFileName);
//			// 文件原地址
			File oldFile = new File(sourcePath + sourceFileName);
			
			oldFile.delete();
//			// 文件新（目标）地址
//			// new一个新文件夹
//			File fnewpath = new File(destPath);
//			// 判断文件夹是否存在
//			if (!fnewpath.exists())
//				fnewpath.mkdirs();
//			// 将文件移到新文件里
//			File fnew = new File(destPath + oldFile.getName());
//			
//			oldFile.renameTo(fnew);
		}

	}

	public static void createFolder(String path) {
		File fnewpath = new File(path);
		// 判断文件夹是否存在
		if (!fnewpath.exists())
			fnewpath.mkdirs();
	}

	public static void copyFile( String oldPath, String newPath ) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File( oldPath );
            if ( oldfile.exists() ) { //文件存在时
               InputStream inStream = new FileInputStream( oldPath ); //读入原文件
               FileOutputStream fs = new FileOutputStream( newPath );
                byte[] buffer = new byte[1444];
                int length;
                while ( ( byteread = inStream.read( buffer ) ) != - 1 ) {
                    bytesum += byteread; //字节数 文件大小
                   
                    fs.write( buffer, 0, byteread );
                }
                inStream.close();
            }
        } catch ( Exception e ) {
            System.out.println( "复制单个文件操作出错" );
            e.printStackTrace();

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
		
		zipFile.delete();
	}

	public static void testFileAmount(String folderPath) {

		int j = 0;
		File receivingInboundFolder = new File(folderPath);

		File[] orderList = receivingInboundFolder.listFiles();

		Map<String, OrderTO> orderMap = new HashMap<String, OrderTO>();
		for (int i = 0; i < orderList.length; i++) {

			File orderFile = orderList[i];
			BufferedReader reader = null;
			// Open the file
			try {
				FileInputStream fileInput = new FileInputStream(orderFile);
				InputStreamReader inputStrReader = new InputStreamReader(
						fileInput, "UTF-8");
				reader = new BufferedReader(inputStrReader);
				String orderLine;
				while ((orderLine = reader.readLine()) != null) {
					j++;
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			j = j - 1;

		}
		System.out.println(j);
	}

}
