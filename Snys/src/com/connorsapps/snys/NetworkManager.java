package com.connorsapps.snys;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class NetworkManager
{
	//Temporary local address of server on my network. You'll need to change this.
	public static final String SERVER = "http://192.168.1.3:8005";
	private Credentials credentials;
	private Gson gson;
	
	/**
	 * Create manager without credentials.
	 * Must set credentials prior to attempting
	 * verified requests
	 */
	public NetworkManager()
	{
		this(null);
	}
	
	/**
	 * Create manager with credentials
	 * @param cred
	 */
	public NetworkManager(Credentials cred)
	{
		this.setCredentials(cred);
		this.gson = new Gson();
	}	
	
	public String doGet(String endpoint, String params) throws IOException
	{
		URL url = new URL(SERVER + endpoint + "?" + params);
		return readInputStream(url.openStream());
	}
	
	public String doPost(String endpoint, String params) throws IOException
	{
		URL url = new URL(SERVER + endpoint);
		
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setRequestProperty("Content-Length", String.valueOf(params.length()));
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		
		DataOutputStream out = new DataOutputStream(con.getOutputStream());
		out.writeBytes(params);
		out.close();
		
		return readInputStream(con.getInputStream());
	}
	
	private String readInputStream(InputStream in) throws IOException
	{
		BufferedReader read = null;
		
		try
		{
			StringBuilder str = new StringBuilder();
			read = new BufferedReader(new InputStreamReader(in));
		
			char[] buffer = new char[1024 * 16];
			int count;
		
			while ((count = read.read(buffer)) != -1)
				str.append(buffer, 0, count);
		
			return str.toString();
		}
		catch (IOException e)
		{
			throw e;
		}
		finally
		{
			if (read != null)
			{
				read.close();
			}
			else
			{
				in.close();
			}
		}
	}
	
	/**
	 * Register a new email and pass on server
	 * @param email
	 * @param pass
	 * @throws IOException
	 * @throws SnysException: Raised in case of failed account creation
	 * @return New credentials for account
	 */
	public Credentials register(String email, String pass) throws IOException, SnysException
	{
		Credentials nCreds = new Credentials(email, pass);
		String r = this.doGet("/register", nCreds.toString());
		GenericResponse resp = gson.fromJson(r, GenericResponse.class);
		
		if (!resp.getError().equals(""))
			throw new SnysException(resp.getError());
		
		return nCreds;
	}

	/**
	 * Check if current credentials are valid
	 */
	public boolean checkValid() throws IOException
	{
		String resp = this.doGet("/checkValid", this.credentials.toString());
		GenericResponse gresp = gson.fromJson(resp, GenericResponse.class);
		Log.d("devBug", gresp.toString());
		
		//If no errors, return true
		return gresp.getError().equals("");
	}
	
	/**
	 * Access server and retrieve all information about user
	 * @param db
	 * @throws IOException
	 */
	public Information getInfo() throws IOException
	{
		String strInfo = this.doGet("/info", this.credentials.toString());		
		
		Log.d("devBug", strInfo);
		
		Information info = new Information();
		
		JsonParser parse = new JsonParser();
		JsonArray array = parse.parse(strInfo).getAsJsonArray();
		
		Iterator<JsonElement> iter = array.iterator();
		
		while (iter.hasNext())
		{
			JsonObject obj = iter.next().getAsJsonObject();
			
			//All values should have tag field, skip if they don't
			if (!obj.has("tag"))
				continue;
			
			String tag = obj.get("tag").getAsString();
			
			switch (tag)
			{
			case "Notification":
				info.pending.add(gson.fromJson(obj, InternalNotification.class).toNotification());
				break;
			case "HandledNotification":
				info.handled.add(gson.fromJson(obj, InternalHandledNotification.class).toNotification());
				break;
			case "Invitation":
				info.invitations.add(gson.fromJson(obj, InternalMembership.class).toGroup());
				break;
			case "Membership":
				info.memberships.add(gson.fromJson(obj, InternalMembership.class).toGroup());
				break;
			default:
				throw new IOException("Invalid JSON element in server response!");
			
			}
		}
		
		return info;
	}
	
	public Notification[] getNotifications() throws IOException
	{
		String strInfo = this.doGet("/notifications", this.credentials.toString());		
		
		Log.d("devBug", strInfo);
		
		JsonParser parse = new JsonParser();
		JsonArray array = parse.parse(strInfo).getAsJsonArray();
		
		Iterator<JsonElement> iter = array.iterator();
		
		List<Notification> notes = new ArrayList<Notification>();
		
		while (iter.hasNext())
		{
			JsonObject obj = iter.next().getAsJsonObject();
			
			//All values should have tag field, skip if they don't
			if (!obj.has("tag"))
				continue;
			
			String tag = obj.get("tag").getAsString();
			
			switch (tag)
			{
			case "Notification":
				notes.add(gson.fromJson(obj, InternalNotification.class).toNotification());
				break;
			case "HandledNotification":
				notes.add(gson.fromJson(obj, InternalHandledNotification.class).toNotification());
				break;
			default:
				throw new IOException("Bad credentials!");
			}
		}
		
		//Move to static array for consistency
		Notification[] notifications = new Notification[notes.size()];
		notes.toArray(notifications);
		return notifications;
	}
	
	private Group[] toGroups(String str)
	{
		InternalMembership[] groups = gson.fromJson(str, InternalMembership[].class);
		
		Group[] rGroups = new Group[groups.length];
		
		for (int i = 0; i < groups.length; i++)
			rGroups[i] = groups[i].toGroup();
		
		return rGroups;
	}
	
	/**
	 * Get the groups this user belongs to from the server
	 * @return
	 * @throws IOException
	 */
	public Group[] getGroups() throws IOException
	{
		String str = this.doGet("/groups", this.credentials.toString());
		
		return toGroups(str);
	}
	
	/**
	 * Get the groups this user has been invited to from the server
	 * @return
	 * @throws IOException
	 */
	public Group[] getInvitations() throws IOException
	{
		String str = this.doGet("/invitations", this.credentials.toString());
		
		return toGroups(str);
	}
	
	public Credentials getCredentials()
	{
		return credentials;
	}

	public void setCredentials(Credentials credentials)
	{
		this.credentials = credentials;
	}

	/**
	 * Login credentials. Used in all verified requests
	 * @author connor
	 *
	 */
	public static class Credentials
	{
		private String email, pass;
		
		Credentials(String email, String pass)
		{
			setEmail(email);
			setPass(pass);
		}

		public String getEmail()
		{
			return email;
		}

		public void setEmail(String email)
		{
			this.email = email;
		}

		public String getPass()
		{
			return pass;
		}

		public void setPass(String pass)
		{
			this.pass = pass;
		}
		
		public String toString()
		{
			return "email=" + email + "&pass=" + pass;
		}
	}
	
	/**
	 * Generic response sent by server.
	 * @author connor
	 *
	 */
	private static class GenericResponse
	{
		private String error, response;

		public String getError()
		{
			return error;
		}

		public void setError(String error)
		{
			this.error = error;
		}

		public String getResponse()
		{
			return response;
		}

		public void setResponse(String response)
		{
			this.response = response;
		}
		
		public String toString()
		{
			return "GenericResponse {error = \"" + error + "\", response = \"" + response + "\"}";
		}
	}
	
	/**
	 * Internal representation of a group
	 * Used for JSON deserialization
	 * @author connor
	 *
	 */
	private static class InternalGroup
	{
		private int gid;
		private String groupname;
		
		public InternalGroup(int gid, String groupname)
		{
			this.gid = gid;
			this.groupname = groupname;
		}
	}
	
	/**
	 * Internal representation of a membership,
	 * used in JSON deserialization. Converted
	 * to a Group.
	 * @author connor
	 *
	 */
	private static class InternalMembership
	{
		private InternalGroup group;
		private String permissions;
		
		public InternalMembership(InternalGroup group, String permissions)
		{
			this.group = group;
			this.permissions = permissions;
		}
		
		public Group toGroup()
		{
			return new Group(group.gid,
					group.groupname,
					permissions);
		}
	}
	
	/**
	 * Internal representation of (unhandled) notification.
	 * Converted to a Notification object.
	 * @author connor
	 *
	 */
	private static class InternalNotification
	{
		private int nid;
		private int associatedGid;
		private String text;
		private long time;
		
		public InternalNotification(int nid, int associatedGid, String text, long time)
		{
			this.nid = nid;
			this.associatedGid = associatedGid;
			this.text = text;
			this.time = time;
		}
		
		public Notification toNotification()
		{
			return new Notification(nid,
					associatedGid,
					text,
					time,
					Notification.Status.UNHANDLED,
					0);
		}
	}
	
	/**
	 * Internal representation of a handled notification.
	 * Converted to a notification before leaving this class
	 * @author connor
	 *
	 */
	private static class InternalHandledNotification
	{
		private InternalNotification notification;
		private String status;
		private long remindAt;
		
		public InternalHandledNotification(InternalNotification notification, String status, long remindAt)
		{
			this.notification = notification;
			this.status = status;
			this.remindAt = remindAt;
		}
		
		/**
		 * Get the external representation of this
		 * object.
		 * @return
		 */
		public Notification toNotification()
		{
			return new Notification(notification.nid,
					notification.associatedGid,
					notification.text,
					notification.time,
					this.status,
					this.remindAt);
		}
	}
	
	/**
	 * A class used to group together information query values.
	 * @author connor
	 *
	 */
	public static class Information
	{
		public final List<Group> memberships, invitations;
		public final List<Notification> pending, handled;
		
		public Information()
		{
			this.memberships = new ArrayList<Group>();
			this.invitations = new ArrayList<Group>();
			this.handled = new ArrayList<Notification>();
			this.pending = new ArrayList<Notification>();
		}
		
		public String toString()
		{
			return "Information {memberships = " + memberships + ", invitations = " + invitations + ", handled = " + handled + ", pending = " + pending + "}";
		}
	}
}
