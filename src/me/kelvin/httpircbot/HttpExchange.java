package me.kelvin.httpircbot;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map.Entry;

public class HttpExchange {

	private final Headers reqheaders;
	private final Headers resheaders;
	private final Socket socket;
	public final HttpType type;
	public final String path;
	private final InputStreamReader reader;
	private boolean sent;
	
	public HttpExchange (Socket s) throws Exception {
		reqheaders = new Headers();
		resheaders = new Headers();
		socket = s;
		reader = new InputStreamReader(s.getInputStream());
		if (!reader.ready()) {
			Thread.sleep(100);
		}
		String[] first = readLine(reader).split(" ");
		if (first.length != 3) {
			System.out.println("First line of HTTP request:");
			for (String str : first) {
				System.out.print(str);
			}
			System.out.println("");
			throw new Exception("Invalid HTTP Header");
		} else if (first[0].equals("GET")) {
			type = HttpType.GET;
		} else if (first[0].equals("POST")) {
			type = HttpType.POST;
		} else {
			type = HttpType.UNKNOWN;
			sendResponseHeaders(501,0);
			s.close();
			throw new Exception("Invalid HTTP Method");
		}
		path = first[1];
		String line;
		while (!(line = readLine(reader)).isEmpty()) {
			int pos = line.indexOf(":");
			String key = line.substring(0, pos);
			String val = line.substring(pos + 2);
			//System.out.println("Header: " + key + " = " + val);
			reqheaders.add(key, val);
		}
		//String body = readAll(reader);
		//System.out.println("Body: " + body);
		//System.out.println("WebSocket: " + (websocket ? "Yes" : "No"));
	}

	private static String readLine(InputStreamReader in) throws IOException {
		StringBuilder res = new StringBuilder(); int i;
		while (in.ready() && (i = in.read()) != -1) {
			if (i == 10) {
				break;
			} else if (i != 13) {
				res.append((char) i);
			}
		}
		return res.toString();
	}
	
	@SuppressWarnings("unused")
	private static String readAll(InputStreamReader in) throws IOException {
		StringBuilder res = new StringBuilder(); int i;
		while (in.ready() && (i = in.read()) != -1) {
			res.append((char) i);
		}
		return res.toString();
	}

	public Headers getRequestHeaders() {
		return reqheaders;
	}
	public Headers getResponseHeaders() {
		return resheaders;
	}
	
	public void write(String str) throws IOException {
		socket.getOutputStream().write(str.getBytes());
	}

	public void sendResponseHeaders(int code, int length) throws Exception {
		if (sent) {
			throw new Exception("Response Headers already sent");
		}
		sent = true;
		String typ = "UNKNOWN CODE";
		switch (code) {
			case 200:
				typ = "OK";
				break;
			case 202:
				typ = "Accepted";
				break;
			case 301:
				typ = "Moved Permanently";
				break;
			case 302:
				typ = "Found";
				break;
			case 400:
				typ = "Bad Request";
				break;
			case 401:
				typ = "Unauthorized";
				break;
			case 403:
				typ = "Forbidden";
				break;
			case 404:
				typ = "Not Found";
				break;
			case 405:
				typ = "Method Not Allowed";
				break;
			case 500:
				typ = "Internal Server Error";
				break;
		}
		write("HTTP/1.1 " + code + " " + typ + "\n");
		for (Entry<String,List<String>> e : resheaders.entrySet()) {
			for (String v : e.getValue()) {
				write(e.getKey() + ": " + v + "\n");
			}
		}
		write("\n");
	}

	public boolean sentResponseHeaders() {
		return sent;
	}

	public OutputStream getResponseBody() throws IOException {
		return socket.getOutputStream();
	}
	
	public InputStreamReader getRequestBody() throws IOException {
		return reader;
	}
	
	public HttpType getType() {
		return type;
	}
	
	public enum HttpType {
		UNKNOWN, GET, POST
	}
	
}
