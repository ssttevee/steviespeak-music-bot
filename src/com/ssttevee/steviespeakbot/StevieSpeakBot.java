package com.ssttevee.steviespeakbot;

import com.ssttevee.steviespeakbot.util.MusicDatabase;
import com.ssttevee.steviespeakbot.util.RandomString;
import com.ssttevee.steviespeakbot.util.UserDatabase;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class StevieSpeakBot extends BaseBot {
	List<BaseBot> childBots = new ArrayList<BaseBot>();
	String authcode = null;
	
	public static void main(String[] args) {
		StevieSpeakBot stevieBot = new StevieSpeakBot();
		stevieBot.connect();

		MusicBot musicBot = new MusicBot();
		musicBot.connect();
		stevieBot.childBots.add(musicBot);

		while(true) {
			Scanner in = new Scanner(System.in);
			stevieBot.proccessCommand(in.nextLine());
		}
	}

	public StevieSpeakBot() {
		super("steviebot");
	}

	@Override
	public boolean connect() {
		if(!super.connect()) return false;

		query.addTeamspeakActionListener(this);
		if(!query.selectVirtualServer(1)) echoError();

		if(!query.addEventNotify(JTS3ServerQuery.EVENT_MODE_TEXTSERVER, 0)) echoError();

		if(!query.setDisplayName("steviebot")) echoError();
		if(!query.sendTextMessage(1, JTS3ServerQuery.TEXTMESSAGE_TARGET_VIRTUALSERVER, "StevieBot Started")) echoError();

		addCommandListener(new CommandListener("quitbot") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				stop();
			}
		});
		addCommandListener(new CommandListener("auth") {
			@Override
			public void onCommand(String[] args, int invokerId) {
				if(authcode == null) {
					sendToInvoker(invokerId, "Error: no authentication code available");
				} else if(args.length == 2) {
					if(authcode.equals(args[1])) {
						UserDatabase.instance.addUser(eventInfo.get("invokername"), eventInfo.get("invokeruid"), "admin");
						sendToInvoker(invokerId, "You are now an Administrator");
					} else {
						sendToInvoker(invokerId, "[b][color=red]BAD AUTHENTICATION CODE![/color][/b]");
					}
				}
			}
		});

		return true;
	}

	@Override
	public void stop() {
		query.sendTextMessage(1, JTS3ServerQuery.TEXTMESSAGE_TARGET_VIRTUALSERVER, "Stopping all bots");

		for(BaseBot bot : childBots) {
			bot.stop();
		}

		super.stop();

		System.exit(0);
	}

	public void proccessCommand(String command) {
		if(command.equals("quit")) {
			stop();
		} else if(command.equals("upgradedb")) {
			MusicDatabase.instance.upgradeDb();
		} else if(command.equals("getauthcode")) {
			authcode = RandomString.getString(10);
			System.out.print(authcode + "\n");
		}
	}
}
