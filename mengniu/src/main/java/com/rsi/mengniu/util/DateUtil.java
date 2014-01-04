package com.rsi.mengniu.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.rsi.mengniu.exception.BaseException;

public class DateUtil {

	public static DateUtil dateUtil;
	public static DateUtil getInstance(){
		synchronized(DateUtil.class){
            //判断是否存在类实例如果不存在就实例化一个
           if (dateUtil==null) {
        	   dateUtil=new DateUtil();
               
            }
		}
		
		return new DateUtil();
	}
	
	
	public static Date toDate(String dateStr) throws BaseException {
		//TODO date format unify
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date receivingDate = dateFormat.parse(dateStr);
			return receivingDate;
		} catch (ParseException e) {
			System.out.println(dateStr);
			throw new BaseException(e);
		}

	}
	
	public static String toStringYYYYMMDD(Date date){

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		return dateFormat.format(date);
	}
	
	public static List<Date> getDateArrayByRange(Date startDate, Date endDate) throws ParseException{
		int days = daysBetween(startDate,endDate);
		List<Date> dateList = new ArrayList<Date>();
		for(int i = 0; i <=days;i++){
			dateList.add(getDateAfter(startDate,i));
		}
		
		return dateList;
	}
	

    public static int daysBetween(Date smdate,Date bdate) throws ParseException  
    {  
    	SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
    	smdate=sdf.parse(sdf.format(smdate));
    	bdate=sdf.parse(sdf.format(bdate));
        Calendar cal = Calendar.getInstance();  
        cal.setTime(smdate);  
        long time1 = cal.getTimeInMillis();               
        cal.setTime(bdate);  
        long time2 = cal.getTimeInMillis();       
        long between_days=(time2-time1)/(1000*3600*24);
          
       return Integer.parseInt(String.valueOf(between_days));         
    }  
	
    public static Date getDateAfter(Date d, int day) {  
    	        Calendar now = Calendar.getInstance();  
    	        now.setTime(d);  
    	        now.set(Calendar.DATE, now.get(Calendar.DATE) + day);  
    	        return now.getTime();  
    	    }  

}
