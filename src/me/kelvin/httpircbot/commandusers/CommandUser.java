package me.kelvin.httpircbot.commandusers;

public abstract interface CommandUser {

	public abstract void logMsg(String msg);
	public abstract void logErr(String err);
	public abstract void logExc(Exception e);
	public abstract String getChannel();
	public abstract String getName();
	public abstract boolean isAdmin();
	public abstract boolean isOperator();
	public abstract void logOrAnnounceMsg(String msg, boolean announce);
	public abstract void logOrAnnounceErr(String msg, boolean announce);
	public abstract void setChannel(String ch);
}
