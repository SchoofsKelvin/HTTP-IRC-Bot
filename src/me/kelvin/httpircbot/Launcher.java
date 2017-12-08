package me.kelvin.httpircbot;

public class Launcher {

	public static void main(String[] args) throws Exception {
		WebConsole console = new WebConsole();
		try {
			console.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		console.close();
	}

}
