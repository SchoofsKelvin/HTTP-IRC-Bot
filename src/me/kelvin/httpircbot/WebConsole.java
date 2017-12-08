package me.kelvin.httpircbot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class WebConsole extends ServerSocket {
	
	public WebConsole() throws IOException {
		super();
	}

	public void start() throws IOException {
		if (isBound()) { return; }
		Integer port = Integer.parseInt(System.getenv("PORT"));
		bind(new InetSocketAddress(port));
		while (true) {
			Socket s = accept();
			new SocketThread(s).start();
		}
	}

	private static int botNumber = 0;
	private static HashMap<Integer,HttpIrcBot> bots = new HashMap<>();
	private static HashMap<String,WebHandler> handlers = new HashMap<>();
	
	private static class HandlerException extends Exception {
		private static final long serialVersionUID = 1L;
		public final int code;
		public HandlerException(int i, String msg) {
			super(msg);
			code = i;
		}
	}
	
	private abstract static class WebHandler {
		protected int botId;
		protected HttpIrcBot bot;
		protected WebConsole console;
		private final boolean needsBot;
		public abstract String handle(BufferedReader reader);
		public WebHandler(boolean needsBot) {
			this.needsBot = needsBot;
		}
		public String handlePls(BufferedReader reader, WebConsole console) throws Exception {
			this.console = console;
			if (needsBot) {
				try {
					if (!reader.ready()) {
						throw new HandlerException(400,"Missing ID");
					}
					botId = Integer.valueOf(reader.readLine());
					bot = bots.get(botId);
				} catch (NumberFormatException e) {
					throw new HandlerException(400,"Missing ID");
				}
				if (bot == null) {
					throw new HandlerException(503,"Bot not found");
				}
			}
			return handle(reader);
		}
	}

	static {
		handlers.put("/Connect", new WebHandler(false) {
			@Override
			public String handle(BufferedReader reader) {
				try {
					botId = botNumber++;
					System.out.println("Connecting bot " + botId);
					bot = new HttpIrcBot(console);
					bots.put(botId, bot);
					bot.doCommands(reader);
					return "Bot: " + botId + "\n" + bot.getOutput();
				} catch (Exception e) {
					return "Error: " + e.getMessage();
				}
			}
		});
		handlers.put("/Handle", new WebHandler(true) {
			@Override
			public String handle(BufferedReader reader) {
				try {
					bot.doCommands(reader);
					if (!bot.isConnected()) {
						bots.remove(botId);
					}
					return bot.getOutput();
				} catch (Exception e) {
					return "Error: " + e.getMessage();
				}
			}
		});
		handlers.put("/Disconnect", new WebHandler(true) {
			@Override
			public String handle(BufferedReader reader) {
				try {
					bots.remove(botId);
					bot.disconnect();
					return "Disconnected";
				} catch (Exception e) {
					return "Error: " + e.getMessage();
				}
			}
		});
	}
	
	public void unregisterBot(int i) {
		if (!bots.containsKey(i)) {
			return;
		}
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				bots.remove(i);
			}
		}, 5000);
	}

	class SocketThread extends Thread {
		private final Socket	socket;
		private HttpExchange	exchange;

		public SocketThread(Socket s) {
			socket = s;
		}

		@Override
		public void run() {
			try {
				exchange = new HttpExchange(socket);
				String response = null;
				try {
					if (exchange.path.equals("/ping")) {
						response = "pong?";
					} else if (exchange.getType().equals(HttpExchange.HttpType.POST)) {
						BufferedReader reader = new BufferedReader(exchange.getRequestBody());
						if (!reader.readLine().equals("Bananas are confirmed!")) {
							response = "Illegal activity detected";
							exchange.sendResponseHeaders(403, response.length());
						} else {
							WebHandler handler = handlers.get(exchange.path);
							if (handler == null) {
								response = "Handler not found";
								exchange.sendResponseHeaders(404, response.length());
							} else {
								response = handler.handlePls(reader,WebConsole.this);
							}
						}
					}
					if (response == null) {
						response = "The page you were looking for... it's gone";
						exchange.sendResponseHeaders(404, response.length());
					} else if (!exchange.sentResponseHeaders()) {
						exchange.sendResponseHeaders(200, response.length());
					}
				} catch (HandlerException e) {
					response = e.getMessage();
					if (!exchange.sentResponseHeaders()) {
						exchange.sendResponseHeaders(e.code, response.length());
					}
				} catch (Exception e) {
					e.printStackTrace();
					response = "Internal server error";
				}
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
