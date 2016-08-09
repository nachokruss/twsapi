package com.twsapi;

import com.ib.controller.ApiConnection.ILogger;

public class SystemOutLogger implements ILogger {

	private boolean showHeader = true;
	private String prefix = null;

	public SystemOutLogger(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void log(String logMsg) {
		if (showHeader) {
			System.out.print(prefix);
			showHeader = false;
		}
		if (logMsg.equals("\n")) {
			showHeader = true;
		}
		System.out.print(logMsg);
	}

}
