package com.ssttevee.serverbridge.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@SuppressWarnings("unchecked")
public class UserDatabase {
	private static final String jsonFile = "/home/" + System.getProperty("user.name") + "/users.json";

	public static UserDatabase instance = new UserDatabase();

	private JSONArray obj;

	public UserDatabase() {
		File f = new File(jsonFile);
		if(f.exists() && !f.isDirectory())
			load();
		else
			obj = new JSONArray();
	}

	public boolean isUserAdmin(String uid) {
		for(int i = 0; i < obj.size(); i++)
			if(((JSONObject)obj.get(i)).get("uid").equals(uid))
				if(((JSONObject)obj.get(i)).get("type").equals("admin")) {
					return true;
				} else {
					return false;
				}
		return false;
	}

	public void addUser(String name, String uid, String type) {
		JSONObject newSong = new JSONObject();
		newSong.put("name", name);
		newSong.put("uid", uid);
		newSong.put("type", type);
		obj.add(newSong);
		save();
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
}
