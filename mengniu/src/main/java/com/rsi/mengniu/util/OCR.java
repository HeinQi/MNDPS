package com.rsi.mengniu.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OCR {
	private final String LANG_OPTION = "-l";
	private final String EOL = System.getProperty("line.separator");
	private String tessPath = "/opt/local/bin";

	public String recognizeText(String imageFile, String outputName) throws Exception {

		StringBuffer strB = new StringBuffer();

		List<String> cmd = new ArrayList<String>();

		cmd.add(tessPath + "/tesseract");

		cmd.add("");
		cmd.add(outputName);
		//cmd.add(LANG_OPTION);
		//cmd.add("eng");
		cmd.add("digits");

		ProcessBuilder pb = new ProcessBuilder();
		// pb.directory(imageFile.getParentFile());

		cmd.set(1, imageFile);
		pb.command(cmd);
		pb.redirectErrorStream(true);
		Process process = pb.start();

		int w = process.waitFor();
		if (w == 0) {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(outputName + ".txt"), "UTF-8"));
			String str;
			while ((str = in.readLine()) != null) {
				strB.append(str);
			}
			in.close();
		} else {
			String msg;
			switch (w) {
			case 1:
				msg = "Errors accessing files. There may be spaces in your image's filename.";
				break;
			case 29:
				msg = "Cannot recognize the image or its selected region.";
				break;
			case 31:
				msg = "Unsupported image format.";
				break;
			default:
				msg = "Errors occurred.";
			}

			throw new RuntimeException(msg);
		}
		return strB.toString();
	}

	private OCR() {
	};

	private static OCR ocr = new OCR();

	public static OCR getInstance() {
		return ocr;
	}
}