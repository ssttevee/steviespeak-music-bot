package com.ssttevee.serverbridge.util.apis;

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class API {

	private static HashMap<String,Class<? extends API>> classes = new HashMap<String,Class<? extends API>>();

	static {
		classes.put("youtube", YouTube.class);
		classes.put("soundcloud", SoundCloud.class);
	}

	public static API getApi(String service, String id) {
		try {
			Class<? extends API> cls = classes.get(service);
			Constructor<? extends API> clsConstructor = cls.getConstructor(String.class);
			return clsConstructor.newInstance(id);
		} catch(Exception e) {
			return null;
		}
	}

	public static API getApi(URL url) {
		try {
			Class<? extends API> cls = classes.get(getServiceByUrl(url));
			Constructor<? extends API> clsConstructor = cls.getConstructor(URL.class);
			return clsConstructor.newInstance(url);
		} catch(Exception e) {
			return null;
		}
	}

	private static String getServiceByUrl(URL url) {
		String service = "";
		Pattern p = Pattern.compile(".*?([^.]+\\.[^.]+)");

		Matcher m = p.matcher(url.getHost());
		if (m.matches()) {
			service = m.group(1).split("\\.")[0];
		}

		if(service.contains("youtu")) service = "youtube";

		return service;
	}

	public abstract String getService();
	public abstract String getTitle();
	public abstract String getId();
	public abstract String getFileName();

}
