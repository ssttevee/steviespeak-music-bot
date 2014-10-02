package com.ssttevee.serverbridge.util;

import com.ssttevee.serverbridge.util.apis.API;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

@SuppressWarnings("unchecked")
public class MusicDatabase {
	private static final String jsonFile = "/home/" + System.getProperty("user.name") + "/songs.json";

	public static MusicDatabase instance = new MusicDatabase();

	private JSONArray obj;

	public MusicDatabase() {
		File f = new File(jsonFile);
		if(f.exists() && !f.isDirectory())
			load();
		else
			obj = new JSONArray();
	}

	public JSONObject addSong(API api) {
		return addSong(api.getFileName(), api.getTitle());
	}

	public JSONObject addSong(String filename, String songName) {
		JSONObject newSong = new JSONObject();
		newSong.put("filename", filename);
		newSong.put("song_name", songName);

		JSONObject lastSong = (JSONObject) obj.get(obj.size() - 1);
		newSong.put("id", (((Long) lastSong.get("id")) + 1));
		obj.add(newSong);
		save();
		return newSong;
	}

	public void delSong(JSONObject song) {
		obj.remove(song);
		save();
	}

	public void renameSong(JSONObject song, String newName) {
		int index = obj.indexOf(song);
		song.put("song_name", newName);
		obj.set(index, song);
		save();
	}

	public JSONObject getRandomSong() {
		return (JSONObject) obj.get(new Random().nextInt((obj.size()) + 1));
	}

	public JSONArray getAllSongs() {
		return obj;
	}

	public JSONArray searchSongsByName(String songName) {
		JSONArray songs = new JSONArray();
		for(int i = 0; i < obj.size(); i++) {
			JSONObject song = (JSONObject) obj.get(i);
			if(((String) song.get("song_name")).toLowerCase().contains(songName.toLowerCase())) {
				songs.add(song);
			}
		}
		return songs;
	}

	public JSONObject findSongByName(String songName) {
		for(int i = 0; i < obj.size(); i++) {
			JSONObject song = (JSONObject) obj.get(i);
			if(((String) song.get("song_name")).contains(songName)) {
				return song;
			}
		}
		return null;
	}

	public JSONArray searchSongsByFile(String filename) {
		JSONArray songs = new JSONArray();
		for(int i = 0; i < obj.size(); i++) {
			JSONObject song = (JSONObject) obj.get(i);
			if(((String) song.get("filename")).contains(filename)) {
				songs.add(song);
			}
		}
		return songs;
	}

	public JSONObject findSongByFile(String filename) {
		for(int i = 0; i < obj.size(); i++) {
			JSONObject song = (JSONObject) obj.get(i);
			if(((String) song.get("filename")).contains(filename)) {
				return song;
			}
		}
		return null;
	}

	public JSONArray searchSongsByUrl(String url) {
		try {
			API api = API.getApi(new URL(url));
			return searchSongsByFile(api.getFileName());
		} catch(MalformedURLException e) {
			return new JSONArray();
		}
	}

	public JSONObject findSongByUrl(String url) {
		try {
			API api = API.getApi(new URL(url));
			return findSongByFile(api.getFileName());
		} catch(MalformedURLException e) {
			return null;
		}
	}

	public boolean isSongExist(String url) {
		try {
			API api = API.getApi(new URL(url));
			if(findSongByFile(api.getFileName()) == null) {
				return false;
			} else {
				return true;
			}
		} catch(MalformedURLException e) {
			return false;
		}
	}

	public JSONObject findSongById(int index) {

		for(int i = 0; i < obj.size(); i++) {
			JSONObject song = (JSONObject) obj.get(i);
			if((song.get("id") + "").equals(index + "")) {
				return song;
			}
		}
		return null;
	}

	public void save() {
		try {
			FileWriter file = new FileWriter(jsonFile);
			file.write(obj.toString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load() {

		JSONParser parser = new JSONParser();

		try {
			obj = (JSONArray) parser.parse(new FileReader(jsonFile));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void upgradeDb() {
		for(int i = 0; i < obj.size(); i++) {
			JSONObject song = (JSONObject) obj.get(i);
			if(!song.containsKey("id")) {
				song.put("id", i);
				obj.set(i, song);
			}
		}
		save();
	}

}
