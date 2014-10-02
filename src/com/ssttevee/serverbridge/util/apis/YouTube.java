package com.ssttevee.serverbridge.util.apis;

import com.ssttevee.serverbridge.util.JSONReader;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTube extends API {

	String videoId = "0";

	public YouTube(String id) throws IOException {
		videoId = id;
	}

	public YouTube(URL url) throws IOException {
		Pattern pattern = Pattern.compile(".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");
		Matcher matcher = pattern.matcher(url.toExternalForm());
		if (matcher.matches()) {
			videoId = matcher.group(1);
		}
	}

	public String getService() {
		return "youtube";
	}

	public String getTitle() {
		if(videoId == "0") return "null";
		try {
			JSONObject json = (JSONObject) JSONReader.readJsonFromUrl("http://gdata.youtube.com/feeds/api/videos/" + videoId + "?v=2&alt=json&fields=title").get("entry");
			json = (JSONObject) json.get("title");
			return (String) json.get("$t");
		} catch(Exception e) {
			e.printStackTrace();
			return "null";
		}
	}

	public String getId() {
		return videoId;
	}

	public String getFileName() {
		return "youtube/" + videoId + ".mp3";
	}

}
