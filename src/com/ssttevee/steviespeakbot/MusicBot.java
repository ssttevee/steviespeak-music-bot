package com.ssttevee.steviespeakbot;

import com.ssttevee.steviespeakbot.util.MusicDatabase;
import com.ssttevee.steviespeakbot.util.UserDatabase;
import com.ssttevee.steviespeakbot.util.apis.API;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

public class MusicBot extends BaseBot {
	private boolean radiomode = false;
	private int botClientId = 0;
	private int botChannelId = 0;

	public MusicBot() {
		super("musicbot");
	}

	@Override
	public boolean connect() {
		if(!super.connect()) return false;

		addCommands();
		query.addTeamspeakActionListener(this);

		query.selectVirtualServer(1);
		query.setDisplayName("musicbot");

		for(HashMap<String, String> client : query.getList(JTS3ServerQuery.LISTMODE_CLIENTLIST))
			if(client.get("client_nickname").equals("MusicBot")) {
				botClientId = Integer.parseInt(client.get("clid"));
				botChannelId = Integer.parseInt(client.get("cid"));
			}
		query.moveClient(query.getCurrentQueryClientID(), botChannelId, "");

		query.addEventNotify(JTS3ServerQuery.EVENT_MODE_CHANNEL, botChannelId);
		query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTCHANNEL, botChannelId);

		query.sendTextMessage(botChannelId, JTS3ServerQuery.TEXTMESSAGE_TARGET_CHANNEL, "Music Bot Started");

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

