package com.ssttevee.serverbridge.util.apis;

import com.ssttevee.serverbridge.util.JSONReader;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URL;

public class SoundCloud extends API{

	private final static String CLIENT_ID = "c0966e3f24bf6a2fc573dde3f306c56a";
	private final static String CLIENT_SECRET = "1f21e14db440ead9de3d8d23aba96372";

	JSONObject track;

	public SoundCloud(String id) throws IOException {
		track = JSONReader.readJsonFromUrl("https://api.soundcloud.com/tracks/" + id + ".json?client_id=" + CLIENT_ID);
	}

	public SoundCloud(URL url) throws IOException {
		track = JSONReader.readJsonFromUrl("http://api.soundcloud.com/resolve.json?url=" + url.toExternalForm() + "&client_id=" + CLIENT_ID);
	}

	public String getService() {
		return "soundcloud";
	}

	public String getTitle() {
		if(track == null) return "null";
		return (String) track.get("title");
	}

	public String getId() {
		if(track == null) return "null";
		return track.get("title") + "";
	}

	public String getFileName() {
		if(track == null) return "null";
		return "soundcloud/" + getId() + ".mp3";
	}

}
