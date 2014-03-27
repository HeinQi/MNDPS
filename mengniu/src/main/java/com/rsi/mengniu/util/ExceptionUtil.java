package com.rsi.mengniu.util;

import java.io.FileNotFoundException;
import java.nio.channels.FileLockInterruptionException;
import java.nio.file.FileSystemException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import com.rsi.mengniu.Constants;

public class ExceptionUtil {
	public static String fromExceptionToMessage(Exception e) {
		if (e instanceof ConnectTimeoutException) {
			return Constants.ERROR_MESSAGE_TIMEOUT;
		}
		if (e instanceof NoHttpResponseException) {
			return Constants.ERROR_MESSAGE_NORESPONSE;
		}
		if (e instanceof FileLockInterruptionException || e instanceof  FileNotFoundException
				||e instanceof FileSystemException) {
			return Constants.ERROR_MESSAGE_NOTFIND;
		}
		return Constants.ERROR_MESSAGE_UNKNOWN;
	}
}
