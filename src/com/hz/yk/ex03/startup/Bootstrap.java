package com.hz.yk.ex03.startup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hz.yk.ex03.connector.http.HttpConnector;

public class Bootstrap {
	static Log log = LogFactory.getLog(Bootstrap.class);

	public static void main(String[] args) {
		HttpConnector connector = new HttpConnector();
		connector.start();
		log.info("server is start ...");

	}
}
