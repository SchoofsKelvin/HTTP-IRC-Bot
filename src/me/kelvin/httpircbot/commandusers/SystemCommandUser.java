package me.kelvin.httpircbot.commandusers;


public class SystemCommandUser extends CommandUserClass {

	@Override
	public void logMsg(String msg) {
		System.out.println(msg);
	}

	@Override
	public void logErr(String err) {
		System.err.println(err);
	}

	@Override
	public void logExc(Exception e) {
		e.printStackTrace();
	}

	@Override
	public String getName() {
		return "[SYSTEM]";
	}
	
	@Override
	public void setChannel(String ch) {
		channel = ch;
	}
}
