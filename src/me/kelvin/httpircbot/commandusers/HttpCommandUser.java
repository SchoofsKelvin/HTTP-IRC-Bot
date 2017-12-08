package me.kelvin.httpircbot.commandusers;

import java.util.List;

public class HttpCommandUser extends CommandUserClass {
	
	private final List<String> buffer;
	
	public HttpCommandUser(List<String> buffer) {
		this.buffer = buffer;
	}
	
	private StringBuilder output = new StringBuilder();

	private void println(String line) {
		output.append(line);
		output.append("\n");
	}
	
	@Override
	public void logMsg(String msg) {
		println(msg);
		buffer.add("logMsg:" + msg);
	}

	@Override
	public void logErr(String err) {
		println(err);
		buffer.add("logErr:" + err);
	}

	@Override
	public void logExc(Exception trace) {
		logErr(trace.toString());
		for (StackTraceElement e : trace.getStackTrace()) {
			logErr("	at " + e.toString());
		}
	}

	@Override
	public String getName() {
		return "[HTTP]";
	}
	
	@Override
	public void setChannel(String ch) {
		channel = ch;
	}

	public String getOutput() {
		return output.toString();
	}
	
	@Override
	public boolean isAdmin() {
		return true;
	}
}
