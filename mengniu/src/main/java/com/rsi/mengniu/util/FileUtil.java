package com.rsi.mengniu.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;
import com.rsi.mengniu.retailer.module.OrderTO;
import com.rsi.mengniu.retailer.module.ReceivingNoteTO;

public class FileUtil {

	

	/**
	 * Export Receiving Info To inbound txt file
	 * @param retailerID
	 * @param userID
	 * @param processingDate
	 * @param receivingList
	 * @throws BaseException
	 */
	public static void exportReceivingNoteToTXT(String retailerID,
			String userID, Date processingDate, List<ReceivingNoteTO> receivingList)
			throws BaseException {
		File receivingInboundFile = new File(Constants.TEST_ROOT_PATH  + retailerID
				+ "/receiving/inbound/Receiving_"+ retailerID + "_" + userID +"_"+ DateUtil.toStringYYYYMMDD(processingDate) +".txt");

		String receivingHeader = "Order_No	Receiving_Date	Store_No	Store_Name	Item_Code	Item_Name	Barcode	Receiving_Qty	Unit_Price	Receiving_Total_Price";

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(receivingInboundFile));
			writer.write(receivingHeader);
			for (int i = 0; i < receivingList.size(); i++) {
				ReceivingNoteTO receivingNoteTO = new ReceivingNoteTO();
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
	 * @param retailerID
	 * @param orderID
	 * @param orderList
	 * @throws BaseException
	 */
	public static void exportOrderInfoToTXT(String retailerID,
			String orderID, List<OrderTO> orderList)
			throws BaseException {
		File orderFile = new File(Constants.TEST_ROOT_PATH + retailerID
				+ "/order/Order_"+ retailerID + "_" + orderID +".txt");

		String orderHeader = "Order_No	Order_Date	Store_No	Store_Name	Item_Code	Item_Name	Barcode	Quantity	Unit_Price	Total_Price";

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(orderFile));
			writer.write(orderHeader);
			for (int i = 0; i < orderList.size(); i++) {
				OrderTO orderTO = new OrderTO();
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
