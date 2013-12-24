package com.rsi.mengniu;

public class Test {
	public static void main(String[] args) {
		String responseStr = "<input type=\"button\" name=\"download\" value=\"下载\" class=\"button2\" onclick=\"javascript:downloads('InyrDetailList000000ZFJ9113110233127.xls');\"/>";
		responseStr = responseStr.substring(responseStr.indexOf("javascript:downloads('")+22);
		System.out.println(responseStr);
		String inyrFileName = responseStr.substring(0,responseStr.indexOf("'"));
		System.out.println(inyrFileName);
		
		
	}

}
