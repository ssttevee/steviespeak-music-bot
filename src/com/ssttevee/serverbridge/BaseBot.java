package com.ssttevee.serverbridge;

import de.stefan1200.jts3serverquery.JTS3ServerQuery;
import de.stefan1200.jts3serverquery.TeamspeakActionListener;

import java.util.*;

public class BaseBot implements TeamspeakActionListener {
	public JTS3ServerQuery query;
	public List<CommandListener> commandListeners = new ArrayList<CommandListener>();

	public BaseBot() {
		this("");
	}

	public BaseBot(String threadName) {
		query = new JTS3ServerQuery(threadName);
	}

	@Override
	public void teamspeakActionPerformed(String eventType, HashMap<String, String> eventInfo) {
		if(eventType.equals("notifytextmessage") && eventInfo.get("msg").startsWith("!")) {
			List<String> args = new LinkedList<String>(Arrays.asList(eventInfo.get("msg").substring(1).split(" ")));
			String command = args.remove(0);

			for(CommandListener cl : commandListeners)
				if(command.equalsIgnoreCase(cl.command)) {
					cl.eventInfo = eventInfo;
					cl.onCommand(args.toArray(new String[args.size()]), Integer.parseInt(eventInfo.get("invokerid")));
				}
		}
	}

	public void addCommandListener(CommandListener cls) {
		commandListeners.add(cls);
	}

	public void removeCommandListener(CommandListener cls) {
		commandListeners.remove(cls);
	}

	public void clearCommandListeners() {
		commandListeners.clear();
	}

	public void sendToInvoker(int invokerId, String message) {
		query.sendTextMessage(invokerId, JTS3ServerQuery.TEXTMESSAGE_TARGET_CLIENT, message);
	}

	public boolean connect() {
		if(!query.connectTS3Query("127.0.0.1", 10011)) return false;
		return query.loginTS3("serveradmin", "dcicYCBD");
	}

	public void stop() {
		query.clearTeamspeakActionListeners();
		query.closeTS3Connection();
	}

	protected abstract class CommandListener {
		public String command;
		protected HashMap<String, String> eventInfo;
		public CommandListener(String command) {
			this.command = command;
		}
		public abstract void onCommand(String[] args, int invokerId);
	}

}
