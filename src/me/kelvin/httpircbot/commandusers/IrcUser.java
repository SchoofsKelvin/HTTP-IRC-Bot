package me.kelvin.httpircbot.commandusers;

import me.kelvin.httpircbot.HttpIrcBot;

public class IrcUser extends CommandUserClass {

	private String	name;
	private String	m_prefix;
	private int		m_cmd_timer;
	private boolean	m_urlpermit;
	private boolean	whisper;

	public IrcUser(HttpIrcBot bot, String username, String prefix, String ch) {
		this.bot = bot;
		name = username;
		m_prefix = prefix;
		channel = ch;
		m_cmd_timer = 0;
		m_urlpermit = false;
		whisper = false;
	}

	public IrcUser(HttpIrcBot bot, String name, String channel) {
		this(bot, name, "", channel);
	}

	@Override
	public String toString() {
		return "TwitchUser[" + m_prefix + name + ", " + m_cmd_timer + "]";
	}

	public void addPrefixChar(String prefix) {
		m_prefix = prefix + m_prefix;
	}

	public void delPrefixChar(String prefix) {
		m_prefix.replace(prefix, "");
	}

	@Override
	public boolean isOperator() {
		if (channel == null) { return false; }
		if (channel.equals("#myhome")) { return true; }
		return isAdmin() || m_prefix.contains("@");
	}

	@Override
	public boolean isAdmin() {
		return m_prefix.contains("&");
	}

	@Override
	public String getName() {
		return name;
	}

	public String getPrefix() {
		return m_prefix;
	}

	public int getCmdTimer() {
		return m_cmd_timer;
	}

	public boolean getUrlPermit() {
		return m_urlpermit;
	}

	public void setPrefix(String prefix) {
		m_prefix = prefix;
	}

	public void setCmdTimer(int time) {
		m_cmd_timer = time;
	}

	public void setUrlPermit(boolean urlpermit) {
		m_urlpermit = urlpermit;
	}

	protected void println(String msg) {
		bot.sendMessage(name, msg);
	}

	@Override
	public void logMsg(String msg) {
		println(msg);
	}

	@Override
	public void logErr(String err) {
		println(err);
	}

	@Override
	public void logExc(Exception trace) {
		println(trace.toString());
		for (StackTraceElement e : trace.getStackTrace()) {
			println("	at " + e.toString());
		}
	}

	public boolean isWhisper() {
		return whisper;
	}

	public void setWhisper(boolean b) {
		whisper = b;
	}

	@Override
	public void setChannel(String ch) {
		channel = ch;
	}

}
