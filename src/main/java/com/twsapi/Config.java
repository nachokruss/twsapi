package com.twsapi;

import java.io.IOException;
import java.util.Properties;

public class Config {
	
	private Properties properties = new Properties();
	
	public Config() {
	}
	
	public void init() throws IOException {
		properties.load(getClass().getResourceAsStream("/app.properties"));
	}
	
	public String getTwsIp() {
		return properties.getProperty("tws.ip", "127.0.0.1");
	}
	
	public Integer getTwsPort() {
		return new Integer(properties.getProperty("tws.port", "7497"));
	}
	
	public boolean isTransmit() {
		return new Boolean(properties.getProperty("transmit", "false"));
	}

}
