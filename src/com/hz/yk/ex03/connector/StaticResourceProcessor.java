package com.hz.yk.ex03.connector;

import java.io.IOException;

import com.hz.yk.ex03.connector.http.HttpRequest;
import com.hz.yk.ex03.connector.http.HttpResponse;

public class StaticResourceProcessor {
	public void process(HttpRequest request, HttpResponse response) {
		try {
			response.sendStaticResource();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
<<<<<<< HEAD

=======
>>>>>>> 418b11be616716474d0002becda8994592bb6545
}
