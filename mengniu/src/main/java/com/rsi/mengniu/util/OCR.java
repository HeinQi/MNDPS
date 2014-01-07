package com.rsi.mengniu.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OCR {
	private String tessPath;

	public String getTessPath() {
		return tessPath;
	}

	public void setTessPath(String tessPath) {
		this.tessPath = tessPath;
	}

	public String recognizeText(String imageFile, String outputName, boolean isDigits) throws Exception {
		StringBuffer strB = new StringBuffer();
		List<String> cmd = new ArrayList<String>();
		cmd.add(tessPath + "/tesseract");
		cmd.add("");
		cmd.add(outputName);
		if (isDigits) {
			cmd.add("digits");

		} else {
			cmd.add("-l");
			cmd.add("eng");
		}

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
		String recognizeStr = strB.toString();
		return recognizeStr.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
	}
}