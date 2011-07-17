package com.hz.yk.ex01;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpServer {
	Log log = LogFactory.getLog(this.getClass());
	/**
	 * WEB_ROOT is the directory where our HTML and other files reside. For this
	 * package, WEB_ROOT is the "webroot" directory under the working directory.
	 * The working directory is the location in the file system from where the
	 * java command was invoked.
	 */
	public static final String WEB_ROOT = System.getProperty("user.dir")
			+ File.separator + "webroot";

	// shutdown command
	private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

	// the shutdown command received
	private boolean shutdown = false;

	public static void main(String[] args) {
		HttpServer server = new HttpServer();
		server.await();
		// System.out.println("####"+WEB_ROOT);
	}

	public void await() {
		ServerSocket serverSocket = null;
		int port = 8088;
		try {
			serverSocket = new ServerSocket(port, 1, InetAddress
					.getByName("127.0.0.1"));
			log.info("��������");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		// Loop waiting for a request
		while (!shutdown) {
			Socket socket = null;
			InputStream input = null;
			OutputStream output = null;

			try {
				socket = serverSocket.accept();
				input = socket.getInputStream();
				output = socket.getOutputStream();
				// create Request object and parse
				Request request = new Request(input);
				request.parse();

				// create Response object
				Response response = new Response(output);
				response.setRequest(request);
				response.sendStaticResource();

				// Close the socket
				socket.close();

				// check if the previous URI is a shutdown command
				shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
}
