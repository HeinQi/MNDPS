package com.rsi.mengniu.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rsi.mengniu.Constants;
import com.rsi.mengniu.exception.BaseException;

public class DateUtil {
	private static Log errorLog = LogFactory.getLog(Constants.SYS_ERROR);

	public static Date toDate(String dateStr) throws BaseException {
		// TODO date format unify
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date receivingDate = dateFormat.parse(dateStr);
			return receivingDate;
		} catch (ParseException e) {
			System.out.println(dateStr);
			throw new BaseException(e);
		}

	}

	public static Date toDate(String dateStr, String datePattern) throws BaseException {
		// TODO date format unify
		DateFormat dateFormat = new SimpleDateFormat(datePattern);
		try {
			Date receivingDate = dateFormat.parse(dateStr);
			return receivingDate;
		} catch (ParseException e) {
			System.out.println(dateStr);
			throw new BaseException(e);
		}

	}

	public static String toString(Date date) {

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(date);
	}

	public static String toString(Date date, String datePattern) {

		DateFormat dateFormat = new SimpleDateFormat(datePattern);
		return dateFormat.format(date);
	}

	public static String toStringYYYYMMDD(Date date) {

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		return dateFormat.format(date);
	}

	public static List<Date> getDateArrayByRange(Date startDate, Date endDate) {
		int days;
		try {
			days = daysBetween(startDate, endDate);
		} catch (ParseException e) {
			errorLog.error(e);
			return new ArrayList<Date>();
		}
		List<Date> dateList = new ArrayList<Date>();
		for (int i = 0; i <= days; i++) {
			dateList.add(getDateAfter(startDate, i));
		}

		return dateList;
	}

	private static int daysBetween(Date smdate, Date bdate) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		smdate = sdf.parse(sdf.format(smdate));
		bdate = sdf.parse(sdf.format(bdate));
		Calendar cal = Calendar.getInstance();
		cal.setTime(smdate);
		long time1 = cal.getTimeInMillis();
		cal.setTime(bdate);
		long time2 = cal.getTimeInMillis();
		long between_days = (time2 - time1) / (1000 * 3600 * 24);

		return Integer.parseInt(String.valueOf(between_days));
	}

	public static Date getDateAfter(Date d, int day) {
		Calendar now = Calendar.getInstance();
		now.setTime(d);
		now.set(Calendar.DATE, now.get(Calendar.DATE) + day);
		return now.getTime();
	}

	public static boolean isInDateRange(Date verifyDate, Date startDate, Date endDate) {
		return (verifyDate.before(endDate) && verifyDate.after(startDate)) || verifyDate.equals(startDate)
				|| verifyDate.equals(endDate);
	}

}
