package me.kelvin.httpircbot;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.PircBot;

import me.kelvin.httpircbot.commandusers.CommandUser;
import me.kelvin.httpircbot.commandusers.HttpCommandUser;
import me.kelvin.httpircbot.commandusers.IrcUser;
import me.kelvin.httpircbot.commandusers.SystemCommandUser;

public class HttpIrcBot extends PircBot {

	public static boolean OFFLINE = false;
	public static boolean SILENT = false;
	
	private final static ArrayList<HttpIrcBot> bots = new ArrayList<>();
	private final static SystemCommandUser systemUser = new SystemCommandUser();
	private final static ArrayList<String> globalcmds = new ArrayList<>();
	
	private final ArrayList<CommandUser> users = new ArrayList<>();
	private final ArrayList<String> outputBuffer = new ArrayList<>(100);
	
	private String preferedNick = "TestBot";
	
	private long lastTick;
	private boolean fullyConnected = false;
	
	static {
		globalcmds.add("select");
		globalcmds.add("sendall");
		globalcmds.add("send");
		globalcmds.add("save");
		globalcmds.add("load");
		globalcmds.add("exit");
		globalcmds.add("raw");
	}

	public HttpIrcBot(WebConsole webHandler) throws Exception {
		this(new String[0]);
		bots.add(this);
	}

	public HttpIrcBot(String[] args) throws Exception {
		for (String arg : args) {
			if (arg.equalsIgnoreCase("offline")) {
				OFFLINE = true;
			} else if (arg.equalsIgnoreCase("silent")) {
				SILENT = true;
			}
		}
		new StartBot().start();
	}
	
