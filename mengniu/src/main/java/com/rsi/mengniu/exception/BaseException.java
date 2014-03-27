package com.rsi.mengniu.exception;


public class BaseException extends Exception {

	public BaseException(Exception e) {
		super(e);
	}
	
	

	public BaseException() {
		super();
		// TODO Auto-generated constructor stub
	}



	public BaseException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}



	public BaseException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}



	/**
	 * 
	 */
	private static final long serialVersionUID = 5280777430201480801L;

	
}
