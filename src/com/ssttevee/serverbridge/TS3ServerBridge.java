package com.ssttevee.serverbridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

import com.ssttevee.serverbridge.util.MusicDatabase;
import com.ssttevee.serverbridge.util.RandomString;
import com.ssttevee.serverbridge.util.UserDatabase;
import com.ssttevee.serverbridge.util.apis.API;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TeamspeakActionListener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class TS3ServerBridge implements TeamspeakActionListener
{
	JTS3ServerQuery query;
	String authcode = null;
	boolean radiomode = false;
	
	public static void main(String[] args)
	{
		TS3ServerBridge jts3 = new TS3ServerBridge();
		jts3.runServerMod();
	}

	void echoError() {
		String error = query.getLastError();
		if (error != null) {
			System.out.println(error);
			if (query.getLastErrorPermissionID() != -1) {
				HashMap<String, String> permInfo = query.getPermissionInfo(query.getLastErrorPermissionID());
				if (permInfo != null) {
					System.out.println("Missing Permission: " + permInfo.get("permname"));
				}
			}
		}
	}
	
	public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo) {
		if (eventType.equals("notifytextmessage") && eventInfo.get("msg").startsWith("!")) {
			String[] args = eventInfo.get("msg").substring(1).split(" ");
			String cmd = args[0];

			// System.out.println(cmd + "|" + args.toString());

			if(cmd.equalsIgnoreCase("quitbot")) {
				query.sendTextMessage(1, 3, "Music Bot Stopping");
				query.removeTeamspeakActionListener();
				query.closeTS3Connection();

				System.exit(0);
			} else if(cmd.equalsIgnoreCase("skip")) {
				try {
					String[] commands = {"bash", "-c", "mpc next"};
					Runtime.getRuntime().exec(commands);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if(cmd.equalsIgnoreCase("db")) {
				StringBuilder builder = new StringBuilder();
				JSONArray allSongs;

				if(args.length >= 3 && args[1].equalsIgnoreCase("search")) {
					String searchQuery = "";
					for(int i = 2; i < args.length; i++) {
						if(i > 2) searchQuery += " ";
						searchQuery += args[i];
					}

					allSongs = MusicDatabase.instance.searchSongsByName(searchQuery);

					builder.append("There were [b]");
					builder.append(allSongs.size());
					builder.append("[/b] song(s) found matching \"");
					builder.append(searchQuery);
					builder.append("\":");
				} else if(args.length >= 3 && args[1].equalsIgnoreCase("lookup")) {
					JSONObject song = MusicDatabase.instance.findSongById(Integer.parseInt(args[2]));

					allSongs = new JSONArray();
					builder.append("ID [color=red]" + args[2] + "[/color] corresponds to ");

					if(song == null) {
						builder.append("nothing...");
					} else {
						allSongs.add(song);
					}
				} else {
					allSongs = MusicDatabase.instance.getAllSongs();

					builder.append("There are [b]");
					builder.append(allSongs.size());
					builder.append("[/b] song(s) in the db:");
				}

				for(int i = 0; i < allSongs.size(); i++) {
					JSONObject song = (JSONObject) allSongs.get(i);
					builder.append("\n[color=red]");
					builder.append(String.format("%04d", Integer.parseInt(song.get("id") + "")));
					builder.append("[/color] - [u]");
					builder.append(song.get("song_name"));
					builder.append("[/u]");

					if(builder.toString().length() >= 900) {
						sendToInvoker(eventInfo, builder.toString());
						builder = new StringBuilder();
					}
				}

				sendToInvoker(eventInfo, builder.toString());
			} else if(cmd.equalsIgnoreCase("dl")) {
				String link = args[1];

				if(!link.contains("[URL]")) {
					sendToInvoker(eventInfo, "\nUsage: !dl [song_url]\n\n\tSong URL can be youtube or soundcloud links");
					return;
				}

				link = link.substring(5, link.length() - 6);

				if(MusicDatabase.instance.isSongExist(link)) {
					sendToInvoker(eventInfo, MusicDatabase.instance.findSongByUrl(link).get("song_name") + " has already been downloaded");
					return;
				}

				try {
					API api = API.getApi(new URL(link));

					sendToInvoker(eventInfo, "Downloading " + link);

					String[] commands = {"bash", "-c", "youtube-dl -o \"/home/$USER/music/" + api.getService() + "/%(id)s.%(ext)s\" --extract-audio --audio-format mp3 " + link};
					Process p = Runtime.getRuntime().exec(commands);
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					String line;
					while((line = reader.readLine()) != null) {
						sendToInvoker(eventInfo, line);
					}

					JSONObject song = MusicDatabase.instance.addSong(api);

					sendToInvoker(eventInfo, "Downloaded [u]" + song.get("song_name") + "[/u] as [color=red]" + song.get("id") + "[/color]");
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if(cmd.equalsIgnoreCase("del")) {
				if(UserDatabase.instance.isUserAdmin(eventInfo.get("invokeruid"))) {
					if(args.length < 2) return;
					JSONObject song = MusicDatabase.instance.findSongById(Integer.parseInt(args[1]));
					MusicDatabase.instance.delSong(song);
					try {
						String[] commands = {"bash", "-c", "rm -f ~/" + song.get("filename")};
						Process p = Runtime.getRuntime().exec(commands);
						p.waitFor();

						sendToInvoker(eventInfo, "[u]" + song.get("song_name") + "[/u] was deleted");
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else {
					sendToInvoker(eventInfo, "Permission denied");
				}
			} else if(cmd.equalsIgnoreCase("rename")) {
				if(UserDatabase.instance.isUserAdmin(eventInfo.get("invokeruid"))) {
					if(args.length < 3) {
						sendToInvoker(eventInfo, "Song cannot have no name");
						return;
					}
					JSONObject song = MusicDatabase.instance.findSongById(Integer.parseInt(args[1]));
					String newName = "";

					for(int i = 2; i < args.length; i++) {
						if(i > 2) newName += " ";
						newName += args[i];
					}

					MusicDatabase.instance.renameSong(song, newName);
					sendToInvoker(eventInfo, "[u]" + song.get("song_name") + "[/u] rename to [u]" + newName + "[/u]");
				} else {
					sendToInvoker(eventInfo, "Permission denied");
				}
			} else if(cmd.equalsIgnoreCase("radiomode")) {
				if(UserDatabase.instance.isUserAdmin(eventInfo.get("invokeruid"))) {
					if(args.length == 2 && args[1].equalsIgnoreCase("on")) {
						if(radiomode) {
							sendToInvoker(eventInfo, "Radio Mode is already on");
						} else {
							try {
								String[] commands = {"bash", "-c", "mpc clear && mpc ls | mpc add && mpc repeat on && mpc random on && mpc consume off && mpc crossfade 5 && mpc shuffle && mpc play"};
								Process p = Runtime.getRuntime().exec(commands);
								p.waitFor();
								radiomode = true;
								sendToInvoker(eventInfo, "Radio Mode is now ON");
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					} else if(args.length == 2 && args[1].equalsIgnoreCase("off")) {
						if(!radiomode) {
							sendToInvoker(eventInfo, "Radio Mode is already off");
						} else {
							try {
								String[] commands = {"bash", "-c", "mpc clear && mpc repeat off && mpc random off && mpc consume on && mpc crossfade 0"};
								Process p = Runtime.getRuntime().exec(commands);
								p.waitFor();
								radiomode = false;
								sendToInvoker(eventInfo, "Radio Mode is now OFF");
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					} else {
						sendToInvoker(eventInfo, "Radio Mode is " + (radiomode ? "ON" : "OFF"));
					}
				} else {
					sendToInvoker(eventInfo, "Permission denied");
				}
			} else if(cmd.equalsIgnoreCase("auth")) {
				if(authcode == null) {
					sendToInvoker(eventInfo, "Error: no authentication code available");
				} else if(args.length == 2) {
					if(authcode.equals(args[1])) {
						UserDatabase.instance.addUser(eventInfo.get("invokername"), eventInfo.get("invokeruid"), "admin");
						authcode = null;
						sendToInvoker(eventInfo, "You are now an Administrator");
					} else {
						sendToInvoker(eventInfo, "[b][color=red]BAD AUTHENTICATION CODE![/color][/b]");
					}
				}
			} else if(cmd.equalsIgnoreCase("songname")) {
				try {
					String queue = "Current Song Queue:";

					String[] commands = {"bash", "-c", "mpc current"};
					Process p = Runtime.getRuntime().exec(commands);
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					int i = 0;
					String line;
					while((line = reader.readLine()) != null) {
						queue += "\n";
						if(i == 0) queue += "[b]";
						queue += MusicDatabase.instance.findSongByFile(line).get("song_name");
						if(i == 0) queue += "[/b]";
						i++;

						if(queue.length() >= 900) {
							sendToInvoker(eventInfo, queue);
							queue = "";
						}
					}

					sendToInvoker(eventInfo, queue);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if(radiomode) {
				sendToInvoker(eventInfo, "Unavailable in radio mode");

			} else if(cmd.equalsIgnoreCase("play")) {
				try {
					String[] commands = {"bash", "-c", "mpc play"};
					Runtime.getRuntime().exec(commands);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if(cmd.equalsIgnoreCase("pause")) {
				try {
					String[] commands = {"bash", "-c", "mpc pause"};
					Runtime.getRuntime().exec(commands);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if(cmd.equalsIgnoreCase("getqueue")) {
				try {
					String queue = "Current Song Queue:";

					String[] commands = {"bash", "-c", "mpc playlist"};
					Process p = Runtime.getRuntime().exec(commands);
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					int i = 0;
					String line;
					while((line = reader.readLine()) != null) {
						queue += "\n";
						if(i == 0) queue += "[b]";
						queue += MusicDatabase.instance.findSongByFile(line).get("song_name");
						if(i == 0) queue += "[/b]";
						i++;

						if(queue.length() >= 900) {
							sendToInvoker(eventInfo, queue);
							queue = "";
						}
					}

					sendToInvoker(eventInfo, queue);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} else if(cmd.equalsIgnoreCase("add")) {
				if(args.length < 2) {
					sendToInvoker(eventInfo, "Bad Syntax");
					return;
				}

				String namequery = args[1];
				JSONObject song = null;

				if(namequery.length() <= 5) {
					try {
						song = MusicDatabase.instance.findSongById(Integer.parseInt(namequery));
					} catch(NumberFormatException e) {
						e.printStackTrace();
					}
				}

				if(namequery.contains("[URL]")) {
					namequery = namequery.substring(5, namequery.length() - 6);
					song = MusicDatabase.instance.findSongByUrl(namequery);
				}

				if(namequery.equals("random")) {
					song = MusicDatabase.instance.getRandomSong();
				}

				if(song == null) {
					System.out.println("song not found");
					sendToInvoker(eventInfo, "Song not found");
					return;
				}

				try {
					String[] commands = { "bash", "-c", "mpc add " + song.get("filename") };
					Process p = Runtime.getRuntime().exec(commands);
					p.waitFor();

					sendToInvoker(eventInfo, "Added [u]" + song.get("song_name") + "[/u] to queue");
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}
	}

	public void sendToInvoker(HashMap<String,String> eventInfo, String message) {
		query.sendTextMessage(Integer.parseInt(eventInfo.get("invokerid")), JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, message);
	}

	public void proccessCommand(String command) {
		if(command.equals("quit")) {
			ServerCommands.quitProgram(query);
		} else if(command.equals("upgradedb")) {
			MusicDatabase.instance.upgradeDb();
		} else if(command.equals("getauthcode")) {
			authcode = RandomString.getString(10);
			System.out.print(authcode + "\n");
		}
	}

	void runServerMod() {
		query = new JTS3ServerQuery();
		
		// Connect to TS3 Server, set your server data here
		if (!query.connectTS3Query("127.0.0.1", 10011)) {
			echoError();
			return;
		}

		query.loginTS3("serveradmin", "dcicYCBD");

		query.setTeamspeakActionListener(this);
		query.selectVirtualServer(1);

		query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTSERVER, 0);
//		query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTPRIVATE, 0);

		query.setDisplayName("musicbot");

		query.sendTextMessage(1, 3, "Music Bot Started");

		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(1000 * 60 * 8);
						query.poke();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();

		while(true) {
			Scanner in = new Scanner(System.in);
			proccessCommand(in.nextLine());
		}
	}
}