	private class StartBot extends Thread {
		@Override
		public void run() {

			if (!OFFLINE) {
				try {
					load(systemUser);
					setVerbose(true);
					HttpIrcBot.this.setName("TestBot");
					setLogin("[secure]");
					setAutoNickChange(true);
					connect("irc.swiftirc.net", 6667); // SSL=6697, Normal=6667
					sendMessage("NickServ","identify [secure]");
					output("Nick changed to " + getNick());
					setMode(getNick(), "+B");
					fullyConnected = true;
				} catch (Exception e1) {
					e1.printStackTrace();
					return;
				}
			}

			float time;
			long timeStart, timeEnd;

			/*new Thread() {
				@Override
				public void run() {
					BufferedReader inp = new BufferedReader(new InputStreamReader(System.in));
					while (isConnected() || OFFLINE) {
						try {
							doCommand(inp.readLine(), systemUser);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();*/
			
			lastTick = System.currentTimeMillis() + 10000;

			while (isConnected() || OFFLINE) {
				timeStart = System.nanoTime();
				
				if (System.currentTimeMillis() > lastTick) {
					quitServer("HTTP Ping Timeout");
				}

				timeEnd = System.nanoTime();
				time = (timeEnd - timeStart) / 1000000.0f;

				/*
				 * Main loop ticks only once per second.
				 */
				try {
					if (time < 1000.0f) {
						Thread.sleep((long) (1000.0f - time));
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static class NoPermissionException extends Exception {
		private static final long	serialVersionUID	= 5038776910259575273L;
	};

	private static void checkPermission(CommandUser user, String typ) throws Exception {
		if (user.isOperator() && !typ.equals("admin")) { return; }
		if (!user.isAdmin()) { throw new NoPermissionException(); }
	}

	private static String mergeStrings(String[] list) {
		if (list.length == 0) {
			return null;
		} else if (list.length == 1) { return list[0]; }
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < list.length - 1; i++) {
			if (i > 0) {
				res.append(", ");
			}
			res.append(list[i]);
		}
		res.append(" and ");
		res.append(list[list.length - 1]);
		return res.toString();
	}

	@SuppressWarnings("unused")
	private static String mergeStrings(ArrayList<String> list) {
		return mergeStrings(list.toArray(new String[0]));
	}

	public void doCommand(String line, CommandUser user) {
		if (user == null) { return; }
		boolean announce = !line.startsWith("!");
		line = announce ? line : line.substring(1);
		String[] parts = line.split(" ");
		if (parts.length == 0) { return; }
		try {
			switch (parts[0].toLowerCase()) {
				// ADMIN
				case "join":
					checkPermission(user, "admin");
					if (parts.length != 2 && parts.length != 3) {
						user.logErr("Usage: join <channel> (password)");
						break;
					} else if (parts.length == 2) {
						joinChannel(parts[1]);
					} else {
						joinChannel(parts[1], parts[2]);
					}
					break;
				/*case "say":
					checkPermission(user, "admin");
					if (parts.length == 1) {
						user.logErr("Usage: say <message>");
						break;
					}
					String msg = line.substring(4);
					for (String ch : getChannels()) {
						sendMessage(ch,msg);
					}
					break;*/
				case "whisper":
					checkPermission(user, "admin");
					if (parts.length < 3) {
						user.logErr("Usage: whisper <user> <message>");
						break;
					}
					String whispertarget = parts[1];
					line = line.substring(9 + whispertarget.length());
					sendMessage(whispertarget, line);
					break;
				case "raw":
					if (parts.length == 1) {
						user.logErr("Usage: raw <message>");
						break;
					}
					checkPermission(user, "admin");
					sendRawLineViaQueue(line.substring(4));
					break;
				case "send":
					checkPermission(user, "admin");
					if (parts.length < 3) {
						user.logErr("Usage: send <channel> <message>");
						break;
					}
					String target = parts[1];
					line = line.substring(6 + target.length());
					//target = target.startsWith("#") ? target : "#" + target;
					sendMessage(target, line);
					break;
				case "sendall":
					checkPermission(user, "admin");
					if (parts.length < 2) {
						user.logErr("Usage: sendall <message>");
						break;
					}
					line = line.substring(8);
					for (String cha : getChannels()) {
						sendMessage(cha, line);
					}
					break;
				case "exit":
					checkPermission(user, "admin");
					if (parts.length == 1) {
						quitServer();
					} else {
						quitServer(line.substring(5));
					}
					break;
				case "nick":
					checkPermission(user, "admin");
					if (parts.length != 2) {
						user.logErr("Usage: nick <nick>");
						break;
					}
					changePreferedNick(parts[1]);
					break;
				/*
					// System
				case "sudo":
					checkPermission(user, "admin");
					if (parts.length < 3) {
						user.logErr("Usage: sudo <user> <message>");
						break;
					}
					String sender = parts[1];
					line = line.substring(6 + sender.length());
					CommandUser sudoer = getOrCreateUser(sender);
					doCommand(line, sudoer);
					break;
				case "save":
					checkPermission(user, "admin");
					try {
						save(user);
						user.logMsg("Bot stuff saved");
					} catch (Exception e) {
						user.logErr("Couldn't save:");
						user.logExc(e);
					}
					break;
				case "load":
					checkPermission(user, "admin");
					try {
						load(user);
						user.logMsg("Bot stuff loaded");
					} catch (Exception e) {
						user.logErr("Couldn't load:");
						user.logExc(e);
					}
					break;
				case "commands":
				case "help":
				case "?":
					if (user instanceof CommandUser) {
						user.logErr("Too lazy to finish the help thing");
						break;
					}
					if (user instanceof IrcUser) {
						if (user.isOperator()) {
							user.logMsg("Mod commands: bets, add, remove, openbets, closebets, clearbets");
						} else {
							user.logMsg("Normal users can only do '!bet <amount>' when betting is enabled");
						}
						break;
					}
					user.logMsg("select <channel>: Select a channel for 'say'");
					user.logMsg("say <message>: Send a message to the selected channel");
					user.logMsg("send <channel> <message>: Send a message to a channel");
					user.logMsg("sendall <message>: Send a message to all channels");
					user.logMsg("bets: List all current bets and if betting");
					user.logMsg("add <name> <bet>: Add a bet for someone");
					user.logMsg("remove <name>: Remove someone's bet");
					user.logMsg("openbets: Allow people to bet");
					user.logMsg("closebets: Stop people from betting");
					user.logMsg("clearbets: Clear all current bets");
					user.logMsg("sudo ...: Make the bot think someone said something");
					user.logMsg("save: Save current bets");
					user.logMsg("load: Load saved bets");
					user.logMsg("exit: Save and exit");
					break;*/
				default:
					if (user instanceof IrcUser) {
						if (((IrcUser) user).isWhisper()) {
							user.logErr("Couldn't find that command! Try 'help' for a list.");
						}
					} else {
						user.logErr("Couldn't find that command!");
						user.logMsg("Try 'help' for a list of commands");
					}
			}
		} catch (NumberFormatException e) {
			user.logErr("Can you use proper numbers, please?");
		} catch (NoPermissionException e) {
			user.logErr("You should only use commands you have access to...");
		} catch (Exception e) {
			e.printStackTrace();
			user.logErr("Error while processing command:");
			user.logExc(e);
		}
	}
	
	public CommandUser getUser(String name) {
		for (CommandUser user : users) {
			if (user.getName().equalsIgnoreCase(name)) {
				return user;
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends CommandUser> T getUserByClass(String name, Class<? extends CommandUser> T) {
		for (CommandUser user : users) {
			if (user.getName().equalsIgnoreCase(name)) {
				if (T.isInstance(user)) {
					return (T) user;
				}
			}
		}
		return null;
	}

	public IrcUser getOrCreateUser(String name) {
		IrcUser user = getUserByClass(name,IrcUser.class);
		if (user != null) { return user; }
		user = new IrcUser(this, name, "", null);
		users.add(user);
		user.setWhisper(true);
		return user;
	}

	public void save(CommandUser user) throws Exception {
		//user.logErr("Not supporting saving right now");
	}

	public void load(CommandUser user) throws Exception {
		//user.logErr("Not supporting loading right now");
	}

	public String doCommands(BufferedReader reader) throws Exception {
		while (!fullyConnected) {
			Thread.sleep(100);
		}
		HttpCommandUser user = new HttpCommandUser(outputBuffer);
		lastTick = System.currentTimeMillis() + 20000;
		String line;
		while (reader.ready() && (line = reader.readLine()) != null) {
			doCommand(line,user);
		}
		return user.getOutput();
	}

	public String getOutput() {
		StringBuilder builder = new StringBuilder(outputBuffer.size()*50);
		for (String str : outputBuffer) {
			builder.append(str);
			builder.append("\n");
		}
		outputBuffer.clear();
		return builder.toString();
	}
	
	public void output(String msg) {
		outputBuffer.add(msg);
		//System.out.println(msg);
	}
	
	public void changePreferedNick(String newNick) {
		preferedNick = newNick;
		changeNick(newNick);
	}
	
	@Override
	public void quitServer(String reason) {
		if (!getNick().equalsIgnoreCase(getName())) {
			sendMessage("NickServ","DROP " + getNick());
		}
		super.quitServer(reason);
	}
	
	@Override
	protected void onServerResponse(int code, String response) {
		if (code == ERR_NICKNAMEINUSE) {
			String[] parts = response.split(" ");
			String old = parts[0], nick = parts[1];
			if (!old.equals(getNick())) return;
			changeNick(nick + "_");
			System.err.println("Couldn't change nick from " + old + " to " + nick + ". Trying " + nick + "_");
		} else if (code == ERR_CANNOTSENDTOCHAN && response.contains("(+r)")) {
			//sendMessage("NickServ","GROUP [secure] [secure]");
		}
	}
	
	// EVENTS
	@Override
	protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
		if (newNick.equals(getNick())) {
			/*if (!oldNick.equalsIgnoreCase(getName())) {
				sendMessage("NickServ", "DROP " + oldNick);
			}*/
			output("Nick changed to " + newNick);
			/*if (newNick.equalsIgnoreCase(getName())) {
				sendMessage("NickServ","IDENTIFY [secure]");
			} else {
				sendMessage("NickServ","GROUP [secure] [secure]");
			}*/
		} else if (!login.equals(getLogin())) {
			output("EVENT: onNickChange:" + oldNick + ":" + newNick);
		} else if (oldNick.equalsIgnoreCase(preferedNick)) {
			changeNick(preferedNick);
		}
	}
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname) {
		if (sender.equals(getNick())) {
			output("We joined " + channel);
			return;
		}
		output("EVENT: onJoin:" + channel + ":" + sender);
	}
	
	@Override
	protected void onDisconnect() {
		output("EVENT: onDisconnect");
	}
	
	@Override
	protected void onKick(String ch, String nick, String login, String host, String rNick, String reason) {
		if (rNick.equals(getNick())) {
			output("We left " + ch);
			bots.remove(this);
			return;
		}
		output("EVENT: onPart:" + ch + ":" + rNick);
	}
	
	@Override
	protected void onPart(String ch, String sender, String login, String hostname) {
		if (sender.equals(getNick())) {
			output("We left " + ch);
			return;
		}
		output("EVENT: onPart:" + ch + ":" + sender);
	}
	
	@Override
	protected void onMessage(String ch, String sender, String login, String hostname, String message) {
		output("EVENT: onMessage:" + ch + ":" + sender + ":" + message);
	}
	
	@Override
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
		output("EVENT: onQuit:" + sourceNick + ":" + reason);
		if (sourceNick.equalsIgnoreCase(preferedNick)) {
			changeNick(preferedNick);
		}
	}
	
	@Override
	protected void onAction(String sender, String login, String hostname, String target, String action) {
		output("EVENT: onAction:" + target + ":" + sender + ":" + action);
	}
	
	private ArrayList<String> nicks;
	
	@Override
	protected void onNotice(String nick, String login, String hostname, String target, String notice) {
		if (target.equalsIgnoreCase(getNick())) {
			if (nick.equalsIgnoreCase("nickserv")) {
				if (notice.contains("Please wait 30 seconds before using the GROUP")) {
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							sendMessage("NickServ","GROUP [secure] [secure]");
						}
					}, 30000);
				} else if (notice.contains("There are too many nicks")) {
					changePreferedNick(getName());
					new Thread() {
						@Override
						public void run() {
							while (!getNick().equalsIgnoreCase(getName())) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							sendMessage("NickServ","IDENTIFY [secure]");
							sendMessage("NickServ","GLIST");
						};
					}.start();
				} else if (notice.contains("List of nicknames in your group")) {
					nicks = new ArrayList<>(5);
				} else if (nicks != null && notice.contains("nicknames in group")) {
					ArrayList<String> grouped = nicks;
					nicks = null;
					for (String groupedNick : grouped) {
						if (!groupedNick.equalsIgnoreCase(getName())) {
							sendMessage("NickServ","DROP " + groupedNick);
						}
					}
					sendMessage("NickServ","GROUP [secure] [secure]");
				} else if (nicks != null) {
					nicks.add(notice.substring(4));
				}
			}
		}
	}
}