		return true;
	}

	@Override
	public void stop() {
		query.sendTextMessage(botChannelId, JTS3ServerQuery.TEXTMESSAGE_TARGET_CHANNEL, "Music Bot Stopping");
		super.stop();
	}

	private void addCommands() {
		addCommandListener(new CommandListener("skip") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				try {
					String[] commands = {"bash", "-c", "mpc next"};
					Runtime.getRuntime().exec(commands);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		addCommandListener(new CommandListener("db") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				StringBuilder builder = new StringBuilder();
				JSONArray allSongs;

				if(args.length >= 2 && args[0].equalsIgnoreCase("search")) {
					String searchQuery = "";
					for(int i = 1; i < args.length; i++) {
						if(i > 1) searchQuery += " ";
						searchQuery += args[i];
					}

					allSongs = MusicDatabase.instance.searchSongsByName(searchQuery);

					builder.append("There were [b]");
					builder.append(allSongs.size());
					builder.append("[/b] song(s) found matching \"");
					builder.append(searchQuery);
					builder.append("\":");
				} else if(args.length >= 2 && args[0].equalsIgnoreCase("lookup")) {
					JSONObject song = MusicDatabase.instance.findSongById(Integer.parseInt(args[2]));

					allSongs = new JSONArray();
					builder.append("ID [color=red]");
					builder.append(args[1]);
					builder.append("[/color] corresponds to ");

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
						sendToInvoker(invokerId, builder.toString());
						builder = new StringBuilder();
					}
				}

				sendToInvoker(invokerId, builder.toString());
			}
		});
		addCommandListener(new CommandListener("dl") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				String link = args[0];

				if(!link.contains("[URL]")) {
					sendToInvoker(invokerId, "\nUsage: !dl [song_url]\n\n\tSong URL can be youtube or soundcloud links");
					return;
				}

				link = link.substring(5, link.length() - 6);

				if(MusicDatabase.instance.isSongExist(link)) {
					sendToInvoker(invokerId, MusicDatabase.instance.findSongByUrl(link).get("song_name") + " has already been downloaded");
					return;
				}

				try {
					API api = API.getApi(new URL(link));

					sendToInvoker(invokerId, "Downloading " + link);

					String[] commands = {"bash", "-c", "youtube-dl -o \"/home/$USER/music/" + api.getService() + "/%(id)s.%(ext)s\" --extract-audio --audio-format mp3 " + link};
					Process p = Runtime.getRuntime().exec(commands);
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					String line;
					while((line = reader.readLine()) != null) {
						sendToInvoker(invokerId, line);
					}

					JSONObject song = MusicDatabase.instance.addSong(api);

					sendToInvoker(invokerId, "Downloaded [u]" + song.get("song_name") + "[/u] as [color=red]" + song.get("id") + "[/color]");
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		addCommandListener(new CommandListener("songname") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				try {
					StringBuilder builder = new StringBuilder();
					builder.append("Current Song: ");

					String[] commands = {"bash", "-c", "mpc current"};
					Process p = Runtime.getRuntime().exec(commands);
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					int i = 0;
					String line;
					while((line = reader.readLine()) != null) {
						builder.append("\n");
						if(i == 0) builder.append("[b]");
						builder.append(MusicDatabase.instance.findSongByFile(line).get("song_name"));
						if(i == 0) builder.append("[/b]");
						i++;
					}

					sendToInvoker(invokerId, builder.toString());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});

		addCommandListener(new CommandListener("play") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(radiomode) {
					sendToInvoker(invokerId, "Unavailable in radio mode");
					return;
				}

				try {
					Runtime.getRuntime().exec(new String[] {"bash", "-c", "mpc play"});
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		addCommandListener(new CommandListener("pause") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(radiomode) {
					sendToInvoker(invokerId, "Unavailable in radio mode");
					return;
				}

				try {
					Runtime.getRuntime().exec(new String[] {"bash", "-c", "mpc pause"});
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		addCommandListener(new CommandListener("getqueue") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(radiomode) {
					sendToInvoker(invokerId, "Unavailable in radio mode");
					return;
				}

				try {
					StringBuilder builder = new StringBuilder();
					builder.append("Current Song Queue:");

					Process p = Runtime.getRuntime().exec(new String[] {"bash", "-c", "mpc playlist"});
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					int i = 0;
					String line;
					while((line = reader.readLine()) != null) {
						builder.append("\n");
						if(i == 0) builder.append("[b]");
						builder.append(MusicDatabase.instance.findSongByFile(line).get("song_name"));
						if(i == 0) builder.append("[/b]");
						i++;

						if(builder.length() >= 900) {
							sendToInvoker(invokerId, builder.toString());
							builder.setLength(0);
						}
					}

					sendToInvoker(invokerId, builder.toString());
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
		addCommandListener(new CommandListener("add") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(radiomode) {
					sendToInvoker(invokerId, "Unavailable in radio mode");
					return;
				}

				if(args.length < 1) {
					sendToInvoker(invokerId, "Bad Syntax");
					return;
				}

				String namequery = args[0];
				JSONObject song = null;

				try {
					if(namequery.equals("random")) {
						int count = 1;
						if(args.length > 1) count = Integer.parseInt(args[1]);
						for(; count > 0; count--) {
							song = MusicDatabase.instance.getRandomSong();

							Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "mpc add " + song.get("filename") });
							p.waitFor();

							sendToInvoker(invokerId, "Added [u]" + song.get("song_name") + "[/u] to queue");
						}
						return;
					}

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

					if(song == null) {
						System.out.println("song not found");
						sendToInvoker(invokerId, "Song not found");
						return;
					}

					Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", "mpc add " + song.get("filename") });
					p.waitFor();

					sendToInvoker(invokerId, "Added [u]" + song.get("song_name") + "[/u] to queue");
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		addCommandListener(new CommandListener("rename") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(UserDatabase.instance.isUserAdmin(eventInfo.get("invokeruid"))) {
					if(args.length < 2) {
						sendToInvoker(invokerId, "Song cannot have no name");
						return;
					}

					JSONObject song = MusicDatabase.instance.findSongById(Integer.parseInt(args[1]));
					String newName = "";

					for(int i = 1; i < args.length; i++) {
						if(i > 1) newName += " ";
						newName += args[i];
					}

					MusicDatabase.instance.renameSong(song, newName);
					sendToInvoker(invokerId, "[u]" + song.get("song_name") + "[/u] rename to [u]" + newName + "[/u]");
				} else {
					sendToInvoker(invokerId, "Permission denied");
				}
			}
		});
		addCommandListener(new CommandListener("del") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(UserDatabase.instance.isUserAdmin(eventInfo.get("invokeruid"))) {
					if(args.length < 1) return;
					JSONObject song = MusicDatabase.instance.findSongById(Integer.parseInt(args[1]));
					MusicDatabase.instance.delSong(song);
					try {
						Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", "rm -f ~/" + song.get("filename")});
						p.waitFor();

						sendToInvoker(invokerId, "[u]" + song.get("song_name") + "[/u] was deleted");
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else {
					sendToInvoker(invokerId, "Permission denied");
				}
			}
		});
		addCommandListener(new CommandListener("radiomode") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(UserDatabase.instance.isUserAdmin(eventInfo.get("invokeruid"))) {
					if(args.length == 1 && args[0].equalsIgnoreCase("on")) {
						if(radiomode) {
							sendToInvoker(invokerId, "Radio Mode is already on");
						} else {
							try {
								String[] commands = {"bash", "-c", "mpc clear && mpc ls | mpc add && mpc repeat on && mpc random on && mpc consume off && mpc crossfade 5 && mpc shuffle && mpc play"};
								Process p = Runtime.getRuntime().exec(commands);
								p.waitFor();
								radiomode = true;
								sendToInvoker(invokerId, "Radio Mode is now ON");
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					} else if(args.length == 1 && args[0].equalsIgnoreCase("off")) {
						if(!radiomode) {
							sendToInvoker(invokerId, "Radio Mode is already off");
						} else {
							try {
								String[] commands = {"bash", "-c", "mpc clear && mpc repeat off && mpc random off && mpc consume on && mpc crossfade 0"};
								Process p = Runtime.getRuntime().exec(commands);
								p.waitFor();
								radiomode = false;
								sendToInvoker(invokerId, "Radio Mode is now OFF");
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					} else {
						sendToInvoker(invokerId, "Radio Mode is " + (radiomode ? "ON" : "OFF"));
					}
				} else {
					sendToInvoker(invokerId, "Permission denied");
				}
			}
		});
	}

	@Override
	public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo) {
		super.teamspeakActionPerformed(eventType, eventInfo);

		if(eventType.equals("notifyclientmoved") && Integer.parseInt(eventInfo.get("clid")) == botClientId) {
			botChannelId = Integer.parseInt(eventInfo.get("ctid"));
			query.removeAllEvents();

			query.moveClient(query.getCurrentQueryClientID(), botChannelId, "");
			query.sendTextMessage(botChannelId, JTS3ServerQuery.TEXTMESSAGE_TARGET_CHANNEL, "Music Bot Moved");

			query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTCHANNEL, botChannelId);
			query.addEventNotify(JTS3ServerQuery.EVENT_MODE_CHANNEL, botChannelId);
		} else if(eventType.equals("notifyclientmoved")) {
			int count = 0;
			for(HashMap<String, String> client : query.getList(JTS3ServerQuery.LISTMODE_CLIENTLIST))
				if(client.get("client_nickname").equals("MusicBot")) {
					if(Integer.parseInt(client.get("cid")) == botChannelId) count++;
				}
			if(count <= 1) for(CommandListener cl : commandListeners) {
				if(cl.command.equalsIgnoreCase("radiomode")) cl.onCommand(new String[] {"off"}, botClientId);
			}
		}
	}
}
