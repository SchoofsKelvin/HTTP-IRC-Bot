package me.kelvin.httpircbot.commandusers;

import me.kelvin.httpircbot.HttpIrcBot;

public abstract class CommandUserClass implements CommandUser {

	protected String	channel;
	protected HttpIrcBot bot;

	@Override
	public abstract void logMsg(String msg);

	@Override
	public void logErr(String err) {
		logMsg(err);
	}

	@Override
	public void logExc(Exception trace) {
		logErr(trace.toString());
		for (StackTraceElement e : trace.getStackTrace()) {
			logErr("	at " + e.toString());
		}
	}

	@Override
	public String getChannel() {
		return channel;
	}

	@Override
	public void setChannel(String ch) {
		channel = ch;
	}

	@Override
	public boolean isAdmin() {
		return true;
	}

	@Override
	public boolean isOperator() {
		return true;
	}

	@Override
	public void logOrAnnounceMsg(String msg, boolean announce) {
		if (announce && channel != null && bot != null) {
			bot.sendMessage(channel, msg);
		} else {
			logMsg(msg);
		}
	}

	@Override
	public void logOrAnnounceErr(String msg, boolean announce) {
		if (announce && channel != null && bot != null) {
			bot.sendMessage(channel, msg);
		} else {
			logErr(msg);
		}
	}

}
