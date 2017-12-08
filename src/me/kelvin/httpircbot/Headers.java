package me.kelvin.httpircbot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Headers extends HashMap<String, List<String>> {
	private static final long	serialVersionUID	= -4383174739058477076L;

	public String getFirst(String key) {
		List<String> list = get(key);
		if (list == null) return null;
		if (list.size() == 0) return null;
		return list.get(0);
	}

	public void add(String key, String val) {
		List<String> vals = get(key);
		if (vals == null) {
			vals = new ArrayList<>();
			put(key,vals);
		}
		vals.add(val);
	}

}
