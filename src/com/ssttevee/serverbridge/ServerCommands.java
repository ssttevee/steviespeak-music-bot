package com.ssttevee.serverbridge;

import com.ssttevee.serverbridge.util.RandomString;
import de.stefan1200.jts3serverquery.JTS3ServerQuery;

public class ServerCommands {

	public static void quitProgram(JTS3ServerQuery query) {
		query.sendTextMessage(1, 3, "Music Bot Stopping");
		query.removeTeamspeakActionListener();
		query.closeTS3Connection();

		System.exit(0);
	}

}
