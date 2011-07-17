package com.hz.yk.ex03.connector.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ��������ְ���Ǵ���һ���������׽��������ȴ�ǰ���� HTTP����
 * 
 * @author "yangk"
 * @date 2011-4-19 ����03:00:15
 * 
 */
public class HttpConnector implements Runnable {
	boolean stopped;
	private String scheme = "http";

	public String getScheme() {
		return scheme;
	}

	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(Constants.PORT, 1, InetAddress
					.getByName("192.168.0.92"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stopped) {
			// Accept the next incoming connection from the server socket
			Socket socket = null;
			try {
				socket = serverSocket.accept();
			} catch (Exception e) {
				continue;
			}
			// Hand this socket off to an HttpProcessor
			HttpProcessor processor = new HttpProcessor(this);
			processor.process(socket);
		}
	}

	public void start() {
		Thread thread = new Thread(this);
		thread.start();
	}
}
